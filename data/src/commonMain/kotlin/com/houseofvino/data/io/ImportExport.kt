package com.houseofvino.data.io

import app.cash.sqldelight.db.QueryResult
import com.houseofvino.core.csv.ColumnMapping
import com.houseofvino.core.csv.Csv
import com.houseofvino.core.csv.DuplicateAssessment
import com.houseofvino.core.csv.DuplicateDetector
import com.houseofvino.core.csv.ExistingLot
import com.houseofvino.core.csv.ExistingVintage
import com.houseofvino.core.csv.Exports
import com.houseofvino.core.csv.ImportDecision
import com.houseofvino.core.csv.ImportIssue
import com.houseofvino.core.csv.ImportOptions
import com.houseofvino.core.csv.ImportParser
import com.houseofvino.core.csv.ImportRecord
import com.houseofvino.core.csv.ParseResult
import com.houseofvino.core.inventory.MovementKind
import com.houseofvino.core.model.BottleFormats
import com.houseofvino.core.model.LocationKind
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.text.TextNormalizer
import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.db.bindAny
import com.houseofvino.data.repo.CellarRepository
import com.houseofvino.data.repo.LocationInput
import com.houseofvino.data.repo.LocationRepository
import com.houseofvino.data.repo.LotInput
import com.houseofvino.data.repo.VintageInput
import com.houseofvino.data.repo.WineInput
import com.houseofvino.data.repo.WineTarget
import com.houseofvino.data.repo.money

/** Result of the review step: everything the user needs to see before committing an import. */
data class ImportPreview(
    val table: Csv.Table,
    val mapping: ColumnMapping,
    val parse: ParseResult,
    val assessments: List<DuplicateAssessment>,
) {
    val validRows: Int get() = parse.records.size
    val errorRows: Int get() = parse.errorRows.size
    val needsReview: Int get() = assessments.count { it.needsReview }
    val bottles: Int get() = parse.records.sumOf { it.quantity }
}

data class ImportReport(
    val winesCreated: Int,
    val vintagesCreated: Int,
    val lotsCreated: Int,
    val bottlesAdded: Int,
    val skipped: Int,
    val failed: List<ImportIssue>,
)

/**
 * CSV import: parse → validate → detect duplicates → user decisions → commit. A row is either
 * fully written (wine, vintage, purchase, stock, tags) or not at all; failures are reported by row.
 */
class ImportService(
    private val database: LocalDatabase,
    private val cellar: CellarRepository,
    private val locations: LocationRepository,
) {

    fun preview(collectionId: String, csvText: String, mapping: ColumnMapping? = null, options: ImportOptions): ImportPreview {
        val table = Csv.read(csvText)
        val m = mapping ?: ColumnMapping.suggest(table.header)
        val parse = ImportParser(m, options).parse(table)
        val detector = DuplicateDetector(existingVintages(collectionId), existingLots(collectionId))
        return ImportPreview(table, m, parse, detector.assess(parse.records))
    }

    /**
     * Writes the import. Each chunk of rows commits in one transaction; if any row in a chunk
     * fails, the chunk is rolled back and replayed row by row so only the failing rows are
     * left out (and reported). Counters and caches are only kept for work that committed.
     */
    fun commit(
        collectionId: String,
        preview: ImportPreview,
        decisions: Map<Int, ImportDecision> = emptyMap(),
        chunkSize: Int = 200,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): ImportReport {
        val committed = ImportState()
        val failed = ArrayList<ImportIssue>()
        var skipped = 0
        val work = preview.assessments.filter { a ->
            val skip = (decisions[a.record.row] ?: a.defaultDecision) == ImportDecision.Skip
            if (skip) skipped++
            !skip
        }
        work.chunked(chunkSize).forEachIndexed { chunkIndex, chunk ->
            val attempt = committed.copy()
            val chunkOk = runCatching {
                database.write { chunk.forEach { importRow(collectionId, it, decisions, attempt) } }
            }.isSuccess
            if (chunkOk) {
                committed.adopt(attempt)
            } else {
                for (a in chunk) {
                    val single = committed.copy()
                    runCatching { database.write { importRow(collectionId, a, decisions, single) } }
                        .onSuccess { committed.adopt(single) }
                        .onFailure { e ->
                            failed += ImportIssue(a.record.row, null, com.houseofvino.core.csv.Severity.ERROR,
                                e.message ?: "Couldn't import this row")
                        }
                }
            }
            onProgress(minOf((chunkIndex + 1) * chunkSize, work.size), work.size)
        }
        return ImportReport(committed.wines, committed.vintages, committed.lots, committed.bottles, skipped, failed)
    }

    /** Mutable bookkeeping for one import attempt; copied per attempt so rollbacks discard it. */
    private class ImportState(
        var wines: Int = 0, var vintages: Int = 0, var lots: Int = 0, var bottles: Int = 0,
        val locations: HashMap<List<String>, String?> = HashMap(),
        /** Wines and vintages created earlier in this import, so later rows attach to them. */
        val createdWines: HashMap<String, String> = HashMap(),
        val createdVintages: HashMap<String, String> = HashMap(),
    ) {
        fun copy() = ImportState(wines, vintages, lots, bottles, HashMap(locations), HashMap(createdWines), HashMap(createdVintages))
        fun adopt(o: ImportState) {
            wines = o.wines; vintages = o.vintages; lots = o.lots; bottles = o.bottles
            locations.clear(); locations.putAll(o.locations)
            createdWines.clear(); createdWines.putAll(o.createdWines)
            createdVintages.clear(); createdVintages.putAll(o.createdVintages)
        }
    }

    private fun importRow(collectionId: String, a: DuplicateAssessment, decisions: Map<Int, ImportDecision>, s: ImportState) {
        val r = a.record
        val decision = decisions[r.row] ?: a.defaultDecision
        val identity = r.identityKey
        val target: WineTarget = when (decision) {
            is ImportDecision.AddToVintage -> WineTarget.ExistingVintage(decision.vintageId)
            is ImportDecision.AddVintageToWine -> WineTarget.ExistingWine(decision.wineId)
            ImportDecision.CreateNew -> s.createdVintages[identity.toString()]?.let { WineTarget.ExistingVintage(it) }
                ?: s.createdWines[identity.wineKey]?.let { WineTarget.ExistingWine(it) }
                ?: WineTarget.NewWine(wineInput(r))
            ImportDecision.Skip -> return
        }
        val vintageExisted = when (target) {
            is WineTarget.ExistingVintage -> true
            is WineTarget.ExistingWine -> cellar.findVintage(target.wineId, r.vintage) != null
            is WineTarget.NewWine -> false
        }
        val locationId = if (r.locationPath.isEmpty()) null else s.locations.getOrPut(r.locationPath) {
            resolveLocation(collectionId, r.locationPath)
        }
        val lot = if (r.quantity > 0) LotInput(
            quantity = r.quantity, formatMl = r.formatMl, locationId = locationId, acquiredOn = r.acquiredOn,
            source = r.source, unitPrice = r.unitPrice, notes = r.bin?.let { "Bin $it" }, kind = MovementKind.OPENING,
        ) else null
        // CreateNew means the user has seen the duplicate review, so a same-named wine is intended.
        val vintageId = cellar.addWine(collectionId, target, vintageInput(r), lot, r.tags, allowDuplicateWine = true)
        val wineId = cellar.vintage(vintageId)?.wineId
        if (target is WineTarget.NewWine) {
            s.wines++
            wineId?.let { s.createdWines[identity.wineKey] = it }
        }
        if (!vintageExisted) s.vintages++
        s.createdVintages[identity.toString()] = vintageId
        if (lot != null) { s.lots++; s.bottles += lot.quantity }
        val code = r.barcode
        if (code != null && wineId != null) {
            cellar.linkBarcode(collectionId, code, if (code.length == 12) "UPC_A" else "EAN_13", wineId, vintageId, r.formatMl)
        }
    }

    private fun wineInput(r: ImportRecord) = WineInput(
        producer = r.producer, name = r.name, color = r.color, style = r.style, country = r.country, region = r.region,
        subRegion = r.subRegion, appellation = r.appellation, classification = r.classification, grapes = r.grapes, notes = r.notes,
    )

    private fun vintageInput(r: ImportRecord) = VintageInput(
        vintage = r.vintage, drinkFrom = r.drinkFrom, drinkUntil = r.drinkUntil,
        drinkWindowSource = if (r.drinkFrom != null || r.drinkUntil != null) "Imported" else null,
    )

    /** Finds or creates the location path ("Main cellar" › "Rack A"). */
    private fun resolveLocation(collectionId: String, path: List<String>): String? {
        var parent: String? = null
        val all = locations.all(collectionId)
        for ((depth, name) in path.withIndex()) {
            val existing = all.firstOrNull { it.parentId == parent && TextNormalizer.normalize(it.name) == TextNormalizer.normalize(name) }
                ?: locations.all(collectionId).firstOrNull { it.parentId == parent && TextNormalizer.normalize(it.name) == TextNormalizer.normalize(name) }
            parent = existing?.id ?: locations.create(collectionId, LocationInput(
                name = name, kind = if (depth == 0) LocationKind.CELLAR else LocationKind.RACK, parentId = parent,
            ))
        }
        return parent
    }

    private fun existingVintages(collectionId: String): List<ExistingVintage> =
        database.catalog.identityIndex(collectionId).executeAsList().map {
            ExistingVintage(it.vintage_id, it.wine_id, it.producer, it.name, VintageYear.fromStorage(it.vintage_kind, it.year?.toInt()))
        }

    private fun existingLots(collectionId: String): List<ExistingLot> =
        database.inventory.existingLotsForDedupe(collectionId).executeAsList().map {
            ExistingLot(it.id, it.vintage_id, it.format_ml.toInt(), com.houseofvino.core.time.IsoDate.parseOrNull(it.acquired_on),
                money(it.unit_price_minor, it.unit_price_currency), it.source, it.received.toInt())
        }
}

/** CSV exports. Rows are streamed from SQL into the writer, never collected as domain objects. */
class ExportService(private val database: LocalDatabase, private val locations: LocationRepository) {

    fun inventoryCsv(collectionId: String): String {
        val paths = locations.all(collectionId).associate { it.id to locations.pathName(it.id) }
        val rows = query("""
            SELECT w.producer, w.name, v.vintage_kind, v.year, w.color, w.style, w.country, w.region, w.sub_region, w.appellation,
                   w.classification,
                   (SELECT group_concat(g.grape || coalesce(' ' || g.percent || '%', ''), ', ') FROM wine_grape g WHERE g.wine_id = w.id AND g.deleted_at IS NULL),
                   l.format_ml, s.quantity, s.location_key, l.acquired_on, l.unit_price_minor, l.unit_price_currency, l.source,
                   v.drink_from, v.drink_until,
                   (SELECT group_concat(t.name, ', ') FROM vintage_tag vt JOIN tag t ON t.id = vt.tag_id WHERE vt.vintage_id = v.id AND vt.deleted_at IS NULL),
                   coalesce(v.notes, w.notes), v.id, l.id
            FROM stock_level s JOIN lot l ON l.id = s.lot_id JOIN vintage v ON v.id = l.vintage_id JOIN wine w ON w.id = v.wine_id
            WHERE l.collection_id = ? AND l.deleted_at IS NULL AND s.quantity > 0
            ORDER BY w.producer_sort, w.name, v.year
        """.trimIndent(), collectionId) { c ->
            Exports.InventoryRow(
                producer = c.getString(0)!!, wine = c.getString(1), vintage = VintageYear.fromStorage(c.getString(2)!!, c.getLong(3)?.toInt()).toString(),
                color = c.getString(4), style = c.getString(5), country = c.getString(6), region = c.getString(7), subRegion = c.getString(8),
                appellation = c.getString(9), classification = c.getString(10), grapes = c.getString(11), formatMl = c.getLong(12)!!.toInt(),
                quantity = c.getLong(13)!!.toInt(), location = c.getString(14)!!.ifEmpty { null }?.let { paths[it] },
                purchaseDate = c.getString(15), unitPrice = money(c.getLong(16), c.getString(17)), source = c.getString(18),
                drinkFrom = c.getLong(19)?.toInt(), drinkUntil = c.getLong(20)?.toInt(), tags = c.getString(21), notes = c.getString(22),
                vintageId = c.getString(23)!!, lotId = c.getString(24)!!,
            )
        }
        return Exports.inventory(rows.asSequence())
    }

    fun tastingsCsv(collectionId: String): String {
        val rows = query("""
            SELECT t.tasted_on, w.producer, w.name, v.vintage_kind, v.year, t.rating, t.notes, t.aromas,
                   trim(coalesce('acidity ' || lower(t.acidity) || ' ', '') || coalesce('tannin ' || lower(t.tannin) || ' ', '') ||
                        coalesce('body ' || lower(t.body) || ' ', '') || coalesce('finish ' || lower(t.finish), '')),
                   t.occasion, t.food_pairing, t.companions, t.place, t.format_ml, t.would_buy_again, t.id, v.id
            FROM tasting t JOIN vintage v ON v.id = t.vintage_id JOIN wine w ON w.id = v.wine_id
            WHERE t.collection_id = ? AND t.deleted_at IS NULL ORDER BY t.tasted_on DESC
        """.trimIndent(), collectionId) { c ->
            Exports.TastingRow(
                tastedOn = c.getString(0)!!, producer = c.getString(1)!!, wine = c.getString(2),
                vintage = VintageYear.fromStorage(c.getString(3)!!, c.getLong(4)?.toInt()).toString(), rating = c.getLong(5)?.toInt(),
                notes = c.getString(6), aromas = c.getString(7), structured = c.getString(8)?.ifEmpty { null }, occasion = c.getString(9),
                foodPairing = c.getString(10), companions = c.getString(11), place = c.getString(12), formatMl = c.getLong(13)?.toInt(),
                wouldBuyAgain = c.getLong(14)?.let { it != 0L }, tastingId = c.getString(15)!!, vintageId = c.getString(16)!!,
            )
        }
        return Exports.tastings(rows.asSequence())
    }

    /** Purchases, sales, gifts and valuations. Amounts stay in their original currencies. */
    fun financialCsv(collectionId: String): String {
        val movements = query("""
            SELECT m.occurred_at, m.kind, w.producer, w.name, v.vintage_kind, v.year, l.format_ml, m.delta,
                   l.unit_price_minor, l.unit_price_currency, m.amount_minor, m.amount_currency, m.counterparty, l.source, m.note, m.id
            FROM movement m JOIN lot l ON l.id = m.lot_id JOIN vintage v ON v.id = l.vintage_id JOIN wine w ON w.id = v.wine_id
            WHERE m.collection_id = ? AND m.kind IN ('PURCHASE', 'SELL', 'GIFT_OUT', 'GIFT_IN', 'LOSS')
              AND m.id NOT IN (SELECT reversal_of FROM movement WHERE kind = 'REVERSAL' AND reversal_of IS NOT NULL)
            ORDER BY m.occurred_at, m.rowid
        """.trimIndent(), collectionId) { c ->
            val kind = c.getString(1)!!
            val qty = kotlin.math.abs(c.getLong(7)!!.toInt())
            val unitCost = money(c.getLong(8), c.getString(9))
            val amount = money(c.getLong(10), c.getString(11))
            Exports.FinancialRow(
                date = com.houseofvino.core.time.IsoDate.fromEpochMillis(c.getLong(0)!!).toString(),
                type = kind.lowercase().replace('_', ' '), producer = c.getString(2)!!, wine = c.getString(3),
                vintage = VintageYear.fromStorage(c.getString(4)!!, c.getLong(5)?.toInt()).toString(), formatMl = c.getLong(6)!!.toInt(),
                quantity = qty, unitAmount = if (kind == "PURCHASE") unitCost else amount?.allocate(qty)?.lastOrNull(),
                totalAmount = amount ?: unitCost?.times(qty), counterparty = c.getString(12), source = c.getString(13), note = c.getString(14),
                reference = c.getString(15)!!,
            )
        }
        val valuations = query("""
            SELECT va.as_of, w.producer, w.name, v.vintage_kind, v.year, va.format_ml, va.amount_minor, va.amount_currency, va.source, va.note, va.id
            FROM valuation va JOIN vintage v ON v.id = va.vintage_id JOIN wine w ON w.id = v.wine_id
            WHERE va.collection_id = ? AND va.deleted_at IS NULL ORDER BY va.as_of
        """.trimIndent(), collectionId) { c ->
            Exports.FinancialRow(
                date = c.getString(0)!!, type = "valuation", producer = c.getString(1)!!, wine = c.getString(2),
                vintage = VintageYear.fromStorage(c.getString(3)!!, c.getLong(4)?.toInt()).toString(), formatMl = c.getLong(5)!!.toInt(),
                quantity = 1, unitAmount = money(c.getLong(6), c.getString(7)), totalAmount = null, counterparty = null,
                source = c.getString(8), note = c.getString(9), reference = c.getString(10)!!,
            )
        }
        return Exports.financial((movements + valuations).sortedBy { it.date }.asSequence())
    }

    fun suggestedFileName(kind: String, today: com.houseofvino.core.time.IsoDate) = "house-of-vino-$kind-$today.csv"

    private fun <T> query(sql: String, vararg args: Any?, map: (app.cash.sqldelight.db.SqlCursor) -> T): List<T> =
        database.driver.executeQuery(null, sql, { c ->
            val out = ArrayList<T>()
            while (c.next().value) out += map(c)
            QueryResult.Value(out)
        }, args.size) { args.forEachIndexed { i, a -> bindAny(i, a) } }.value

    companion object {
        fun formatName(ml: Int) = BottleFormats.nameFor(ml)
    }
}

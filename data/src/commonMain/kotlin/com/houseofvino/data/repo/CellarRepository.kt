package com.houseofvino.data.repo

import com.houseofvino.core.inventory.LedgerError
import com.houseofvino.core.inventory.LedgerPlanner
import com.houseofvino.core.inventory.Movement
import com.houseofvino.core.inventory.MovementKind
import com.houseofvino.core.inventory.StockAllocator
import com.houseofvino.core.inventory.StockKey
import com.houseofvino.core.inventory.StockProjection
import com.houseofvino.core.model.GrapeShare
import com.houseofvino.core.model.Lot
import com.houseofvino.core.model.Packaging
import com.houseofvino.core.model.Tag
import com.houseofvino.core.model.Vintage
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.Wine
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import com.houseofvino.core.money.Money
import com.houseofvino.core.text.TextNormalizer
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.db.WriteScope
import com.houseofvino.data.sync.SyncTables

data class WineInput(
    val producer: String,
    val name: String? = null,
    val color: WineColor? = null,
    val style: WineStyle? = null,
    val country: String? = null,
    val region: String? = null,
    val subRegion: String? = null,
    val appellation: String? = null,
    val classification: String? = null,
    val grapes: List<GrapeShare> = emptyList(),
    val notes: String? = null,
) {
    fun validated(): WineInput {
        require(producer.isNotBlank()) { "Producer is required" }
        val total = grapes.mapNotNull { it.percent }.sum()
        require(total <= 100) { "Grape percentages add up to $total%" }
        return copy(
            producer = producer.trim(), name = name.clean(), country = country.clean(), region = region.clean(),
            subRegion = subRegion.clean(), appellation = appellation.clean(), classification = classification.clean(),
            notes = notes.clean(), grapes = grapes.distinctBy { TextNormalizer.normalize(it.grape) },
        )
    }
}

data class VintageInput(
    val vintage: VintageYear,
    val drinkFrom: Int? = null,
    val drinkUntil: Int? = null,
    val drinkWindowSource: String? = null,
    val abvBasisPoints: Int? = null,
    val notes: String? = null,
)

data class LotInput(
    val quantity: Int,
    val formatMl: Int = 750,
    val locationId: String? = null,
    val acquiredOn: IsoDate? = null,
    val source: String? = null,
    val orderRef: String? = null,
    val unitPrice: Money? = null,
    val packaging: Packaging? = null,
    val bottlesPerCase: Int? = null,
    val notes: String? = null,
    val kind: MovementKind = MovementKind.PURCHASE,
    val wishlistItemId: String? = null,
)

/** Where new bottles go: a brand-new wine, a new vintage of a known wine, or a known vintage. */
sealed interface WineTarget {
    data class NewWine(val wine: WineInput) : WineTarget
    data class ExistingWine(val wineId: String) : WineTarget
    data class ExistingVintage(val vintageId: String) : WineTarget
}

class DuplicateWineException(val existingWineIds: List<String>) :
    IllegalStateException("A wine with this producer and name already exists")

class VintageExistsException(val existingVintageId: String) :
    IllegalStateException("This vintage already exists for the wine")

class StockRemainingException(val bottles: Int) :
    IllegalStateException("$bottles bottle(s) are still in stock; record them as drunk, sold or lost first")

data class StockLine(
    val lotId: String,
    val locationId: String?,
    val locationName: String?,
    val quantity: Int,
    val formatMl: Int,
    val acquiredOn: IsoDate?,
    val unitPrice: Money?,
    val source: String?,
)

data class MovementLine(val movement: Movement, val locationName: String?, val formatMl: Int, val reversed: Boolean)

data class VintageDetail(
    val wine: Wine,
    val vintage: Vintage,
    val tags: List<Tag>,
    val stock: List<StockLine>,
    val lots: List<Lot>,
    val otherVintages: List<Vintage>,
    val labelPhotoPath: String?,
    val averageRating: Int?,
    val tastingCount: Int,
) {
    val totalBottles: Int get() = stock.sumOf { it.quantity }
}

/**
 * Wine identity and inventory. Every stock change goes through [LedgerPlanner] and is written in a
 * single transaction that re-reads current stock first, so concurrent UI actions cannot oversell.
 */
class CellarRepository(private val database: LocalDatabase) {

    private val planner = LedgerPlanner(database.ids, database.deviceId)
    private val catalog get() = database.catalog
    private val inventory get() = database.inventory

    // ---------------------------------------------------------------------------------------------
    // Reads

    fun wine(wineId: String): Wine? = catalog.wineById(wineId).executeAsOneOrNull()?.let { row ->
        row.toDomain(catalog.grapesForWine(wineId).executeAsList())
    }

    fun vintage(vintageId: String): Vintage? = catalog.vintageById(vintageId).executeAsOneOrNull()?.toDomain()

    fun vintageDetail(vintageId: String): VintageDetail? {
        val v = vintage(vintageId) ?: return null
        val w = wine(v.wineId) ?: return null
        val stock = stockLines(vintageId)
        val rating = database.journal.averageRatingForVintage(vintageId).executeAsOne()
        return VintageDetail(
            wine = w, vintage = v,
            tags = catalog.tagsForVintage(vintageId).executeAsList().map { it.toDomain() },
            stock = stock,
            lots = inventory.lotsForVintage(vintageId).executeAsList().map { it.toDomain() },
            otherVintages = catalog.vintagesForWine(v.wineId).executeAsList().map { it.toDomain() }.filter { it.id != vintageId },
            labelPhotoPath = v.labelPhotoId?.let { database.journal.photoById(it).executeAsOneOrNull()?.local_path },
            averageRating = rating.average?.let { kotlin.math.round(it).toInt() },
            tastingCount = rating.rated.toInt(),
        )
    }

    fun stockLines(vintageId: String): List<StockLine> = inventory.stockForVintage(vintageId).executeAsList().map {
        StockLine(
            lotId = it.lot_id, locationId = it.location_key.ifEmpty { null }, locationName = it.location_name,
            quantity = it.quantity.toInt(), formatMl = it.format_ml.toInt(), acquiredOn = IsoDate.parseOrNull(it.acquired_on),
            unitPrice = money(it.unit_price_minor, it.unit_price_currency), source = it.source,
        )
    }

    fun quantityForVintage(vintageId: String): Int = inventory.quantityForVintage(vintageId).executeAsOne().toInt()

    fun movementHistory(vintageId: String, limit: Long = 200): List<MovementLine> {
        val rows = inventory.movementsForVintage(vintageId, limit).executeAsList()
        val reversed = if (rows.isEmpty()) emptySet() else
            inventory.reversedIds(rows.map { it.id }).executeAsList().mapNotNull { it.reversal_of }.toSet()
        return rows.map { r ->
            MovementLine(
                movement = Movement(
                    id = r.id, collectionId = r.collection_id, lotId = r.lot_id, locationId = r.location_id, delta = r.delta.toInt(),
                    kind = MovementKind.valueOf(r.kind), occurredAt = r.occurred_at, transferGroupId = r.transfer_group_id,
                    reversalOf = r.reversal_of, amount = money(r.amount_minor, r.amount_currency), counterparty = r.counterparty,
                    note = r.note, deviceId = r.device_id, tastingId = r.tasting_id, createdAt = r.created_at,
                ),
                locationName = r.location_name, formatMl = r.format_ml.toInt(), reversed = r.id in reversed,
            )
        }
    }

    /** Wines with the same normalised producer and name (used to warn before creating a duplicate). */
    fun findWinesByIdentity(collectionId: String, producer: String, name: String?): List<Wine> {
        val sort = TextNormalizer.normalize(producer)
        val wanted = TextNormalizer.normalize(name)
        return catalog.winesByIdentity(collectionId, sort).executeAsList()
            .filter { TextNormalizer.normalize(it.name) == wanted }
            .map { it.toDomain(catalog.grapesForWine(it.id).executeAsList()) }
    }

    fun findVintage(wineId: String, vintage: VintageYear): Vintage? =
        catalog.vintageByIdentity(wineId, vintage.kind, vintage.yearOrNull?.toLong()).executeAsList().firstOrNull()?.toDomain()

    // ---------------------------------------------------------------------------------------------
    // Wine identity writes

    /**
     * Adds a wine (or a vintage, or bottles) in one transaction. Returns the vintage id.
     * Creating a [WineTarget.NewWine] whose identity already exists throws
     * [DuplicateWineException] unless [allowDuplicateWine] — the UI then offers the existing wine.
     */
    fun addWine(
        collectionId: String,
        target: WineTarget,
        vintage: VintageInput?,
        lot: LotInput?,
        tags: List<String> = emptyList(),
        allowDuplicateWine: Boolean = false,
    ): String = database.write {
        val vintageId = resolveTarget(collectionId, target, vintage, allowDuplicateWine)
        if (lot != null && lot.quantity > 0) addLotInScope(collectionId, vintageId, lot)
        if (tags.isNotEmpty()) setTagsInScope(collectionId, vintageId, tags)
        vintageId
    }

    internal fun WriteScope.resolveTarget(collectionId: String, target: WineTarget, vintage: VintageInput?, allowDuplicateWine: Boolean): String =
        when (target) {
            is WineTarget.ExistingVintage -> {
                val row = catalog.vintageById(target.vintageId).executeAsOneOrNull()
                require(row != null && row.deleted_at == null) { "Vintage not found" }
                target.vintageId
            }
            is WineTarget.ExistingWine -> {
                val v = requireNotNull(vintage) { "Vintage is required" }
                findVintage(target.wineId, v.vintage)?.id ?: insertVintage(collectionId, target.wineId, v)
            }
            is WineTarget.NewWine -> {
                val input = target.wine.validated()
                if (!allowDuplicateWine) {
                    val existing = findWinesByIdentity(collectionId, input.producer, input.name)
                    if (existing.isNotEmpty()) throw DuplicateWineException(existing.map { it.id })
                }
                val wineId = insertWine(collectionId, input)
                insertVintage(collectionId, wineId, requireNotNull(vintage) { "Vintage is required" })
            }
        }

    private fun WriteScope.insertWine(collectionId: String, input: WineInput): String {
        val id = database.newId()
        catalog.insertWine(
            id, collectionId, input.producer, TextNormalizer.normalize(input.producer), input.name, input.color?.name,
            input.style?.name, input.country, input.region, input.subRegion, input.appellation, input.classification,
            input.notes, now, now,
        )
        touched(SyncTables.WINE, id)
        input.grapes.forEachIndexed { i, g -> insertGrape(collectionId, id, g, i) }
        return id
    }

    private fun WriteScope.insertGrape(collectionId: String, wineId: String, g: GrapeShare, position: Int) {
        val gid = database.newId()
        catalog.insertGrape(gid, collectionId, wineId, g.grape.trim(), g.percent?.toLong(), position.toLong(), now, now)
        touched(SyncTables.WINE_GRAPE, gid)
    }

    private fun WriteScope.insertVintage(collectionId: String, wineId: String, v: VintageInput): String {
        validateVintage(v)
        val id = database.newId()
        catalog.insertVintage(
            id, collectionId, wineId, v.vintage.kind, v.vintage.yearOrNull?.toLong(), v.drinkFrom?.toLong(),
            v.drinkUntil?.toLong(), v.drinkWindowSource.clean(), v.abvBasisPoints?.toLong(), v.notes.clean(), null, now, now,
        )
        touched(SyncTables.VINTAGE, id)
        return id
    }

    private fun validateVintage(v: VintageInput) {
        require(v.drinkFrom == null || v.drinkUntil == null || v.drinkFrom <= v.drinkUntil) { "Drinking window must start before it ends" }
        require(v.abvBasisPoints == null || v.abvBasisPoints in 0..8000) { "Alcohol must be between 0% and 80%" }
    }

    fun updateWine(wineId: String, input: WineInput) = database.write {
        val row = requireNotNull(catalog.wineById(wineId).executeAsOneOrNull()) { "Wine not found" }
        val w = input.validated()
        catalog.updateWine(
            producer = w.producer, producerSort = TextNormalizer.normalize(w.producer), name = w.name, color = w.color?.name,
            style = w.style?.name, country = w.country, region = w.region, subRegion = w.subRegion, appellation = w.appellation,
            classification = w.classification, notes = w.notes, updatedAt = now, id = wineId,
        )
        touched(SyncTables.WINE, wineId)
        val existing = catalog.grapesForWine(wineId).executeAsList()
        val wanted = w.grapes
        if (existing.map { GrapeShare(it.grape, it.percent?.toInt()) } != wanted) {
            existing.forEach { catalog.softDeleteGrape(now, it.id); touched(SyncTables.WINE_GRAPE, it.id) }
            wanted.forEachIndexed { i, g -> insertGrape(row.collection_id, wineId, g, i) }
        }
    }

    /**
     * Updates vintage details. Changing the year to one that already exists for this wine is
     * refused with [VintageExistsException]; combining two vintages is only possible through
     * [mergeVintages] with explicit confirmation.
     */
    fun updateVintage(vintageId: String, input: VintageInput) = database.write {
        val current = requireNotNull(catalog.vintageById(vintageId).executeAsOneOrNull()) { "Vintage not found" }
        validateVintage(input)
        val clash = findVintage(current.wine_id, input.vintage)
        if (clash != null && clash.id != vintageId) throw VintageExistsException(clash.id)
        catalog.updateVintage(
            kind = input.vintage.kind, year = input.vintage.yearOrNull?.toLong(), drinkFrom = input.drinkFrom?.toLong(),
            drinkUntil = input.drinkUntil?.toLong(), drinkWindowSource = input.drinkWindowSource.clean(),
            abvBp = input.abvBasisPoints?.toLong(), notes = input.notes.clean(), labelPhotoId = current.label_photo_id,
            updatedAt = now, id = vintageId,
        )
        touched(SyncTables.VINTAGE, vintageId)
    }

    fun setLabelPhoto(vintageId: String, photoId: String?) = database.write {
        val v = requireNotNull(catalog.vintageById(vintageId).executeAsOneOrNull())
        catalog.updateVintage(v.vintage_kind, v.year, v.drink_from, v.drink_until, v.drink_window_source, v.abv_bp, v.notes, photoId, now, vintageId)
        touched(SyncTables.VINTAGE, vintageId)
    }

    /**
     * Moves everything from [sourceVintageId] into [targetVintageId] and retires the source.
     * When the two vintages differ (different years, or year vs NV), [confirmDifferentVintage]
     * must be true — this is the explicit confirmation the data-integrity rules require.
     */
    fun mergeVintages(sourceVintageId: String, targetVintageId: String, confirmDifferentVintage: Boolean) = database.write {
        require(sourceVintageId != targetVintageId) { "Choose two different records" }
        val source = requireNotNull(catalog.vintageById(sourceVintageId).executeAsOneOrNull()) { "Source not found" }
        val target = requireNotNull(catalog.vintageById(targetVintageId).executeAsOneOrNull()) { "Target not found" }
        val sameVintage = source.vintage_kind == target.vintage_kind && source.year == target.year
        require(sameVintage || confirmDifferentVintage) { "These are different vintages; confirm the merge explicitly" }
        val reassign = listOf("lot", "tasting", "valuation", "vintage_tag", "barcode", "wishlist_item")
        for (table in reassign) {
            val ids = idsWhere("SELECT id FROM $table WHERE vintage_id = ?", sourceVintageId)
            for (id in ids) {
                database.driver.execute(null, "UPDATE $table SET vintage_id = ?, updated_at = ? WHERE id = ?", 3) {
                    bindString(0, targetVintageId); bindLong(1, now); bindString(2, id)
                }
                touched(SyncTables.byName(table), id)
            }
        }
        catalog.softDeleteVintage(now, sourceVintageId)
        touched(SyncTables.VINTAGE, sourceVintageId)
        touched(SyncTables.VINTAGE, targetVintageId)
    }

    /** Soft-deletes a vintage. Refused while bottles remain; tastings and history are kept. */
    fun deleteVintage(vintageId: String) = database.write {
        val qty = inventory.quantityForVintage(vintageId).executeAsOne().toInt()
        if (qty > 0) throw StockRemainingException(qty)
        catalog.softDeleteVintage(now, vintageId)
        touched(SyncTables.VINTAGE, vintageId)
    }

    // ---------------------------------------------------------------------------------------------
    // Lots and stock

    fun addLot(collectionId: String, vintageId: String, lot: LotInput): String = database.write {
        addLotInScope(collectionId, vintageId, lot)
    }

    internal fun WriteScope.addLotInScope(collectionId: String, vintageId: String, lot: LotInput): String {
        require(lot.quantity >= 1) { "Quantity must be at least 1" }
        require(lot.formatMl > 0) { "Bottle size is required" }
        require(lot.unitPrice == null || !lot.unitPrice.isNegative) { "Price can't be negative" }
        val lotId = database.newId()
        inventory.insertLot(
            lotId, collectionId, vintageId, lot.formatMl.toLong(), lot.acquiredOn?.toString(), lot.source.clean(),
            lot.orderRef.clean(), lot.unitPrice?.minor, lot.unitPrice?.currency?.code, lot.packaging?.name,
            lot.bottlesPerCase?.toLong(), lot.wishlistItemId, lot.notes.clean(), now, now,
        )
        touched(SyncTables.LOT, lotId)
        val m = planner.receive(LedgerPlanner.Context(collectionId, now), lotId, lot.locationId, lot.quantity, lot.kind,
            amount = lot.unitPrice?.let { it * lot.quantity }, counterparty = lot.source.clean())
        insertMovement(m)
        return lotId
    }

    fun updateLot(lotId: String, lot: LotInput) = database.write {
        requireNotNull(inventory.lotById(lotId).executeAsOneOrNull()) { "Purchase not found" }
        inventory.updateLot(
            formatMl = lot.formatMl.toLong(), acquiredOn = lot.acquiredOn?.toString(), source = lot.source.clean(),
            orderRef = lot.orderRef.clean(), unitPriceMinor = lot.unitPrice?.minor, unitPriceCurrency = lot.unitPrice?.currency?.code,
            packaging = lot.packaging?.name, bottlesPerCase = lot.bottlesPerCase?.toLong(), notes = lot.notes.clean(),
            updatedAt = now, id = lotId,
        )
        touched(SyncTables.LOT, lotId)
    }

    private fun currentStock(lotId: String): StockProjection {
        val levels = inventory.stockForLot(lotId).executeAsList().associate {
            StockKey(it.lot_id, it.location_key.ifEmpty { null }) to it.quantity.toInt()
        }
        return StockProjection.ofLevels(levels)
    }

    private fun WriteScope.insertMovement(m: Movement) {
        inventory.insertMovement(
            m.id, m.collectionId, m.lotId, m.locationId, m.delta.toLong(), m.kind.name, m.occurredAt, m.transferGroupId,
            m.reversalOf, m.amount?.minor, m.amount?.currency?.code, m.counterparty, m.note, m.deviceId, m.tastingId, m.createdAt,
        )
        touched(SyncTables.MOVEMENT, m.id)
    }

    private fun collectionOfLot(lotId: String): String =
        requireNotNull(inventory.lotById(lotId).executeAsOneOrNull()) { "Purchase not found" }.collection_id

    /** Records bottles leaving the collection (drunk, sold, given, lost). Returns the movement. */
    fun depart(
        lotId: String,
        locationId: String?,
        quantity: Int,
        kind: MovementKind,
        amount: Money? = null,
        counterparty: String? = null,
        note: String? = null,
        occurredAt: Long? = null,
    ): Movement = database.write { departInScope(lotId, locationId, quantity, kind, amount, counterparty, note, occurredAt, null) }

    internal fun WriteScope.departInScope(
        lotId: String, locationId: String?, quantity: Int, kind: MovementKind, amount: Money?, counterparty: String?,
        note: String?, occurredAt: Long?, tastingId: String?,
    ): Movement {
        val ctx = LedgerPlanner.Context(collectionOfLot(lotId), now)
        val m = planner.depart(ctx, currentStock(lotId), lotId, locationId, quantity, kind, occurredAt ?: now,
            amount, counterparty.clean(), note.clean(), tastingId)
        insertMovement(m)
        releaseSlots(lotId, locationId)
        return m
    }

    /** "Drink 2 bottles of this vintage": allocates across lots/locations (oldest purchase first). */
    fun departFromVintage(vintageId: String, quantity: Int, kind: MovementKind, note: String? = null,
                          policy: StockAllocator.Policy = StockAllocator.Policy.OLDEST_PURCHASE_FIRST): List<Movement> = database.write {
        val buckets = stockLines(vintageId).map {
            StockAllocator.Bucket(StockKey(it.lotId, it.locationId), it.quantity, it.acquiredOn?.epochDay, it.locationName)
        }
        StockAllocator.allocate(buckets, quantity, policy).map { draw ->
            departInScope(draw.key.lotId, draw.key.locationId, draw.quantity, kind, null, null, note, null, null)
        }
    }

    fun transfer(lotId: String, fromLocationId: String?, toLocationId: String?, quantity: Int, note: String? = null): List<Movement> =
        database.write {
            val ctx = LedgerPlanner.Context(collectionOfLot(lotId), now)
            val legs = planner.transfer(ctx, currentStock(lotId), lotId, fromLocationId, toLocationId, quantity, note.clean())
            legs.forEach { insertMovement(it) }
            releaseSlots(lotId, fromLocationId)
            legs
        }

    fun adjust(lotId: String, locationId: String?, countedQuantity: Int, note: String?): Movement? = database.write {
        val ctx = LedgerPlanner.Context(collectionOfLot(lotId), now)
        planner.adjustTo(ctx, currentStock(lotId), lotId, locationId, countedQuantity, note.clean())?.also {
            insertMovement(it)
            releaseSlots(lotId, locationId)
        }
    }

    /** Undoes a movement (both legs for a transfer) by appending reversal rows. */
    fun reverse(movementId: String, note: String? = null): List<Movement> = database.write {
        val target = requireNotNull(inventory.movementById(movementId).executeAsOneOrNull()) { "Movement not found" }.toDomain()
        val legs = target.transferGroupId?.let { g -> inventory.movementsInTransferGroup(g).executeAsList().map { it.toDomain() } }
            ?: listOf(target)
        val already = inventory.reversedIds(legs.map { it.id }).executeAsList().mapNotNull { it.reversal_of }.toSet()
        val ctx = LedgerPlanner.Context(target.collectionId, now)
        val reversals = planner.reverse(ctx, currentStock(target.lotId), legs, already, note.clean())
        reversals.forEach { insertMovement(it) }
        legs.mapNotNull { it.locationId }.distinct().forEach { releaseSlots(target.lotId, it) }
        reversals
    }

    /** Keeps grid positions consistent with stock: clears the most recently filled slots first. */
    private fun WriteScope.releaseSlots(lotId: String, locationId: String?) {
        if (locationId == null) return
        val stock = inventory.stockBucket(lotId, locationId).executeAsOneOrNull()?.toInt() ?: 0
        val slots = inventory.slotsForLotAt(lotId, locationId).executeAsList()
        val excess = slots.size - stock.coerceAtLeast(0)
        if (excess <= 0) return
        slots.take(excess).forEach { s ->
            inventory.setSlotLot(null, now, s.id)
            touched(SyncTables.SLOT, s.id)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Tags and barcodes

    fun setTags(collectionId: String, vintageId: String, names: List<String>) = database.write {
        setTagsInScope(collectionId, vintageId, names)
    }

    internal fun WriteScope.setTagsInScope(collectionId: String, vintageId: String, names: List<String>) {
        val wanted = names.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        val links = catalog.vintageTagLinks(vintageId).executeAsList()
        val current = catalog.tagsForVintage(vintageId).executeAsList()
        val wantedLower = wanted.map { it.lowercase() }.toSet()
        for (link in links) {
            val tag = current.firstOrNull { it.id == link.tag_id }
            if (tag == null || tag.name.lowercase() !in wantedLower) {
                catalog.softDeleteVintageTag(now, link.id)
                touched(SyncTables.VINTAGE_TAG, link.id)
            }
        }
        val have = current.map { it.name.lowercase() }.toSet()
        for (name in wanted.filter { it.lowercase() !in have }) {
            require(name.length <= 40) { "Tag names are at most 40 characters" }
            val tagId = catalog.tagByName(collectionId, name).executeAsOneOrNull()?.id ?: run {
                val id = database.newId()
                catalog.insertTag(id, collectionId, name, null, now, now)
                touched(SyncTables.TAG, id)
                id
            }
            val linkId = database.newId()
            catalog.insertVintageTag(linkId, collectionId, vintageId, tagId, now, now)
            touched(SyncTables.VINTAGE_TAG, linkId)
        }
    }

    fun allTags(collectionId: String): List<Pair<Tag, Int>> =
        catalog.tagsForCollection(collectionId).executeAsList().map {
            Tag(it.id, it.collection_id, it.name, it.color) to it.uses.toInt()
        }

    fun linkBarcode(collectionId: String, code: String, symbology: String, wineId: String, vintageId: String?, formatMl: Int?) =
        database.write {
            val existing = catalog.barcodesForCode(collectionId, code).executeAsList()
            if (existing.any { it.wine_id == wineId && it.vintage_id == vintageId && it.format_ml == formatMl?.toLong() }) return@write
            val id = database.newId()
            catalog.insertBarcode(id, collectionId, code, symbology, wineId, vintageId, formatMl?.toLong(), now, now)
            touched(SyncTables.BARCODE, id)
        }

    // ---------------------------------------------------------------------------------------------
    // Valuations (estimates always carry source, currency and date)

    fun valuations(vintageId: String): List<com.houseofvino.core.model.Valuation> =
        database.journal.valuationsForVintage(vintageId).executeAsList().map { it.toDomain() }

    fun addValuation(collectionId: String, vintageId: String, formatMl: Int, amount: Money, source: String, asOf: IsoDate, note: String?): String =
        database.write {
            com.houseofvino.core.model.Valuation("new", collectionId, vintageId, formatMl, amount, source.trim(), asOf, note) // validates
            val id = database.newId()
            database.journal.insertValuation(id, collectionId, vintageId, formatMl.toLong(), amount.minor, amount.currency.code,
                source.trim(), asOf.toString(), note.clean(), now, now)
            touched(SyncTables.VALUATION, id)
            id
        }

    fun deleteValuation(id: String) = database.write {
        database.journal.softDeleteValuation(now, id)
        touched(SyncTables.VALUATION, id)
    }

    // ---------------------------------------------------------------------------------------------
    // Integrity

    /** Cache vs ledger drift; always empty unless the database was tampered with. */
    fun stockDrift(): Int = inventory.stockDrift().executeAsList().size

    fun rebuildStockCache() = database.db.transaction {
        inventory.clearStockLevels()
        inventory.rebuildStockLevels()
    }

    private fun idsWhere(sql: String, arg: String): List<String> =
        database.driver.executeQuery(null, sql, { c ->
            val out = ArrayList<String>()
            while (c.next().value) out += c.getString(0)!!
            app.cash.sqldelight.db.QueryResult.Value(out)
        }, 1) { bindString(0, arg) }.value
}

internal fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

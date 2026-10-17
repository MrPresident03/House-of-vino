package com.houseofvino.core.csv

import com.houseofvino.core.model.BottleFormats
import com.houseofvino.core.model.GrapeShare
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.ExcessPrecision
import com.houseofvino.core.money.Money
import com.houseofvino.core.text.TextNormalizer
import com.houseofvino.core.time.DateOrder
import com.houseofvino.core.time.FlexibleDateParser
import com.houseofvino.core.time.IsoDate

/** Target fields a CSV column can be mapped to. */
enum class ImportField(val label: String, val required: Boolean = false) {
    PRODUCER("Producer", required = true),
    WINE_NAME("Wine / cuvée"),
    VINTAGE("Vintage"),
    COLOR("Colour"),
    STYLE("Style"),
    COUNTRY("Country"),
    REGION("Region"),
    SUB_REGION("Sub-region"),
    APPELLATION("Appellation"),
    CLASSIFICATION("Classification"),
    GRAPES("Grapes"),
    FORMAT("Bottle size"),
    QUANTITY("Quantity"),
    PURCHASE_DATE("Purchase date"),
    UNIT_PRICE("Price per bottle"),
    TOTAL_PRICE("Total price"),
    CURRENCY("Currency"),
    SOURCE("Bought from"),
    LOCATION("Location"),
    BIN("Bin / position"),
    DRINK_FROM("Drink from"),
    DRINK_UNTIL("Drink until"),
    TAGS("Tags"),
    NOTES("Notes"),
    BARCODE("Barcode"),
    ;

    companion object {
        /** Header synonyms (normalised) including CellarTracker, Vivino and common spreadsheet exports. */
        val SYNONYMS: Map<ImportField, List<String>> = mapOf(
            PRODUCER to listOf("producer", "winery", "domaine", "chateau", "estate", "maker", "brand"),
            WINE_NAME to listOf("wine", "wine name", "name", "cuvee", "label", "designation"),
            VINTAGE to listOf("vintage", "year", "millesime", "annata", "anada"),
            COLOR to listOf("color", "colour", "type"),
            STYLE to listOf("style", "category"),
            COUNTRY to listOf("country"),
            REGION to listOf("region"),
            SUB_REGION to listOf("subregion", "sub region"),
            APPELLATION to listOf("appellation", "aoc", "denomination", "ava", "doc"),
            CLASSIFICATION to listOf("classification", "cru"),
            GRAPES to listOf("grapes", "grape", "varietal", "variety", "varietals", "masterVarietal", "master varietal", "cepage"),
            FORMAT to listOf("size", "bottle size", "format", "volume", "ml"),
            QUANTITY to listOf("quantity", "qty", "bottles", "count", "number of bottles", "stock", "on hand"),
            PURCHASE_DATE to listOf("purchase date", "purchased", "date purchased", "bought", "acquired", "date acquired", "delivery date"),
            UNIT_PRICE to listOf("price", "unit price", "price per bottle", "bottle price", "cost", "purchase price"),
            TOTAL_PRICE to listOf("total", "total price", "total cost", "line total"),
            CURRENCY to listOf("currency", "ccy"),
            SOURCE to listOf("store", "merchant", "supplier", "source", "vendor", "retailer", "bought from"),
            LOCATION to listOf("location", "cellar", "storage"),
            BIN to listOf("bin", "position", "slot", "rack"),
            DRINK_FROM to listOf("drink from", "begin consume", "beginconsume", "window start", "from"),
            DRINK_UNTIL to listOf("drink until", "drink by", "end consume", "endconsume", "window end", "until", "to"),
            TAGS to listOf("tags", "labels", "lists"),
            NOTES to listOf("notes", "note", "comments", "comment", "memo"),
            BARCODE to listOf("barcode", "upc", "ean", "gtin"),
        )
    }
}

data class ImportOptions(
    val defaultCurrency: CurrencyCode,
    val dateOrder: DateOrder = DateOrder.YMD,
    val decimalSeparator: Char? = null,
    val defaultFormatMl: Int = BottleFormats.STANDARD,
    val defaultQuantity: Int = 1,
)

/** Column index per field; at most one column per field. */
data class ColumnMapping(val columns: Map<ImportField, Int>) {
    operator fun get(field: ImportField): Int? = columns[field]

    fun missingRequired(): List<ImportField> = ImportField.entries.filter { it.required && it !in columns }

    companion object {
        /**
         * Proposes a mapping from header names. Exact synonym matches win over partial ones,
         * and each column is used at most once.
         */
        fun suggest(header: List<String>): ColumnMapping {
            val normalised = header.map { TextNormalizer.normalize(it) }
            val result = LinkedHashMap<ImportField, Int>()
            val used = HashSet<Int>()
            // Pass 1: exact matches.
            for ((field, synonyms) in ImportField.SYNONYMS) {
                val idx = normalised.indices.firstOrNull { it !in used && normalised[it] in synonyms.map(TextNormalizer::normalize) }
                if (idx != null) { result[field] = idx; used += idx }
            }
            // Pass 2: header contains a multi-word synonym ("Purchase Date (UTC)").
            for ((field, synonyms) in ImportField.SYNONYMS) {
                if (field in result) continue
                val idx = normalised.indices.firstOrNull { i ->
                    i !in used && synonyms.map(TextNormalizer::normalize).any { s -> s.contains(' ') && normalised[i].contains(s) }
                }
                if (idx != null) { result[field] = idx; used += idx }
            }
            return ColumnMapping(result)
        }
    }
}

enum class Severity { ERROR, WARNING }

data class ImportIssue(val row: Int, val field: ImportField?, val severity: Severity, val message: String)

/** One fully parsed import row (one acquisition batch of one vintage). */
data class ImportRecord(
    val row: Int,
    val producer: String,
    val name: String?,
    val vintage: VintageYear,
    val color: WineColor?,
    val style: WineStyle?,
    val country: String?,
    val region: String?,
    val subRegion: String?,
    val appellation: String?,
    val classification: String?,
    val grapes: List<GrapeShare>,
    val formatMl: Int,
    val quantity: Int,
    val acquiredOn: IsoDate?,
    val unitPrice: Money?,
    val source: String?,
    val locationPath: List<String>,
    val bin: String?,
    val drinkFrom: Int?,
    val drinkUntil: Int?,
    val tags: List<String>,
    val notes: String?,
    val barcode: String?,
) {
    val identityKey: IdentityKey get() = IdentityKey.of(producer, name, vintage)
}

data class IdentityKey(val producer: String, val name: String, val vintage: String) {
    val wineKey: String get() = "$producer|$name"

    companion object {
        fun of(producer: String, name: String?, vintage: VintageYear) =
            IdentityKey(TextNormalizer.normalize(producer), TextNormalizer.normalize(name), vintage.toString())
    }
}

data class ParseResult(val records: List<ImportRecord>, val issues: List<ImportIssue>) {
    val errorRows: Set<Int> get() = issues.filter { it.severity == Severity.ERROR }.map { it.row }.toSet()
}

/** Turns CSV rows into validated [ImportRecord]s. Rows with errors are reported, never half-imported. */
class ImportParser(private val mapping: ColumnMapping, private val options: ImportOptions) {

    fun parse(table: Csv.Table): ParseResult {
        val records = ArrayList<ImportRecord>()
        val issues = ArrayList<ImportIssue>()
        mapping.missingRequired().forEach { issues += ImportIssue(0, it, Severity.ERROR, "Map a column to ${it.label}") }
        if (issues.isNotEmpty()) return ParseResult(emptyList(), issues)
        table.rows.forEachIndexed { index, cells ->
            val rowNumber = index + 2 // header is row 1 in spreadsheet terms
            val rowIssues = ArrayList<ImportIssue>()
            val record = parseRow(rowNumber, cells, rowIssues)
            issues += rowIssues
            if (record != null && rowIssues.none { it.severity == Severity.ERROR }) records += record
        }
        return ParseResult(records, issues)
    }

    private fun parseRow(row: Int, cells: List<String>, issues: MutableList<ImportIssue>): ImportRecord? {
        fun cell(f: ImportField): String? = mapping[f]?.let { cells.getOrNull(it) }?.trim()?.takeIf { it.isNotEmpty() }
        fun error(f: ImportField?, msg: String) { issues += ImportIssue(row, f, Severity.ERROR, msg) }
        fun warn(f: ImportField?, msg: String) { issues += ImportIssue(row, f, Severity.WARNING, msg) }

        val producer = cell(ImportField.PRODUCER)
        if (producer == null) { error(ImportField.PRODUCER, "Producer is empty"); return null }

        val vintageText = cell(ImportField.VINTAGE)
        val vintage = VintageYear.parse(vintageText) ?: run {
            error(ImportField.VINTAGE, "\"$vintageText\" isn't a vintage year, NV or blank"); VintageYear.Unknown
        }

        val formatText = cell(ImportField.FORMAT)
        val formatMl = if (formatText == null) options.defaultFormatMl else parseFormat(formatText) ?: run {
            error(ImportField.FORMAT, "\"$formatText\" isn't a bottle size (try 750ml, 1.5L or Magnum)"); options.defaultFormatMl
        }

        val qtyText = cell(ImportField.QUANTITY)
        val quantity = if (qtyText == null) options.defaultQuantity else qtyText.replace(" ", "").toIntOrNull() ?: run {
            error(ImportField.QUANTITY, "\"$qtyText\" isn't a whole number of bottles"); 0
        }
        if (quantity < 0) error(ImportField.QUANTITY, "Quantity can't be negative")
        if (quantity > 10_000) error(ImportField.QUANTITY, "Quantity $quantity looks wrong (over 10,000)")
        if (quantity == 0) warn(ImportField.QUANTITY, "Quantity is 0: the wine will be added without stock")

        val currency = cell(ImportField.CURRENCY)?.let { code ->
            CurrencyCode.ofOrNull(code) ?: run { error(ImportField.CURRENCY, "Unknown currency \"$code\""); null }
        } ?: options.defaultCurrency

        val unitPrice = cell(ImportField.UNIT_PRICE)?.let { parseMoney(it, currency, ImportField.UNIT_PRICE, ::error) }
        val totalPrice = cell(ImportField.TOTAL_PRICE)?.let { parseMoney(it, currency, ImportField.TOTAL_PRICE, ::error) }
        val price: Money? = when {
            unitPrice != null -> {
                if (totalPrice != null && quantity > 0 && unitPrice * quantity != totalPrice) {
                    warn(ImportField.TOTAL_PRICE, "Total ${totalPrice.toPlainString()} doesn't equal $quantity × ${unitPrice.toPlainString()}; using the unit price")
                }
                unitPrice
            }
            totalPrice != null && quantity > 0 -> {
                val split = totalPrice.allocate(quantity)
                if (split.distinct().size > 1) warn(ImportField.TOTAL_PRICE, "Total doesn't divide evenly; unit price rounded down by up to 1 minor unit")
                split.last()
            }
            else -> null
        }
        if (price != null && price.isNegative) error(ImportField.UNIT_PRICE, "Price can't be negative")

        val dateText = cell(ImportField.PURCHASE_DATE)
        val acquiredOn = dateText?.let { d ->
            FlexibleDateParser.parse(d, options.dateOrder) ?: run {
                error(ImportField.PURCHASE_DATE, "\"$d\" isn't a date in the chosen format (${options.dateOrder})"); null
            }
        }

        val drinkFrom = cell(ImportField.DRINK_FROM)?.let { parseYear(it, ImportField.DRINK_FROM, ::error) }
        val drinkUntil = cell(ImportField.DRINK_UNTIL)?.let { parseYear(it, ImportField.DRINK_UNTIL, ::error) }
        if (drinkFrom != null && drinkUntil != null && drinkFrom > drinkUntil) {
            error(ImportField.DRINK_UNTIL, "Drinking window ends ($drinkUntil) before it starts ($drinkFrom)")
        }

        val colorText = cell(ImportField.COLOR)
        val color = colorText?.let { parseColor(it) }
        val styleFromColor = colorText?.let { parseStyle(it) }
        if (colorText != null && color == null && styleFromColor == null) warn(ImportField.COLOR, "Couldn't read colour \"$colorText\"; left blank")
        val styleText = cell(ImportField.STYLE)
        val style = styleText?.let { parseStyle(it) } ?: styleFromColor

        val grapes = cell(ImportField.GRAPES)?.let { parseGrapes(it) }.orEmpty()
        val barcode = cell(ImportField.BARCODE)?.filter { it.isDigit() }?.takeIf { it.length in 8..14 }
        if (cell(ImportField.BARCODE) != null && barcode == null) warn(ImportField.BARCODE, "Barcode ignored: expected 8–14 digits")

        return ImportRecord(
            row = row, producer = producer, name = cell(ImportField.WINE_NAME), vintage = vintage,
            color = color, style = style, country = cell(ImportField.COUNTRY), region = cell(ImportField.REGION),
            subRegion = cell(ImportField.SUB_REGION), appellation = cell(ImportField.APPELLATION),
            classification = cell(ImportField.CLASSIFICATION), grapes = grapes, formatMl = formatMl,
            quantity = quantity.coerceAtLeast(0), acquiredOn = acquiredOn, unitPrice = price,
            source = cell(ImportField.SOURCE),
            locationPath = cell(ImportField.LOCATION)?.split('/', '>', '›', '\\')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
            bin = cell(ImportField.BIN), drinkFrom = drinkFrom, drinkUntil = drinkUntil,
            tags = cell(ImportField.TAGS)?.split(',', ';', '|')?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct().orEmpty(),
            notes = cell(ImportField.NOTES), barcode = barcode,
        )
    }

    private fun parseMoney(text: String, currency: CurrencyCode, field: ImportField, error: (ImportField?, String) -> Unit): Money? =
        Money.parseOrNull(text, currency, options.decimalSeparator, ExcessPrecision.HALF_EVEN)
            ?: run { error(field, "\"$text\" isn't an amount"); null }

    private fun parseYear(text: String, field: ImportField, error: (ImportField?, String) -> Unit): Int? {
        val y = text.trim().take(4).toIntOrNull()
        return if (y != null && y in 1700..2200) y else { error(field, "\"$text\" isn't a year"); null }
    }

    companion object {
        fun parseFormat(text: String): Int? {
            val t = TextNormalizer.normalize(text)
            BottleFormats.named.firstOrNull { TextNormalizer.normalize(it.name) == t }?.let { return it.ml }
            when (t) {
                "half", "demi", "halbe" -> return 375
                "standard", "bottle", "btl", "regular" -> return 750
                "magnum", "mag" -> return 1500
                "double magnum", "dmag" -> return 3000
                "jeroboam" -> return 3000 // Champagne/Burgundy convention; Bordeaux 5 L is listed by name
                "methuselah", "imperiale" -> return 6000
            }
            BottleFormats.parseVolume(text)?.let { return it }
            // Bare numbers: 750 → ml, 75 → cl, 1.5 → litres.
            val n = text.trim().replace(',', '.')
            n.toIntOrNull()?.let { return if (it in 100..30000) it else if (it in 10..99) it * 10 else null }
            val parts = n.split('.')
            if (parts.size == 2 && parts.all { p -> p.all { it.isDigit() } && p.isNotEmpty() }) {
                val ml = parts[0].toInt() * 1000 + parts[1].take(3).padEnd(3, '0').toInt()
                return ml.takeIf { it in 100..30000 }
            }
            return null
        }

        fun parseColor(text: String): WineColor? {
            val t = TextNormalizer.normalize(text)
            return when {
                t.contains("rose") || t.contains("rosado") || t.contains("rosato") -> WineColor.ROSE
                t.contains("orange") || t.contains("amber") -> WineColor.ORANGE
                t.contains("red") || t.contains("rouge") || t.contains("rosso") || t.contains("tinto") -> WineColor.RED
                t.contains("white") || t.contains("blanc") || t.contains("bianco") || t.contains("blanco") -> WineColor.WHITE
                else -> null
            }
        }

        fun parseStyle(text: String): WineStyle? {
            val t = TextNormalizer.normalize(text)
            return when {
                listOf("sparkling", "champagne", "cremant", "cava", "spumante", "sekt", "mousseux", "petillant", "pet nat").any { t.contains(it) } -> WineStyle.SPARKLING
                listOf("fortified", "port", "sherry", "madeira", "marsala", "vin doux").any { t.contains(it) } -> WineStyle.FORTIFIED
                listOf("sweet", "dessert", "liquoreux", "late harvest", "icewine", "ice wine", "tokaji", "sauternes").any { t.contains(it) } -> WineStyle.SWEET
                t == "still" || t == "dry" -> WineStyle.STILL
                else -> null
            }
        }

        private val PERCENT = Regex("""^(.*?)\s*\(?\s*(\d{1,3})\s*%\s*\)?$""")

        /** "Cabernet Sauvignon 70%, Merlot 30%" / "Syrah; Grenache" / "Merlot (85%)". */
        fun parseGrapes(text: String): List<GrapeShare> =
            text.split(',', ';', '/', '|', '+').map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { part ->
                val m = PERCENT.matchEntire(part)
                if (m != null && m.groupValues[1].isNotBlank()) {
                    val pct = m.groupValues[2].toInt()
                    GrapeShare(m.groupValues[1].trim(), pct.takeIf { it in 0..100 })
                } else {
                    val leadingPct = Regex("""^(\d{1,3})\s*%\s*(.+)$""").matchEntire(part)
                    if (leadingPct != null) GrapeShare(leadingPct.groupValues[2].trim(), leadingPct.groupValues[1].toInt().takeIf { it in 0..100 })
                    else GrapeShare(part, null)
                }
            }.distinctBy { TextNormalizer.normalize(it.grape) }
    }
}

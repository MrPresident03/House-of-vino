package com.houseofvino.data.catalogue

import app.cash.sqldelight.db.SqlDriver
import com.houseofvino.core.text.TextNormalizer

/**
 * The catalogue pack: a read-only SQLite file shipped inside the app (ADR-14, docs/CATALOGUE.md).
 * Built at compile time from tools/lwin/lwin.tsv.gz by the `:data:buildCataloguePack` task.
 */
object CataloguePack {
    const val FILE_NAME = "lwin.db"

    /** Bumped when the pack's layout changes. Stored as PRAGMA user_version. */
    const val FORMAT = 1

    /** TSV header the builder expects (tools/lwin/extract.py writes it). */
    val TSV_COLUMNS = listOf(
        "lwin", "producer_title", "producer_name", "wine", "country", "region", "sub_region", "site", "parcel",
        "colour", "type", "sub_type", "designation", "classification", "vintage_config", "first_vintage", "final_vintage",
    )

    private val SCHEMA = listOf(
        "CREATE TABLE meta (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)",
        "CREATE TABLE wine (lwin INTEGER NOT NULL PRIMARY KEY, producer_title TEXT, producer_name TEXT NOT NULL, wine TEXT, " +
            "country TEXT, region TEXT, sub_region TEXT, site TEXT, parcel TEXT, colour TEXT, type TEXT, sub_type TEXT, " +
            "designation TEXT, classification TEXT, first_vintage INTEGER, final_vintage INTEGER)",
        // Contentless: the text lives in `wine`; the index holds normalised tokens only (docid = lwin).
        "CREATE VIRTUAL TABLE wine_fts USING fts4(producer, wine, place, content=\"\", tokenize=simple)",
    )

    /** Writes the pack into an empty database on [driver]. [rows] are TSV rows keyed by [TSV_COLUMNS]. */
    fun build(driver: SqlDriver, rows: Sequence<Map<String, String>>, meta: Map<String, String>): Int {
        SCHEMA.forEach { driver.execute(null, it, 0) }
        var n = 0
        driver.execute(null, "BEGIN", 0)
        for (r in rows) {
            fun v(k: String) = r[k]?.trim()?.takeIf { it.isNotEmpty() }
            val lwin = v("lwin")?.toLongOrNull() ?: continue
            val producerName = v("producer_name") ?: continue
            driver.execute(null, "INSERT INTO wine (${LwinCatalogue.COLUMNS}) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", 16) {
                bindLong(0, lwin)
                listOf("producer_title", "producer_name", "wine", "country", "region", "sub_region", "site", "parcel",
                    "colour", "type", "sub_type", "designation", "classification").forEachIndexed { i, k -> bindString(i + 1, v(k)) }
                bindLong(14, v("first_vintage")?.toLongOrNull())
                bindLong(15, v("final_vintage")?.toLongOrNull())
            }
            val producer = TextNormalizer.normalize(listOfNotNull(v("producer_title"), producerName).joinToString(" "))
            val place = TextNormalizer.normalize(listOfNotNull(v("region"), v("sub_region"), v("site"), v("parcel"), v("classification")).joinToString(" "))
            driver.execute(null, "INSERT INTO wine_fts (docid, producer, wine, place) VALUES (?,?,?,?)", 4) {
                bindLong(0, lwin); bindString(1, producer); bindString(2, TextNormalizer.normalize(v("wine"))); bindString(3, place)
            }
            n++
        }
        for ((k, value) in meta + ("wines" to n.toString())) {
            driver.execute(null, "INSERT INTO meta (key, value) VALUES (?, ?)", 2) { bindString(0, k); bindString(1, value) }
        }
        driver.execute(null, "COMMIT", 0)
        driver.execute(null, "INSERT INTO wine_fts(wine_fts) VALUES ('optimize')", 0)
        driver.execute(null, "PRAGMA user_version = $FORMAT", 0)
        return n
    }

    /** Parses the TSV text format written by tools/lwin/extract.py. */
    fun parseTsv(lines: Sequence<String>): Sequence<Map<String, String>> {
        var header: List<String>? = null
        return lines.mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val cells = line.split('\t')
            val h = header
            if (h == null) {
                header = cells
                require(cells.containsAll(TSV_COLUMNS)) { "Unexpected catalogue columns: $cells" }
                null
            } else h.zip(cells).toMap()
        }
    }
}

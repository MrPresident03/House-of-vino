package com.houseofvino.data.search

import app.cash.sqldelight.db.QueryResult
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.search.SearchScope
import com.houseofvino.core.text.TextNormalizer
import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.db.bindAny

/**
 * Maintains the FTS4 full-text index. The index is derived data: it is created at runtime,
 * versioned via [FORMAT_VERSION], and can always be rebuilt from the base tables.
 *
 * One document per searchable entity:
 *  - CELLAR:   one per vintage (wine identity + vintage + grapes + tags + notes)
 *  - WISHLIST: one per wishlist item
 *  - JOURNAL:  one per tasting (wine identity + tasting notes, occasion, pairing…)
 * All text is normalised by [TextNormalizer] on the way in and on the way out.
 */
class SearchIndex(private val database: LocalDatabase) {

    fun ensureCreated() {
        val driver = database.driver
        driver.execute(null, "CREATE VIRTUAL TABLE IF NOT EXISTS search_fts USING fts4(" +
            "producer, name, vintage, place, grapes, tags, notes, tokenize=simple)", 0)
        driver.execute(null, "CREATE VIRTUAL TABLE IF NOT EXISTS search_terms USING fts4aux(search_fts)", 0)
        val current = database.local.getSetting(VERSION_KEY).executeAsOneOrNull()
        if (current != FORMAT_VERSION.toString()) rebuildAll()
    }

    /** Drops every document and re-indexes all entities. Used after format changes and in recovery. */
    fun rebuildAll() {
        database.db.transaction {
            database.driver.execute(null, "DELETE FROM search_fts", 0)
            database.local.clearSearchDocs()
            entityIds("SELECT id FROM vintage WHERE deleted_at IS NULL").forEach { indexVintage(it) }
            entityIds("SELECT id FROM wishlist_item WHERE deleted_at IS NULL").forEach { indexWishlist(it) }
            entityIds("SELECT id FROM tasting WHERE deleted_at IS NULL").forEach { indexTasting(it) }
            database.local.putSetting(VERSION_KEY, FORMAT_VERSION.toString())
        }
    }

    /** Re-indexes every document affected by changes to the given (table, id) rows. */
    fun reindexForChanges(changes: List<Pair<String, String>>) {
        val vintages = LinkedHashSet<String>()
        val wishlist = LinkedHashSet<String>()
        val tastings = LinkedHashSet<String>()
        for ((table, id) in changes) {
            when (table) {
                "wine" -> {
                    vintages += entityIds("SELECT id FROM vintage WHERE wine_id = ?", id)
                    wishlist += entityIds("SELECT id FROM wishlist_item WHERE wine_id = ? OR vintage_id IN (SELECT id FROM vintage WHERE wine_id = ?)", id, id)
                    tastings += entityIds("SELECT t.id FROM tasting t JOIN vintage v ON v.id = t.vintage_id WHERE v.wine_id = ?", id)
                }
                "wine_grape" -> entityIds("SELECT wine_id FROM wine_grape WHERE id = ?", id).forEach { wineId ->
                    vintages += entityIds("SELECT id FROM vintage WHERE wine_id = ?", wineId)
                }
                "vintage" -> {
                    vintages += id
                    wishlist += entityIds("SELECT id FROM wishlist_item WHERE vintage_id = ?", id)
                    tastings += entityIds("SELECT id FROM tasting WHERE vintage_id = ?", id)
                }
                "vintage_tag" -> vintages += entityIds("SELECT vintage_id FROM vintage_tag WHERE id = ?", id)
                "tag" -> vintages += entityIds("SELECT vintage_id FROM vintage_tag WHERE tag_id = ?", id)
                "lot" -> vintages += entityIds("SELECT vintage_id FROM lot WHERE id = ?", id)
                "wishlist_item" -> wishlist += id
                "tasting" -> tastings += id
            }
        }
        vintages.forEach { indexVintage(it) }
        wishlist.forEach { indexWishlist(it) }
        tastings.forEach { indexTasting(it) }
    }

    private fun indexVintage(vintageId: String) {
        val row = database.driver.executeQuery(null, """
            SELECT w.producer, w.name, v.vintage_kind, v.year, w.country, w.region, w.sub_region, w.appellation,
                   w.classification, w.color, w.style, w.notes, v.notes, v.deleted_at, w.deleted_at,
                   (SELECT group_concat(g.grape, ' ') FROM wine_grape g WHERE g.wine_id = w.id AND g.deleted_at IS NULL),
                   (SELECT group_concat(t.name, ' ') FROM vintage_tag vt JOIN tag t ON t.id = vt.tag_id
                      WHERE vt.vintage_id = v.id AND vt.deleted_at IS NULL AND t.deleted_at IS NULL),
                   (SELECT group_concat(l.source, ' ') FROM lot l WHERE l.vintage_id = v.id AND l.deleted_at IS NULL)
            FROM vintage v JOIN wine w ON w.id = v.wine_id WHERE v.id = ?
        """.trimIndent(), { c ->
            if (!c.next().value) return@executeQuery QueryResult.Value(null)
            val deleted = c.getLong(13) != null || c.getLong(14) != null
            QueryResult.Value(if (deleted) null else Doc(
                producer = c.getString(0),
                name = c.getString(1),
                vintage = vintageText(c.getString(2)!!, c.getLong(3)),
                place = listOfNotNull(c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8)).joinToString(" "),
                grapes = c.getString(15),
                tags = c.getString(16),
                notes = listOfNotNull(c.getString(9)?.let(::colorWords), c.getString(10)?.let(::styleWords), c.getString(11), c.getString(12), c.getString(17)).joinToString(" "),
            ))
        }, 1) { bindString(0, vintageId) }.value
        upsert(SearchScope.CELLAR, vintageId, row)
    }

    private fun indexWishlist(itemId: String) {
        val row = database.driver.executeQuery(null, """
            SELECT coalesce(w.producer, w2.producer, i.producer), coalesce(w.name, w2.name, i.name),
                   coalesce(v.vintage_kind, i.vintage_kind), coalesce(v.year, i.year),
                   coalesce(w.region, w2.region), coalesce(w.appellation, w2.appellation), coalesce(w.country, w2.country),
                   i.notes, i.deleted_at,
                   (SELECT group_concat(ml.merchant, ' ') FROM merchant_link ml WHERE ml.wishlist_item_id = i.id AND ml.deleted_at IS NULL)
            FROM wishlist_item i
            LEFT JOIN vintage v ON v.id = i.vintage_id
            LEFT JOIN wine w ON w.id = v.wine_id
            LEFT JOIN wine w2 ON w2.id = i.wine_id
            WHERE i.id = ?
        """.trimIndent(), { c ->
            if (!c.next().value) return@executeQuery QueryResult.Value(null)
            QueryResult.Value(if (c.getLong(8) != null) null else Doc(
                producer = c.getString(0), name = c.getString(1),
                vintage = c.getString(2)?.let { vintageText(it, c.getLong(3)) },
                place = listOfNotNull(c.getString(4), c.getString(5), c.getString(6)).joinToString(" "),
                grapes = null, tags = null,
                notes = listOfNotNull(c.getString(7), c.getString(9)).joinToString(" "),
            ))
        }, 1) { bindString(0, itemId) }.value
        upsert(SearchScope.WISHLIST, itemId, row)
    }

    private fun indexTasting(tastingId: String) {
        val row = database.driver.executeQuery(null, """
            SELECT w.producer, w.name, v.vintage_kind, v.year, w.region, w.appellation, w.country,
                   t.notes, t.aromas, t.occasion, t.food_pairing, t.companions, t.place, t.deleted_at,
                   (SELECT group_concat(g.grape, ' ') FROM wine_grape g WHERE g.wine_id = w.id AND g.deleted_at IS NULL)
            FROM tasting t JOIN vintage v ON v.id = t.vintage_id JOIN wine w ON w.id = v.wine_id
            WHERE t.id = ?
        """.trimIndent(), { c ->
            if (!c.next().value) return@executeQuery QueryResult.Value(null)
            QueryResult.Value(if (c.getLong(13) != null) null else Doc(
                producer = c.getString(0), name = c.getString(1), vintage = vintageText(c.getString(2)!!, c.getLong(3)),
                place = listOfNotNull(c.getString(4), c.getString(5), c.getString(6), c.getString(12)).joinToString(" "),
                grapes = c.getString(14), tags = null,
                notes = listOfNotNull(c.getString(7), c.getString(8), c.getString(9), c.getString(10), c.getString(11)).joinToString(" "),
            ))
        }, 1) { bindString(0, tastingId) }.value
        upsert(SearchScope.JOURNAL, tastingId, row)
    }

    private data class Doc(
        val producer: String?, val name: String?, val vintage: String?, val place: String?,
        val grapes: String?, val tags: String?, val notes: String?,
    )

    private fun upsert(scope: SearchScope, entityId: String, doc: Doc?) {
        val key = "${scope.name}:$entityId"
        val existing = database.local.searchDocByKey(key).executeAsOneOrNull()
        if (existing != null) {
            database.driver.execute(null, "DELETE FROM search_fts WHERE docid = ?", 1) { bindLong(0, existing.docid) }
            if (doc == null) {
                database.local.deleteSearchDoc(existing.docid)
                return
            }
        }
        if (doc == null) return
        val docid = existing?.docid ?: run {
            database.local.insertSearchDoc(key, scope.name, entityId)
            database.local.lastInsertRowId().executeAsOne()
        }
        val values = listOf(doc.producer, doc.name, doc.vintage, doc.place, doc.grapes, doc.tags, doc.notes)
            .map { TextNormalizer.normalize(it) }
        database.driver.execute(null,
            "INSERT INTO search_fts(docid, producer, name, vintage, place, grapes, tags, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", 8) {
            bindLong(0, docid)
            values.forEachIndexed { i, v -> bindString(i + 1, v) }
        }
    }

    private fun entityIds(sql: String, vararg args: String): List<String> =
        database.driver.executeQuery(null, sql, { c ->
            val out = ArrayList<String>()
            while (c.next().value) c.getString(0)?.let(out::add)
            QueryResult.Value(out)
        }, args.size) { args.forEachIndexed { i, a -> bindAny(i, a) } }.value

    private fun colorWords(color: String) = when (color) {
        "RED" -> "red rouge"; "WHITE" -> "white blanc"; "ROSE" -> "rose"; "ORANGE" -> "orange"; else -> ""
    }

    private fun styleWords(style: String) = when (style) {
        "SPARKLING" -> "sparkling"; "FORTIFIED" -> "fortified"; "SWEET" -> "sweet dessert"; else -> ""
    }

    companion object {
        const val FORMAT_VERSION = 1
        const val VERSION_KEY = "search_index_version"

        fun vintageText(kind: String, year: Long?): String = when (kind) {
            "YEAR" -> year.toString()
            "NV" -> "nv nonvintage"
            else -> ""
        }

        fun vintageOf(kind: String?, year: Long?): VintageYear? =
            kind?.let { VintageYear.fromStorage(it, year?.toInt()) }
    }
}

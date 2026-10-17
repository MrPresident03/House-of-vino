package com.houseofvino.data.sync

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Reads and writes whole rows of synced tables as JSON objects keyed by column name. This is the
 * single place that turns local rows into the wire format and back.
 */
object RowCodec {

    fun read(driver: SqlDriver, table: SyncTable, id: String, includeLocalOnly: Boolean = false): JsonObject? {
        val cols = if (includeLocalOnly) table.columns else table.syncedColumns
        val sql = "SELECT ${cols.joinToString { quote(it.first) }} FROM ${quote(table.name)} WHERE id = ?"
        return driver.executeQuery(null, sql, { cursor ->
            if (!cursor.next().value) return@executeQuery QueryResult.Value(null)
            val map = LinkedHashMap<String, JsonElement>()
            cols.forEachIndexed { i, (name, type) ->
                map[name] = when (type) {
                    ColType.TEXT -> cursor.getString(i)?.let(::JsonPrimitive) ?: JsonNull
                    ColType.INTEGER -> cursor.getLong(i)?.let(::JsonPrimitive) ?: JsonNull
                }
            }
            QueryResult.Value(JsonObject(map))
        }, 1) { bindString(0, id) }.value
    }

    /**
     * Writes a server row locally. Mutable tables are upserted (only synced columns are touched,
     * so device-local columns survive). Append-only rows are inserted if absent; an existing
     * append-only row only gets its `rev` filled in.
     *
     * @return true if a row was inserted or changed.
     */
    fun applyRemote(driver: SqlDriver, table: SyncTable, row: JsonObject): Boolean {
        val cols = table.syncedColumns
        val id = (row["id"] as? JsonPrimitive)?.content ?: throw IllegalArgumentException("Row without id in ${table.name}")
        val placeholders = cols.joinToString { "?" }
        val names = cols.joinToString { quote(it.first) }
        val sql = if (table.appendOnly) {
            "INSERT OR IGNORE INTO ${quote(table.name)}($names) VALUES ($placeholders)"
        } else {
            val updates = cols.filter { it.first != "id" }.joinToString { "${quote(it.first)} = excluded.${quote(it.first)}" }
            "INSERT INTO ${quote(table.name)}($names) VALUES ($placeholders) ON CONFLICT(id) DO UPDATE SET $updates"
        }
        val changed = driver.execute(null, sql, cols.size) {
            cols.forEachIndexed { i, (name, type) -> bindValue(this, i, type, row[name], table.name, name) }
        }.value
        if (table.appendOnly && changed == 0L) {
            val rev = (row["rev"] as? JsonPrimitive)?.longOrNull
            if (rev != null) {
                driver.execute(null, "UPDATE ${quote(table.name)} SET rev = ? WHERE id = ? AND rev IS NULL", 2) {
                    bindLong(0, rev); bindString(1, id)
                }
            }
            return false
        }
        return changed > 0
    }

    fun setRev(driver: SqlDriver, table: SyncTable, id: String, rev: Long) {
        driver.execute(null, "UPDATE ${quote(table.name)} SET rev = ? WHERE id = ?", 2) { bindLong(0, rev); bindString(1, id) }
    }

    fun rev(driver: SqlDriver, table: SyncTable, id: String): Long? =
        driver.executeQuery(null, "SELECT rev FROM ${quote(table.name)} WHERE id = ?", { c ->
            QueryResult.Value(if (c.next().value) c.getLong(0) else null)
        }, 1) { bindString(0, id) }.value

    private fun bindValue(
        stmt: app.cash.sqldelight.db.SqlPreparedStatement, index: Int, type: ColType, value: JsonElement?,
        table: String, column: String,
    ) {
        if (value == null || value is JsonNull) {
            when (type) {
                ColType.TEXT -> stmt.bindString(index, null)
                ColType.INTEGER -> stmt.bindLong(index, null)
            }
            return
        }
        val p = value as? JsonPrimitive ?: throw IllegalArgumentException("$table.$column: expected a scalar")
        when (type) {
            ColType.TEXT -> stmt.bindString(index, p.content)
            ColType.INTEGER -> stmt.bindLong(index, p.longOrNull ?: when (p.content) {
                "true" -> 1L
                "false" -> 0L
                else -> throw IllegalArgumentException("$table.$column: expected an integer, got ${p.content}")
            })
        }
    }

    /** Identifiers come only from [SyncTables], never from input; quoting is defensive. */
    internal fun quote(identifier: String): String {
        require(identifier.all { it.isLetterOrDigit() || it == '_' }) { "Bad identifier $identifier" }
        return "\"$identifier\""
    }
}

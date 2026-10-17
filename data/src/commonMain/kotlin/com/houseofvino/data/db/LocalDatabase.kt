package com.houseofvino.data.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.houseofvino.core.id.IdGenerator
import com.houseofvino.core.time.Clock
import com.houseofvino.data.search.SearchIndex
import com.houseofvino.data.sync.Journal
import com.houseofvino.data.sync.SyncTable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * The local database plus the write discipline that keeps it consistent: every write runs in a
 * transaction, journals each changed synced row for household sync, and refreshes the search
 * index for affected documents, all before commit.
 */
class LocalDatabase(
    val driver: SqlDriver,
    val clock: Clock,
    val ids: IdGenerator,
    deviceId: String,
) {
    /** This install's identity in the household. Replaced when the phone is erased. */
    var deviceId: String = deviceId
        private set

    val db = HouseOfVinoDatabase(driver)
    val catalog get() = db.catalogQueries
    val inventory get() = db.inventoryQueries
    val journal get() = db.journalQueries
    val local get() = db.localQueries
    val sync get() = db.syncQueries

    val searchIndex = SearchIndex(this)
    val changeLog = Journal(this)

    private val _changes = MutableSharedFlow<Set<String>>(extraBufferCapacity = 64)

    /** Emits the set of table names changed by each committed write. UI observers re-query. */
    val changes: SharedFlow<Set<String>> = _changes.asSharedFlow()

    init {
        searchIndex.ensureCreated()
    }

    /**
     * Runs [block] in a transaction. Changes recorded with [WriteScope.touched] are journaled
     * (unless [fromSync]: they came from another phone and are journaled already) and re-indexed
     * before the transaction commits.
     */
    fun <T> write(fromSync: Boolean = false, block: WriteScope.() -> T): T {
        val scope = WriteScope(this)
        val result = db.transactionWithResult {
            val r = scope.block()
            scope.flush(enqueue = !fromSync)
            r
        }
        if (scope.tables.isNotEmpty()) _changes.tryEmit(scope.tables.toSet())
        return result
    }

    fun now(): Long = clock.nowMillis()

    fun newId(): String = ids.newId()

    /** Executes a raw statement (used for pragmas and maintenance). */
    fun exec(sql: String) {
        driver.execute(null, sql, 0)
    }

    fun queryLong(sql: String, vararg args: Any?): Long? = driver.executeQuery(null, sql, { c ->
        QueryResult.Value(if (c.next().value) c.getLong(0) else null)
    }, args.size) { args.forEachIndexed { i, a -> bindAny(i, a) } }.value

    /**
     * Erases every synced row and all sync bookkeeping on this phone ("Erase this phone"),
     * including the collection row and the household key. The phone gets a new device id. Immutability
     * triggers are bypassed only for the duration of this transaction. Returns the local paths of
     * photo files, which the caller deletes.
     */
    fun wipeAll(): List<String> {
        val photoPaths = ArrayList<String>()
        db.transaction {
            driver.executeQuery(null, "SELECT local_path FROM photo WHERE local_path IS NOT NULL", { c ->
                while (c.next().value) c.getString(0)?.let(photoPaths::add)
                QueryResult.Value(Unit)
            }, 0)
            local.setMaintenance(1)
            // Self-references use ON DELETE RESTRICT, which SQLite enforces row by row even inside
            // one DELETE statement, so those tables are emptied leaves-first.
            deleteLeavesFirst("movement", "reversal_of")
            deleteLeavesFirst("location", "parent_id")
            for (table in com.houseofvino.data.sync.SyncTables.ALL.reversed()) driver.execute(null, "DELETE FROM \"${table.name}\"", 0)
            // `collection` goes too: the caller starts a new local collection with a fresh id.
            for (table in listOf("stock_level", "journal", "row_clock", "peer", "household_key", "sync_conflict", "recent_query", "search_doc", "search_fts", "collection")) {
                driver.execute(null, "DELETE FROM $table", 0)
            }
            driver.execute(null, "DELETE FROM setting WHERE setting_key NOT IN ('active_collection', 'search_index_version')", 0)
            // A new identity: this phone's old change numbers may already be on other phones.
            deviceId = ids.newId()
            local.putSetting(com.houseofvino.data.repo.SettingsRepository.KEY_DEVICE_ID, deviceId)
            local.setMaintenance(0)
        }
        _changes.tryEmit(com.houseofvino.data.sync.SyncTables.ALL.map { it.name }.toSet() + setOf("setting", "collection"))
        return photoPaths
    }

    private fun deleteLeavesFirst(table: String, parentColumn: String) {
        repeat(10_000) {
            if ((queryLong("SELECT count(*) FROM $table") ?: 0L) == 0L) return
            val removed = driver.execute(null,
                "DELETE FROM $table WHERE id NOT IN (SELECT $parentColumn FROM $table WHERE $parentColumn IS NOT NULL)", 0).value
            if (removed == 0L) return // a cycle; the plain DELETE that follows will report it
        }
    }

    fun notifyChanged(tables: Set<String>) {
        _changes.tryEmit(tables)
    }
}

class WriteScope internal constructor(private val database: LocalDatabase) {
    private val touchedRows = LinkedHashMap<Pair<String, String>, SyncTable>()
    internal val tables = LinkedHashSet<String>()

    val now: Long = database.now()

    /** Records that a synced row was inserted or changed in this transaction. */
    fun touched(table: SyncTable, id: String) {
        touchedRows[table.name to id] = table
        tables += table.name
    }

    /** Records a change to a local-only table so observers refresh. */
    fun touchedLocal(table: String) {
        tables += table
    }

    internal fun flush(enqueue: Boolean) {
        if (touchedRows.isEmpty()) return
        val rows = touchedRows.map { (key, table) -> table to key.second }
        if (enqueue) rows.forEach { (table, id) -> database.changeLog.record(table, id) }
        database.searchIndex.reindexForChanges(rows.map { it.first.name to it.second })
    }
}

internal fun app.cash.sqldelight.db.SqlPreparedStatement.bindAny(index: Int, value: Any?) {
    when (value) {
        null -> bindString(index, null)
        is String -> bindString(index, value)
        is Long -> bindLong(index, value)
        is Int -> bindLong(index, value.toLong())
        is Boolean -> bindLong(index, if (value) 1L else 0L)
        is ByteArray -> bindBytes(index, value)
        else -> throw IllegalArgumentException("Unsupported bind type ${value::class}")
    }
}

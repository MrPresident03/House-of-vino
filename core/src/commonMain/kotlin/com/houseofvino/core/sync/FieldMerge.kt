package com.houseofvino.core.sync

/** A synced column value. Columns are TEXT or INTEGER, so this is all the wire needs. */
sealed interface SyncValue {
    data object Null : SyncValue { override fun toString() = "null" }
    data class Text(val value: String) : SyncValue { override fun toString() = "\"$value\"" }
    data class Num(val value: Long) : SyncValue { override fun toString() = value.toString() }

    companion object {
        fun of(value: Any?): SyncValue = when (value) {
            null -> Null
            is String -> Text(value)
            is Long -> Num(value)
            is Int -> Num(value.toLong())
            is Boolean -> Num(if (value) 1 else 0)
            else -> throw IllegalArgumentException("Unsupported sync value ${value::class}")
        }
    }
}

/** The current value of one field and the clock of the write that set it. */
data class FieldState(val value: SyncValue, val clock: Hlc)

/** One field of a change: the new value and the clock of the value it replaced ([base]). */
data class FieldWrite(val column: String, val value: SyncValue, val base: Hlc?)

/** Two writes to the same field made without seeing each other, with different values. */
data class FieldConflict(
    val column: String,
    val kept: SyncValue,
    val keptClock: Hlc,
    val lost: SyncValue,
    val lostClock: Hlc,
)

/**
 * Field-level last-writer-wins merge. Pure and deterministic: applying the same set of changes
 * in any causally consistent order gives the same state on every device.
 */
object FieldMerge {

    class Result(
        val state: Map<String, FieldState>,
        /** Columns whose value changed, with their new value. */
        val changed: Map<String, SyncValue>,
        /** Independent writes with different values; the losing value is reported here. */
        val conflicts: List<FieldConflict>,
        /** Columns that received a write made with knowledge of the current value. */
        val fastForwarded: Set<String>,
    )

    /** Applies the writes of one change stamped [clock] to [state]. */
    fun receive(state: Map<String, FieldState>, clock: Hlc, writes: List<FieldWrite>): Result {
        val next = LinkedHashMap(state)
        val changed = LinkedHashMap<String, SyncValue>()
        val conflicts = ArrayList<FieldConflict>()
        val fastForwarded = LinkedHashSet<String>()
        for (w in writes) {
            val cur = next[w.column]
            when {
                cur == null -> {
                    next[w.column] = FieldState(w.value, clock)
                    changed[w.column] = w.value
                }
                cur.clock == clock -> Unit // duplicate delivery
                w.base == cur.clock -> {
                    next[w.column] = FieldState(w.value, clock)
                    if (w.value != cur.value) changed[w.column] = w.value
                    fastForwarded += w.column
                }
                else -> {
                    // Made without seeing the current value: highest clock wins everywhere.
                    val incomingWins = clock > cur.clock
                    if (w.value != cur.value) {
                        conflicts += if (incomingWins) {
                            FieldConflict(w.column, w.value, clock, cur.value, cur.clock)
                        } else {
                            FieldConflict(w.column, cur.value, cur.clock, w.value, clock)
                        }
                    }
                    if (incomingWins) {
                        next[w.column] = FieldState(w.value, clock)
                        if (w.value != cur.value) changed[w.column] = w.value
                    }
                }
            }
        }
        return Result(next, changed, conflicts, fastForwarded)
    }

    /**
     * The writes a local edit produces: every column of [row] whose value differs from [state]
     * (all of them for a new row), each based on the clock it replaces.
     */
    fun localWrites(state: Map<String, FieldState>, row: Map<String, SyncValue>): List<FieldWrite> =
        row.mapNotNull { (column, value) ->
            val cur = state[column]
            if (cur != null && cur.value == value) null else FieldWrite(column, value, cur?.clock)
        }
}

package com.houseofvino.core.inventory

import com.houseofvino.core.id.IdGenerator
import com.houseofvino.core.money.Money

/** Kinds of inventory movement. The sign of a movement's delta is fixed by its kind. */
enum class MovementKind(val sign: Sign) {
    PURCHASE(Sign.POSITIVE),
    /** Bottles that existed before tracking began (imports, first setup). */
    OPENING(Sign.POSITIVE),
    CONSUME(Sign.NEGATIVE),
    SELL(Sign.NEGATIVE),
    GIFT_OUT(Sign.NEGATIVE),
    GIFT_IN(Sign.POSITIVE),
    /** Breakage, spoilage, theft, corked and discarded. */
    LOSS(Sign.NEGATIVE),
    TRANSFER_OUT(Sign.NEGATIVE),
    TRANSFER_IN(Sign.POSITIVE),
    /** Stock-take correction. */
    ADJUST(Sign.EITHER),
    /** Undo of an earlier movement; exact negation of it. */
    REVERSAL(Sign.EITHER);

    enum class Sign { POSITIVE, NEGATIVE, EITHER }

    fun accepts(delta: Int): Boolean = delta != 0 && when (sign) {
        Sign.POSITIVE -> delta > 0
        Sign.NEGATIVE -> delta < 0
        Sign.EITHER -> true
    }

    /** True for movements that take bottles out of the collection for good. */
    val isDeparture: Boolean get() = this == CONSUME || this == SELL || this == GIFT_OUT || this == LOSS
}

/** One immutable ledger row. `locationId == null` means "not yet put away". */
data class Movement(
    val id: String,
    val collectionId: String,
    val lotId: String,
    val locationId: String?,
    val delta: Int,
    val kind: MovementKind,
    val occurredAt: Long,
    val transferGroupId: String? = null,
    val reversalOf: String? = null,
    val amount: Money? = null,
    val counterparty: String? = null,
    val note: String? = null,
    val deviceId: String,
    val tastingId: String? = null,
    val createdAt: Long,
) {
    init {
        require(kind.accepts(delta)) { "$kind cannot have delta $delta" }
        require((kind == MovementKind.TRANSFER_IN || kind == MovementKind.TRANSFER_OUT) == (transferGroupId != null)) {
            "Transfers, and only transfers, carry a transfer group id"
        }
        require((kind == MovementKind.REVERSAL) == (reversalOf != null)) {
            "Reversals, and only reversals, reference the reversed movement"
        }
    }
}

/** Key of one stock bucket: a lot at a location (or unassigned). */
data class StockKey(val lotId: String, val locationId: String?)

/** Current stock derived from the ledger. */
class StockProjection private constructor(private val levels: Map<StockKey, Int>) {

    fun quantity(key: StockKey): Int = levels[key] ?: 0
    fun quantity(lotId: String, locationId: String?): Int = quantity(StockKey(lotId, locationId))
    fun totalForLot(lotId: String): Int = levels.entries.filter { it.key.lotId == lotId }.sumOf { it.value }
    fun nonZero(): Map<StockKey, Int> = levels.filterValues { it != 0 }

    /** Buckets whose quantity is below zero — evidence of a conflict (e.g. concurrent consumption). */
    fun negativeBuckets(): Map<StockKey, Int> = levels.filterValues { it < 0 }

    companion object {
        fun of(movements: Iterable<Movement>): StockProjection {
            val map = HashMap<StockKey, Int>()
            for (m in movements) {
                val key = StockKey(m.lotId, m.locationId)
                map[key] = (map[key] ?: 0) + m.delta
            }
            return StockProjection(map)
        }

        fun ofLevels(levels: Map<StockKey, Int>) = StockProjection(levels)
    }
}

sealed class LedgerError(message: String) : Exception(message) {
    class InsufficientStock(val key: StockKey, val available: Int, val requested: Int) :
        LedgerError("Only $available bottle(s) available, $requested requested")
    class InvalidQuantity(val quantity: Int) : LedgerError("Quantity must be at least 1 (was $quantity)")
    class SameLocation : LedgerError("Source and destination are the same location")
    class AlreadyReversed(val movementId: String) : LedgerError("Movement $movementId has already been reversed")
    class NotReversible(val movementId: String) : LedgerError("Movement $movementId cannot be reversed")
    class WouldGoNegative(val key: StockKey, val resulting: Int) :
        LedgerError("Reversal would leave ${-resulting} bottle(s) below zero")
}

/**
 * Plans ledger rows for stock operations. Planning is pure: callers persist the returned
 * rows in a single transaction after re-checking stock inside that transaction.
 */
class LedgerPlanner(
    private val ids: IdGenerator,
    private val deviceId: String,
) {
    data class Context(val collectionId: String, val now: Long)

    fun receive(
        ctx: Context,
        lotId: String,
        locationId: String?,
        quantity: Int,
        kind: MovementKind = MovementKind.PURCHASE,
        occurredAt: Long = ctx.now,
        amount: Money? = null,
        counterparty: String? = null,
        note: String? = null,
    ): Movement {
        if (quantity < 1) throw LedgerError.InvalidQuantity(quantity)
        require(kind.sign == MovementKind.Sign.POSITIVE) { "$kind is not an incoming movement" }
        return Movement(
            id = ids.newId(), collectionId = ctx.collectionId, lotId = lotId, locationId = locationId,
            delta = quantity, kind = kind, occurredAt = occurredAt, amount = amount,
            counterparty = counterparty, note = note, deviceId = deviceId, createdAt = ctx.now,
        )
    }

    fun depart(
        ctx: Context,
        stock: StockProjection,
        lotId: String,
        locationId: String?,
        quantity: Int,
        kind: MovementKind,
        occurredAt: Long = ctx.now,
        amount: Money? = null,
        counterparty: String? = null,
        note: String? = null,
        tastingId: String? = null,
    ): Movement {
        if (quantity < 1) throw LedgerError.InvalidQuantity(quantity)
        require(kind.isDeparture) { "$kind is not a departure" }
        val key = StockKey(lotId, locationId)
        val available = stock.quantity(key)
        if (available < quantity) throw LedgerError.InsufficientStock(key, available, quantity)
        return Movement(
            id = ids.newId(), collectionId = ctx.collectionId, lotId = lotId, locationId = locationId,
            delta = -quantity, kind = kind, occurredAt = occurredAt, amount = amount,
            counterparty = counterparty, note = note, deviceId = deviceId, tastingId = tastingId,
            createdAt = ctx.now,
        )
    }

    /** Two rows with a shared group id; equal and opposite. */
    fun transfer(
        ctx: Context,
        stock: StockProjection,
        lotId: String,
        fromLocationId: String?,
        toLocationId: String?,
        quantity: Int,
        note: String? = null,
    ): List<Movement> {
        if (quantity < 1) throw LedgerError.InvalidQuantity(quantity)
        if (fromLocationId == toLocationId) throw LedgerError.SameLocation()
        val from = StockKey(lotId, fromLocationId)
        val available = stock.quantity(from)
        if (available < quantity) throw LedgerError.InsufficientStock(from, available, quantity)
        val group = ids.newId()
        val base = Movement(
            id = ids.newId(), collectionId = ctx.collectionId, lotId = lotId, locationId = fromLocationId,
            delta = -quantity, kind = MovementKind.TRANSFER_OUT, occurredAt = ctx.now, transferGroupId = group,
            note = note, deviceId = deviceId, createdAt = ctx.now,
        )
        return listOf(
            base,
            base.copy(id = ids.newId(), locationId = toLocationId, delta = quantity, kind = MovementKind.TRANSFER_IN),
        )
    }

    /** Sets the bucket to [countedQuantity] (stock-take). Returns null when nothing changes. */
    fun adjustTo(
        ctx: Context,
        stock: StockProjection,
        lotId: String,
        locationId: String?,
        countedQuantity: Int,
        note: String?,
    ): Movement? {
        require(countedQuantity >= 0) { "Counted quantity cannot be negative" }
        val delta = countedQuantity - stock.quantity(lotId, locationId)
        if (delta == 0) return null
        return Movement(
            id = ids.newId(), collectionId = ctx.collectionId, lotId = lotId, locationId = locationId,
            delta = delta, kind = MovementKind.ADJUST, occurredAt = ctx.now, note = note,
            deviceId = deviceId, createdAt = ctx.now,
        )
    }

    /**
     * Reverses [targets] (a single movement, or both legs of a transfer). Reversal rows negate
     * the originals exactly. Refuses if any target is already reversed, is itself a reversal,
     * or if undoing it would push a bucket below zero.
     */
    fun reverse(
        ctx: Context,
        stock: StockProjection,
        targets: List<Movement>,
        alreadyReversedIds: Set<String>,
        note: String? = null,
    ): List<Movement> {
        require(targets.isNotEmpty())
        for (t in targets) {
            if (t.kind == MovementKind.REVERSAL) throw LedgerError.NotReversible(t.id)
            if (t.id in alreadyReversedIds) throw LedgerError.AlreadyReversed(t.id)
        }
        val groups = targets.mapNotNull { it.transferGroupId }.toSet()
        require(groups.size <= 1 && (groups.isEmpty() || targets.size == 2)) {
            "A transfer must be reversed as a pair"
        }
        val resulting = HashMap<StockKey, Int>()
        for (t in targets) {
            val key = StockKey(t.lotId, t.locationId)
            resulting[key] = (resulting[key] ?: stock.quantity(key)) - t.delta
        }
        resulting.entries.firstOrNull { it.value < 0 }?.let { throw LedgerError.WouldGoNegative(it.key, it.value) }
        return targets.map { t ->
            Movement(
                id = ids.newId(), collectionId = ctx.collectionId, lotId = t.lotId, locationId = t.locationId,
                delta = -t.delta, kind = MovementKind.REVERSAL, occurredAt = ctx.now, reversalOf = t.id,
                note = note, deviceId = deviceId, createdAt = ctx.now,
            )
        }
    }
}

/** Chooses which buckets to draw from when the user asks for N bottles of a vintage. */
object StockAllocator {
    data class Bucket(val key: StockKey, val quantity: Int, val acquiredEpochDay: Long?, val locationName: String?)
    data class Draw(val key: StockKey, val quantity: Int)

    enum class Policy { OLDEST_PURCHASE_FIRST, NEWEST_PURCHASE_FIRST, LARGEST_BUCKET_FIRST }

    fun allocate(buckets: List<Bucket>, quantity: Int, policy: Policy = Policy.OLDEST_PURCHASE_FIRST): List<Draw> {
        if (quantity < 1) throw LedgerError.InvalidQuantity(quantity)
        val available = buckets.filter { it.quantity > 0 }
        val total = available.sumOf { it.quantity }
        if (total < quantity) {
            throw LedgerError.InsufficientStock(available.firstOrNull()?.key ?: StockKey("", null), total, quantity)
        }
        val ordered = when (policy) {
            Policy.OLDEST_PURCHASE_FIRST -> available.sortedWith(compareBy({ it.acquiredEpochDay ?: Long.MIN_VALUE }, { it.key.lotId }))
            Policy.NEWEST_PURCHASE_FIRST -> available.sortedWith(compareByDescending<Bucket> { it.acquiredEpochDay ?: Long.MIN_VALUE }.thenBy { it.key.lotId })
            Policy.LARGEST_BUCKET_FIRST -> available.sortedWith(compareByDescending<Bucket> { it.quantity }.thenBy { it.key.lotId })
        }
        var remaining = quantity
        val draws = ArrayList<Draw>()
        for (b in ordered) {
            if (remaining == 0) break
            val take = minOf(b.quantity, remaining)
            draws += Draw(b.key, take)
            remaining -= take
        }
        return draws
    }
}

/** Validates a set of movements received from sync against the ledger's invariants. */
object LedgerAudit {
    data class Finding(val description: String, val movementIds: List<String>)

    fun audit(movements: List<Movement>): List<Finding> {
        val findings = ArrayList<Finding>()
        val byId = movements.associateBy { it.id }
        movements.filter { it.transferGroupId != null }.groupBy { it.transferGroupId }.forEach { (group, legs) ->
            val sum = legs.sumOf { it.delta }
            if (legs.size != 2 || sum != 0) {
                findings += Finding("Transfer $group is unbalanced (${legs.size} legs, net $sum)", legs.map { it.id })
            }
        }
        movements.filter { it.kind == MovementKind.REVERSAL }.groupBy { it.reversalOf }.forEach { (target, revs) ->
            val original = byId[target]
            if (original == null) {
                findings += Finding("Reversal of unknown movement $target", revs.map { it.id })
            } else {
                if (revs.size > 1) findings += Finding("Movement $target reversed ${revs.size} times", revs.map { it.id })
                if (revs.any { it.delta != -original.delta || it.lotId != original.lotId || it.locationId != original.locationId }) {
                    findings += Finding("Reversal does not mirror movement $target", revs.map { it.id })
                }
            }
        }
        StockProjection.of(movements).negativeBuckets().forEach { (key, qty) ->
            findings += Finding("Stock for lot ${key.lotId} at ${key.locationId ?: "unassigned"} is $qty",
                movements.filter { it.lotId == key.lotId && it.locationId == key.locationId }.map { it.id })
        }
        return findings
    }
}

package com.houseofvino.core.inventory

import com.houseofvino.core.id.IdGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LedgerTest {
    private val planner = LedgerPlanner(IdGenerator.sequential(), deviceId = "device-a")
    private val ctx = LedgerPlanner.Context(collectionId = "c1", now = 1_000L)

    private fun receive(qty: Int, location: String? = "rack") =
        planner.receive(ctx, lotId = "lot1", locationId = location, quantity = qty)

    @Test
    fun signRulesAreEnforcedByKind() {
        assertTrue(MovementKind.PURCHASE.accepts(1))
        assertTrue(!MovementKind.PURCHASE.accepts(-1))
        assertTrue(MovementKind.CONSUME.accepts(-2))
        assertTrue(!MovementKind.CONSUME.accepts(2))
        assertTrue(MovementKind.ADJUST.accepts(-3) && MovementKind.ADJUST.accepts(3))
        MovementKind.entries.forEach { assertTrue(!it.accepts(0), "$it must reject zero") }
        assertFailsWith<IllegalArgumentException> {
            Movement("m", "c", "l", null, 1, MovementKind.CONSUME, 0, deviceId = "d", createdAt = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            Movement("m", "c", "l", null, 1, MovementKind.TRANSFER_IN, 0, deviceId = "d", createdAt = 0) // no group id
        }
    }

    @Test
    fun consumptionNeverExceedsStock() {
        val stock = StockProjection.of(listOf(receive(2)))
        val drink = planner.depart(ctx, stock, "lot1", "rack", 2, MovementKind.CONSUME)
        assertEquals(-2, drink.delta)
        val err = assertFailsWith<LedgerError.InsufficientStock> {
            planner.depart(ctx, stock, "lot1", "rack", 3, MovementKind.CONSUME)
        }
        assertEquals(2, err.available)
        assertFailsWith<LedgerError.InsufficientStock> { // stock is per location
            planner.depart(ctx, stock, "lot1", "fridge", 1, MovementKind.CONSUME)
        }
        assertFailsWith<LedgerError.InvalidQuantity> { planner.depart(ctx, stock, "lot1", "rack", 0, MovementKind.SELL) }
    }

    @Test
    fun transferIsBalancedAndAtomicPair() {
        val stock = StockProjection.of(listOf(receive(6)))
        val legs = planner.transfer(ctx, stock, "lot1", "rack", "fridge", 2)
        assertEquals(2, legs.size)
        assertEquals(0, legs.sumOf { it.delta })
        assertEquals(legs[0].transferGroupId, legs[1].transferGroupId)
        val after = StockProjection.of(listOf(receive(6)) + legs)
        assertEquals(4, after.quantity("lot1", "rack"))
        assertEquals(2, after.quantity("lot1", "fridge"))
        assertEquals(6, after.totalForLot("lot1"))
        assertFailsWith<LedgerError.SameLocation> { planner.transfer(ctx, stock, "lot1", "rack", "rack", 1) }
        assertFailsWith<LedgerError.InsufficientStock> { planner.transfer(ctx, stock, "lot1", "rack", "fridge", 7) }
    }

    @Test
    fun reversalUndoesExactlyOnce() {
        val purchase = receive(3)
        val drink = planner.depart(ctx, StockProjection.of(listOf(purchase)), "lot1", "rack", 1, MovementKind.CONSUME)
        val ledger = listOf(purchase, drink)
        val undo = planner.reverse(ctx, StockProjection.of(ledger), listOf(drink), alreadyReversedIds = emptySet())
        assertEquals(1, undo.single().delta)
        assertEquals(drink.id, undo.single().reversalOf)
        assertEquals(3, StockProjection.of(ledger + undo).quantity("lot1", "rack"))
        assertFailsWith<LedgerError.AlreadyReversed> {
            planner.reverse(ctx, StockProjection.of(ledger + undo), listOf(drink), alreadyReversedIds = setOf(drink.id))
        }
        assertFailsWith<LedgerError.NotReversible> {
            planner.reverse(ctx, StockProjection.of(ledger + undo), undo, alreadyReversedIds = emptySet())
        }
    }

    @Test
    fun cannotReverseAPurchaseWhoseBottlesWereDrunk() {
        val purchase = receive(1)
        val drink = planner.depart(ctx, StockProjection.of(listOf(purchase)), "lot1", "rack", 1, MovementKind.CONSUME)
        assertFailsWith<LedgerError.WouldGoNegative> {
            planner.reverse(ctx, StockProjection.of(listOf(purchase, drink)), listOf(purchase), emptySet())
        }
    }

    @Test
    fun transfersReverseAsAPair() {
        val purchase = receive(4)
        val legs = planner.transfer(ctx, StockProjection.of(listOf(purchase)), "lot1", "rack", "fridge", 4)
        val stock = StockProjection.of(listOf(purchase) + legs)
        assertFailsWith<IllegalArgumentException> { planner.reverse(ctx, stock, listOf(legs[0]), emptySet()) }
        val undo = planner.reverse(ctx, stock, legs, emptySet())
        val after = StockProjection.of(listOf(purchase) + legs + undo)
        assertEquals(4, after.quantity("lot1", "rack"))
        assertEquals(0, after.quantity("lot1", "fridge"))
    }

    @Test
    fun adjustmentSetsCountedQuantity() {
        val stock = StockProjection.of(listOf(receive(5)))
        assertEquals(-2, planner.adjustTo(ctx, stock, "lot1", "rack", 3, "stock take")!!.delta)
        assertEquals(1, planner.adjustTo(ctx, stock, "lot1", "rack", 6, null)!!.delta)
        assertNull(planner.adjustTo(ctx, stock, "lot1", "rack", 5, null))
    }

    @Test
    fun allocatorDrawsByPolicy() {
        val buckets = listOf(
            StockAllocator.Bucket(StockKey("old", "a"), 2, acquiredEpochDay = 100, locationName = "A"),
            StockAllocator.Bucket(StockKey("new", "b"), 5, acquiredEpochDay = 900, locationName = "B"),
            StockAllocator.Bucket(StockKey("empty", "c"), 0, acquiredEpochDay = 50, locationName = "C"),
        )
        assertEquals(
            listOf(StockAllocator.Draw(StockKey("old", "a"), 2), StockAllocator.Draw(StockKey("new", "b"), 1)),
            StockAllocator.allocate(buckets, 3),
        )
        assertEquals(listOf(StockAllocator.Draw(StockKey("new", "b"), 3)), StockAllocator.allocate(buckets, 3, StockAllocator.Policy.NEWEST_PURCHASE_FIRST))
        assertFailsWith<LedgerError.InsufficientStock> { StockAllocator.allocate(buckets, 8) }
    }

    @Test
    fun auditDetectsConcurrentOverConsumption() {
        // Two devices each drink "the last bottle" offline; after sync the ledger is merged.
        val purchase = receive(1)
        val stockSeen = StockProjection.of(listOf(purchase))
        val deviceB = LedgerPlanner(IdGenerator.sequential("00000000-0000-4000-9000-"), "device-b")
        val a = planner.depart(ctx, stockSeen, "lot1", "rack", 1, MovementKind.CONSUME)
        val b = deviceB.depart(ctx, stockSeen, "lot1", "rack", 1, MovementKind.CONSUME)
        val merged = listOf(purchase, a, b)
        assertEquals(-1, StockProjection.of(merged).quantity("lot1", "rack"))
        val findings = LedgerAudit.audit(merged)
        assertEquals(1, findings.size)
        assertTrue(findings.single().movementIds.containsAll(listOf(a.id, b.id)))
    }

    @Test
    fun auditDetectsBrokenTransfersAndBadReversals() {
        val purchase = receive(2)
        val legs = planner.transfer(ctx, StockProjection.of(listOf(purchase)), "lot1", "rack", "fridge", 1)
        assertTrue(LedgerAudit.audit(listOf(purchase) + legs).isEmpty())
        assertEquals(1, LedgerAudit.audit(listOf(purchase, legs[0])).count { "unbalanced" in it.description })
        val bogus = Movement("r1", "c1", "lot1", "rack", 5, MovementKind.REVERSAL, 0, reversalOf = purchase.id, deviceId = "x", createdAt = 0)
        assertTrue(LedgerAudit.audit(listOf(purchase, bogus)).any { "does not mirror" in it.description })
    }
}

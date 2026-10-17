package com.houseofvino.core.sync

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HlcTest {
    @Test fun encodingSortsLikeTheClock() {
        val values = listOf(Hlc(5, 0, "b"), Hlc(5, 1, "a"), Hlc(12, 0, "a"), Hlc(5, 0, "a"), Hlc(1_760_000_000_000, 3, "z"))
        assertEquals(values.sorted().map { it.encode() }, values.map { it.encode() }.sorted())
        for (v in values) assertEquals(v, Hlc.parse(v.encode()))
    }

    @Test fun nodeIdsWithDashesRoundTrip() {
        val h = Hlc(42, 7, "4f1c2d3e-aaaa-4bbb-8ccc-000000000001")
        assertEquals(h, Hlc.parse(h.encode()))
    }

    @Test fun malformedIsRejected() {
        assertFailsWith<IllegalArgumentException> { Hlc.parse("12-3-x") }
        assertEquals(null, Hlc.parseOrNull("nope"))
    }

    @Test fun ticksAreStrictlyIncreasingEvenWhenTheWallClockGoesBack() {
        var wall = 1000L
        val c = HlcClock("a", { wall })
        val a = c.tick()
        wall = 900 // clock adjusted backwards
        val b = c.tick()
        val d = c.tick()
        assertTrue(a < b && b < d)
        assertEquals(1000, d.millis)
    }

    @Test fun observingARemoteClockOrdersLaterLocalEventsAfterIt() {
        var wall = 1000L
        val local = HlcClock("a", { wall })
        val remote = Hlc(5000, 9, "b") // the other phone's clock is ahead
        local.observe(remote)
        val next = local.tick()
        assertTrue(next > remote)
        wall = 6000
        assertEquals(Hlc(6000, 0, "a"), local.tick())
    }

    @Test fun counterOverflowCarriesIntoMillis() {
        val c = HlcClock("a", { 10 }, Hlc(10, Hlc.MAX_COUNTER, "a"))
        assertEquals(Hlc(11, 0, "a"), c.tick())
    }
}

class FieldMergeTest {
    private fun t(s: String) = SyncValue.Text(s)
    private fun hlc(m: Long, node: String) = Hlc(m, 0, node)

    @Test fun firstWriteCreatesTheField() {
        val r = FieldMerge.receive(emptyMap(), hlc(1, "a"), listOf(FieldWrite("name", t("Margaux"), null)))
        assertEquals(t("Margaux"), r.state["name"]!!.value)
        assertEquals(mapOf("name" to t("Margaux")), r.changed)
        assertTrue(r.conflicts.isEmpty())
    }

    @Test fun aWriteBasedOnTheCurrentValueFastForwardsWithoutConflict() {
        val s = mapOf("name" to FieldState(t("Old"), hlc(1, "a")))
        val r = FieldMerge.receive(s, hlc(2, "b"), listOf(FieldWrite("name", t("New"), hlc(1, "a"))))
        assertEquals(t("New"), r.state["name"]!!.value)
        assertTrue(r.conflicts.isEmpty())
        assertEquals(setOf("name"), r.fastForwarded)
    }

    @Test fun independentWritesPickTheHigherClockAndReportTheLoser() {
        val base = hlc(1, "a")
        val s = mapOf("notes" to FieldState(t("mine"), hlc(5, "a")))
        // b edited from the same base without seeing "mine"; b's clock is later, so b wins.
        val r = FieldMerge.receive(s, hlc(7, "b"), listOf(FieldWrite("notes", t("theirs"), base)))
        assertEquals(t("theirs"), r.state["notes"]!!.value)
        assertEquals(listOf(FieldConflict("notes", t("theirs"), hlc(7, "b"), t("mine"), hlc(5, "a"))), r.conflicts)
        // Earlier independent write: current value stays and the incoming value is the loser.
        val r2 = FieldMerge.receive(s, hlc(3, "b"), listOf(FieldWrite("notes", t("older"), base)))
        assertEquals(t("mine"), r2.state["notes"]!!.value)
        assertEquals(t("older"), r2.conflicts.single().lost)
        assertTrue(r2.changed.isEmpty())
    }

    @Test fun independentWritesWithTheSameValueAreNotAConflict() {
        val s = mapOf("rating" to FieldState(SyncValue.Num(94), hlc(5, "a")))
        val r = FieldMerge.receive(s, hlc(6, "b"), listOf(FieldWrite("rating", SyncValue.Num(94), null)))
        assertTrue(r.conflicts.isEmpty())
    }

    @Test fun duplicatesAreIgnored() {
        val s = mapOf("name" to FieldState(t("X"), hlc(5, "a")))
        val r = FieldMerge.receive(s, hlc(5, "a"), listOf(FieldWrite("name", t("X"), null)))
        assertTrue(r.changed.isEmpty() && r.conflicts.isEmpty())
    }

    @Test fun localWritesContainOnlyChangedFieldsWithTheirBase() {
        val s = mapOf("a" to FieldState(t("1"), hlc(1, "x")), "b" to FieldState(t("2"), hlc(2, "x")))
        val w = FieldMerge.localWrites(s, mapOf("a" to t("1"), "b" to t("3"), "c" to SyncValue.Null))
        assertEquals(listOf(FieldWrite("b", t("3"), hlc(2, "x")), FieldWrite("c", SyncValue.Null, null)), w)
    }

    /**
     * Property: several replicas edit the same fields independently and exchange changes in
     * random causally consistent orders (with duplicates). They always converge, and every
     * field ends with the value of its highest-clocked write.
     */
    @Test fun replicasConvergeUnderRandomEditsAndDeliveryOrders() {
        val rnd = Random(20261018)
        repeat(300) { round ->
            val nodes = listOf("a", "b", "c")
            var wall = 1000L
            val clocks = nodes.associateWith { n -> HlcClock(n, { wall }) }
            val states = nodes.associateWith { mutableMapOf<String, FieldState>() }.toMutableMap()
            data class Change(val clock: Hlc, val writes: List<FieldWrite>)
            val logs = nodes.associateWith { mutableListOf<Change>() } // every change each node knows, in arrival order
            val seen = nodes.associateWith { mutableSetOf<Hlc>() }

            fun deliver(to: String, from: String) {
                // Causal: pass on everything the sender knows that the receiver lacks, in HLC order.
                val missing = logs.getValue(from).filter { it.clock !in seen.getValue(to) }.sortedBy { it.clock }
                for (c in missing + missing.take(rnd.nextInt(2))) { // occasionally re-deliver
                    if (c.clock in seen.getValue(to)) {
                        FieldMerge.receive(states.getValue(to), c.clock, c.writes).also { r ->
                            assertTrue(r.changed.isEmpty(), "duplicate changed state")
                        }
                        continue
                    }
                    clocks.getValue(to).observe(c.clock)
                    val r = FieldMerge.receive(states.getValue(to), c.clock, c.writes)
                    states[to] = r.state.toMutableMap()
                    logs.getValue(to) += c
                    seen.getValue(to) += c.clock
                }
            }

            repeat(30) {
                wall += rnd.nextLong(0, 3)
                val n = nodes.random(rnd)
                if (rnd.nextInt(3) == 0) {
                    deliver(n, (nodes - n).random(rnd))
                } else {
                    val row = mapOf("f${rnd.nextInt(3)}" to SyncValue.Num(rnd.nextLong(0, 4)))
                    val writes = FieldMerge.localWrites(states.getValue(n), row)
                    if (writes.isNotEmpty()) {
                        val clock = clocks.getValue(n).tick()
                        states[n] = FieldMerge.receive(states.getValue(n), clock, writes).state.toMutableMap()
                        logs.getValue(n) += Change(clock, writes)
                        seen.getValue(n) += clock
                    }
                }
            }
            // Gossip until everyone has everything.
            repeat(3) { for (to in nodes) for (from in nodes - to) deliver(to, from) }
            val reference = states.getValue("a").mapValues { it.value.value }
            for (n in nodes) assertEquals(reference, states.getValue(n).mapValues { it.value.value }, "round $round, node $n")
            val all = logs.getValue("a")
            for ((col, v) in reference) {
                val last = all.filter { c -> c.writes.any { it.column == col } }.maxBy { it.clock }
                assertEquals(last.writes.first { it.column == col }.value, v, "round $round field $col")
            }
        }
    }
}

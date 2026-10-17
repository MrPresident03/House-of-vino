package com.houseofvino.core.sync

/**
 * Hybrid logical clock timestamp (Kulkarni et al.): physical milliseconds plus a logical
 * counter, tie-broken by the device id. Totally ordered, and the [encode]d form sorts in the
 * same order, so it can be compared as text in SQL.
 */
data class Hlc(val millis: Long, val counter: Int, val node: String) : Comparable<Hlc> {
    init {
        require(millis in 0..MAX_MILLIS) { "HLC millis out of range: $millis" }
        require(counter in 0..MAX_COUNTER) { "HLC counter out of range: $counter" }
        require(node.isNotEmpty() && node.none { it.isWhitespace() }) { "HLC node must be a non-empty token" }
    }

    override fun compareTo(other: Hlc): Int =
        compareValuesBy(this, other, { it.millis }, { it.counter }, { it.node })

    fun encode(): String = millis.toString().padStart(15, '0') + "-" + counter.toString().padStart(6, '0') + "-" + node

    override fun toString(): String = encode()

    companion object {
        const val MAX_COUNTER = 999_999
        const val MAX_MILLIS = 999_999_999_999_999L

        fun parse(text: String): Hlc {
            val parts = text.split('-', limit = 3)
            require(parts.size == 3 && parts[0].length == 15 && parts[1].length == 6) { "Not an HLC: $text" }
            return Hlc(parts[0].toLong(), parts[1].toInt(), parts[2])
        }

        fun parseOrNull(text: String?): Hlc? = text?.let { runCatching { parse(it) }.getOrNull() }
    }
}

/**
 * Issues HLC timestamps for one device. [tick] for local events, [observe] for every received
 * timestamp, so the next local event sorts after everything this device has seen.
 */
class HlcClock(private val node: String, private val wall: () -> Long, initial: Hlc? = null) {
    var last: Hlc = initial ?: Hlc(0, 0, node)
        private set

    fun tick(): Hlc {
        val now = wall().coerceIn(0, Hlc.MAX_MILLIS)
        last = when {
            now > last.millis -> Hlc(now, 0, node)
            last.counter < Hlc.MAX_COUNTER -> Hlc(last.millis, last.counter + 1, node)
            else -> Hlc(last.millis + 1, 0, node)
        }
        return last
    }

    fun observe(remote: Hlc) {
        val now = wall().coerceIn(0, Hlc.MAX_MILLIS)
        val millis = maxOf(now, last.millis, remote.millis)
        val counter = when {
            millis == last.millis && millis == remote.millis -> maxOf(last.counter, remote.counter)
            millis == last.millis -> last.counter
            millis == remote.millis -> remote.counter
            else -> 0
        }
        last = Hlc(millis, counter, node)
    }
}

package com.houseofvino.core.crypto

/**
 * Poly1305 one-time authenticator (RFC 8439 §2.5), 26-bit limb arithmetic in 64-bit longs
 * (the "donna-32" layout). Every intermediate fits comfortably in a signed Long.
 */
internal object Poly1305 {
    private const val MASK26 = 0x3ffffffL

    private fun le32(b: ByteArray, at: Int): Long = b.leInt(at).toLong() and 0xffffffffL

    fun mac(message: ByteArray, key: ByteArray): ByteArray {
        require(key.size == 32) { "Poly1305 key must be 32 bytes" }
        // r, clamped
        val r0 = le32(key, 0) and 0x3ffffffL
        val r1 = (le32(key, 3) ushr 2) and 0x3ffff03L
        val r2 = (le32(key, 6) ushr 4) and 0x3ffc0ffL
        val r3 = (le32(key, 9) ushr 6) and 0x3f03fffL
        val r4 = (le32(key, 12) ushr 8) and 0x00fffffL
        val s1 = r1 * 5; val s2 = r2 * 5; val s3 = r3 * 5; val s4 = r4 * 5

        var h0 = 0L; var h1 = 0L; var h2 = 0L; var h3 = 0L; var h4 = 0L
        val block = ByteArray(17)
        var offset = 0
        while (offset < message.size) {
            val n = minOf(16, message.size - offset)
            block.fill(0)
            message.copyInto(block, 0, offset, offset + n)
            block[n] = 1 // the high bit: 2^(8n)
            val t0 = le32(block, 0); val t1 = le32(block, 4); val t2 = le32(block, 8); val t3 = le32(block, 12)
            val hi = (block[16].toLong() and 0xFF)
            h0 += t0 and MASK26
            h1 += ((t0 ushr 26) or (t1 shl 6)) and MASK26
            h2 += ((t1 ushr 20) or (t2 shl 12)) and MASK26
            h3 += ((t2 ushr 14) or (t3 shl 18)) and MASK26
            h4 += (t3 ushr 8) or (hi shl 24)

            val d0 = h0 * r0 + h1 * s4 + h2 * s3 + h3 * s2 + h4 * s1
            var d1 = h0 * r1 + h1 * r0 + h2 * s4 + h3 * s3 + h4 * s2
            var d2 = h0 * r2 + h1 * r1 + h2 * r0 + h3 * s4 + h4 * s3
            var d3 = h0 * r3 + h1 * r2 + h2 * r1 + h3 * r0 + h4 * s4
            var d4 = h0 * r4 + h1 * r3 + h2 * r2 + h3 * r1 + h4 * r0

            var c = d0 ushr 26; h0 = d0 and MASK26
            d1 += c; c = d1 ushr 26; h1 = d1 and MASK26
            d2 += c; c = d2 ushr 26; h2 = d2 and MASK26
            d3 += c; c = d3 ushr 26; h3 = d3 and MASK26
            d4 += c; c = d4 ushr 26; h4 = d4 and MASK26
            h0 += c * 5; c = h0 ushr 26; h0 = h0 and MASK26
            h1 += c
            offset += n
        }

        // Full carry, then compute h - p and select.
        var c = h1 ushr 26; h1 = h1 and MASK26
        h2 += c; c = h2 ushr 26; h2 = h2 and MASK26
        h3 += c; c = h3 ushr 26; h3 = h3 and MASK26
        h4 += c; c = h4 ushr 26; h4 = h4 and MASK26
        h0 += c * 5; c = h0 ushr 26; h0 = h0 and MASK26
        h1 += c

        var g0 = h0 + 5; c = g0 ushr 26; g0 = g0 and MASK26
        var g1 = h1 + c; c = g1 ushr 26; g1 = g1 and MASK26
        var g2 = h2 + c; c = g2 ushr 26; g2 = g2 and MASK26
        var g3 = h3 + c; c = g3 ushr 26; g3 = g3 and MASK26
        val g4 = h4 + c - (1L shl 26)
        // If g4 is non-negative, h >= p, so use g.
        val useG = (g4 ushr 63) xor 1L // 1 when g4 >= 0
        val maskG = -useG
        val maskH = maskG.inv()
        h0 = (h0 and maskH) or (g0 and maskG)
        h1 = (h1 and maskH) or (g1 and maskG)
        h2 = (h2 and maskH) or (g2 and maskG)
        h3 = (h3 and maskH) or (g3 and maskG)
        h4 = (h4 and maskH) or (g4 and maskG)

        // h = h % 2^128, then + s
        val f0 = ((h0) or (h1 shl 26)) and 0xffffffffL
        val f1 = ((h1 ushr 6) or (h2 shl 20)) and 0xffffffffL
        val f2 = ((h2 ushr 12) or (h3 shl 14)) and 0xffffffffL
        val f3 = ((h3 ushr 18) or (h4 shl 8)) and 0xffffffffL
        var acc = f0 + le32(key, 16)
        val out = ByteArray(16)
        out.putLeInt(0, acc.toInt())
        acc = f1 + le32(key, 20) + (acc ushr 32)
        out.putLeInt(4, acc.toInt())
        acc = f2 + le32(key, 24) + (acc ushr 32)
        out.putLeInt(8, acc.toInt())
        acc = f3 + le32(key, 28) + (acc ushr 32)
        out.putLeInt(12, acc.toInt())
        return out
    }
}

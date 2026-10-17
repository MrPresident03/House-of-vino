package com.houseofvino.core.qr

/**
 * QR Code encoder (ISO/IEC 18004), byte mode, versions 1–40. Used to show household invites;
 * verified against a reference encoder and decoder (see QrCodeTest).
 */
class QrCode private constructor(val version: Int, val mask: Int, private val modules: Array<BooleanArray>) {
    val size: Int get() = modules.size

    /** True for a dark module. Row-major: [row][column]. */
    operator fun get(row: Int, col: Int): Boolean = modules[row][col]

    enum class Ecc(val formatBits: Int, val tableIndex: Int) { L(1, 0), M(0, 1), Q(3, 2), H(2, 3) }

    companion object {
        /** Encodes [text] as UTF-8 in the smallest version that fits. [mask] null picks the best mask. */
        fun encode(text: String, ecc: Ecc = Ecc.M, mask: Int? = null, minVersion: Int = 1): QrCode {
            val data = text.encodeToByteArray()
            val version = (minVersion..40).firstOrNull { v -> 4 + countBits(v) + data.size * 8 <= dataCodewords(v, ecc) * 8 }
                ?: throw IllegalArgumentException("Too much data for a QR code (${data.size} bytes)")
            val codewords = addEcc(dataCodewords(data, version, ecc), version, ecc)
            val base = Matrix(version)
            base.drawFunctionPatterns()
            base.drawCodewords(codewords)
            val chosen = mask ?: (0 until 8).minBy { m -> base.copy().apply { applyMask(m); drawFormat(ecc, m) }.penalty() }
            require(chosen in 0..7) { "Mask must be 0..7" }
            val final = base.copy().apply { applyMask(chosen); drawFormat(ecc, chosen); drawVersion() }
            return QrCode(version, chosen, final.dark)
        }

        private fun countBits(version: Int) = if (version <= 9) 8 else 16

        private fun blocks(version: Int, ecc: Ecc): List<Pair<Int, Int>> {
            val t = RS_BLOCKS[version - 1][ecc.tableIndex]
            val out = ArrayList<Pair<Int, Int>>()
            for (i in t.indices step 3) repeat(t[i]) { out += t[i + 1] to t[i + 2] }
            return out
        }

        private fun dataCodewords(version: Int, ecc: Ecc) = blocks(version, ecc).sumOf { it.second }

        private fun dataCodewords(data: ByteArray, version: Int, ecc: Ecc): ByteArray {
            val capacity = dataCodewords(version, ecc)
            val bits = BitBuffer()
            bits.put(0b0100, 4)
            bits.put(data.size, countBits(version))
            for (b in data) bits.put(b.toInt() and 0xFF, 8)
            bits.put(0, minOf(4, capacity * 8 - bits.length))
            while (bits.length % 8 != 0) bits.put(0, 1)
            var pad = 0xEC
            while (bits.length < capacity * 8) { bits.put(pad, 8); pad = if (pad == 0xEC) 0x11 else 0xEC }
            return bits.toBytes()
        }

        private fun addEcc(data: ByteArray, version: Int, ecc: Ecc): ByteArray {
            val blocks = blocks(version, ecc)
            val dataBlocks = ArrayList<ByteArray>()
            val eccBlocks = ArrayList<ByteArray>()
            var offset = 0
            for ((total, dataCount) in blocks) {
                val d = data.copyOfRange(offset, offset + dataCount)
                offset += dataCount
                dataBlocks += d
                eccBlocks += ReedSolomon.remainder(d, total - dataCount)
            }
            val out = ArrayList<Byte>()
            for (i in 0 until dataBlocks.maxOf { it.size }) for (b in dataBlocks) if (i < b.size) out += b[i]
            for (i in 0 until eccBlocks.maxOf { it.size }) for (b in eccBlocks) if (i < b.size) out += b[i]
            return out.toByteArray()
        }

        internal val RS_BLOCKS: Array<Array<IntArray>> = arrayOf(
        arrayOf(intArrayOf(1, 26, 19), intArrayOf(1, 26, 16), intArrayOf(1, 26, 13), intArrayOf(1, 26, 9)),
        arrayOf(intArrayOf(1, 44, 34), intArrayOf(1, 44, 28), intArrayOf(1, 44, 22), intArrayOf(1, 44, 16)),
        arrayOf(intArrayOf(1, 70, 55), intArrayOf(1, 70, 44), intArrayOf(2, 35, 17), intArrayOf(2, 35, 13)),
        arrayOf(intArrayOf(1, 100, 80), intArrayOf(2, 50, 32), intArrayOf(2, 50, 24), intArrayOf(4, 25, 9)),
        arrayOf(intArrayOf(1, 134, 108), intArrayOf(2, 67, 43), intArrayOf(2, 33, 15, 2, 34, 16), intArrayOf(2, 33, 11, 2, 34, 12)),
        arrayOf(intArrayOf(2, 86, 68), intArrayOf(4, 43, 27), intArrayOf(4, 43, 19), intArrayOf(4, 43, 15)),
        arrayOf(intArrayOf(2, 98, 78), intArrayOf(4, 49, 31), intArrayOf(2, 32, 14, 4, 33, 15), intArrayOf(4, 39, 13, 1, 40, 14)),
        arrayOf(intArrayOf(2, 121, 97), intArrayOf(2, 60, 38, 2, 61, 39), intArrayOf(4, 40, 18, 2, 41, 19), intArrayOf(4, 40, 14, 2, 41, 15)),
        arrayOf(intArrayOf(2, 146, 116), intArrayOf(3, 58, 36, 2, 59, 37), intArrayOf(4, 36, 16, 4, 37, 17), intArrayOf(4, 36, 12, 4, 37, 13)),
        arrayOf(intArrayOf(2, 86, 68, 2, 87, 69), intArrayOf(4, 69, 43, 1, 70, 44), intArrayOf(6, 43, 19, 2, 44, 20), intArrayOf(6, 43, 15, 2, 44, 16)),
        arrayOf(intArrayOf(4, 101, 81), intArrayOf(1, 80, 50, 4, 81, 51), intArrayOf(4, 50, 22, 4, 51, 23), intArrayOf(3, 36, 12, 8, 37, 13)),
        arrayOf(intArrayOf(2, 116, 92, 2, 117, 93), intArrayOf(6, 58, 36, 2, 59, 37), intArrayOf(4, 46, 20, 6, 47, 21), intArrayOf(7, 42, 14, 4, 43, 15)),
        arrayOf(intArrayOf(4, 133, 107), intArrayOf(8, 59, 37, 1, 60, 38), intArrayOf(8, 44, 20, 4, 45, 21), intArrayOf(12, 33, 11, 4, 34, 12)),
        arrayOf(intArrayOf(3, 145, 115, 1, 146, 116), intArrayOf(4, 64, 40, 5, 65, 41), intArrayOf(11, 36, 16, 5, 37, 17), intArrayOf(11, 36, 12, 5, 37, 13)),
        arrayOf(intArrayOf(5, 109, 87, 1, 110, 88), intArrayOf(5, 65, 41, 5, 66, 42), intArrayOf(5, 54, 24, 7, 55, 25), intArrayOf(11, 36, 12, 7, 37, 13)),
        arrayOf(intArrayOf(5, 122, 98, 1, 123, 99), intArrayOf(7, 73, 45, 3, 74, 46), intArrayOf(15, 43, 19, 2, 44, 20), intArrayOf(3, 45, 15, 13, 46, 16)),
        arrayOf(intArrayOf(1, 135, 107, 5, 136, 108), intArrayOf(10, 74, 46, 1, 75, 47), intArrayOf(1, 50, 22, 15, 51, 23), intArrayOf(2, 42, 14, 17, 43, 15)),
        arrayOf(intArrayOf(5, 150, 120, 1, 151, 121), intArrayOf(9, 69, 43, 4, 70, 44), intArrayOf(17, 50, 22, 1, 51, 23), intArrayOf(2, 42, 14, 19, 43, 15)),
        arrayOf(intArrayOf(3, 141, 113, 4, 142, 114), intArrayOf(3, 70, 44, 11, 71, 45), intArrayOf(17, 47, 21, 4, 48, 22), intArrayOf(9, 39, 13, 16, 40, 14)),
        arrayOf(intArrayOf(3, 135, 107, 5, 136, 108), intArrayOf(3, 67, 41, 13, 68, 42), intArrayOf(15, 54, 24, 5, 55, 25), intArrayOf(15, 43, 15, 10, 44, 16)),
        arrayOf(intArrayOf(4, 144, 116, 4, 145, 117), intArrayOf(17, 68, 42), intArrayOf(17, 50, 22, 6, 51, 23), intArrayOf(19, 46, 16, 6, 47, 17)),
        arrayOf(intArrayOf(2, 139, 111, 7, 140, 112), intArrayOf(17, 74, 46), intArrayOf(7, 54, 24, 16, 55, 25), intArrayOf(34, 37, 13)),
        arrayOf(intArrayOf(4, 151, 121, 5, 152, 122), intArrayOf(4, 75, 47, 14, 76, 48), intArrayOf(11, 54, 24, 14, 55, 25), intArrayOf(16, 45, 15, 14, 46, 16)),
        arrayOf(intArrayOf(6, 147, 117, 4, 148, 118), intArrayOf(6, 73, 45, 14, 74, 46), intArrayOf(11, 54, 24, 16, 55, 25), intArrayOf(30, 46, 16, 2, 47, 17)),
        arrayOf(intArrayOf(8, 132, 106, 4, 133, 107), intArrayOf(8, 75, 47, 13, 76, 48), intArrayOf(7, 54, 24, 22, 55, 25), intArrayOf(22, 45, 15, 13, 46, 16)),
        arrayOf(intArrayOf(10, 142, 114, 2, 143, 115), intArrayOf(19, 74, 46, 4, 75, 47), intArrayOf(28, 50, 22, 6, 51, 23), intArrayOf(33, 46, 16, 4, 47, 17)),
        arrayOf(intArrayOf(8, 152, 122, 4, 153, 123), intArrayOf(22, 73, 45, 3, 74, 46), intArrayOf(8, 53, 23, 26, 54, 24), intArrayOf(12, 45, 15, 28, 46, 16)),
        arrayOf(intArrayOf(3, 147, 117, 10, 148, 118), intArrayOf(3, 73, 45, 23, 74, 46), intArrayOf(4, 54, 24, 31, 55, 25), intArrayOf(11, 45, 15, 31, 46, 16)),
        arrayOf(intArrayOf(7, 146, 116, 7, 147, 117), intArrayOf(21, 73, 45, 7, 74, 46), intArrayOf(1, 53, 23, 37, 54, 24), intArrayOf(19, 45, 15, 26, 46, 16)),
        arrayOf(intArrayOf(5, 145, 115, 10, 146, 116), intArrayOf(19, 75, 47, 10, 76, 48), intArrayOf(15, 54, 24, 25, 55, 25), intArrayOf(23, 45, 15, 25, 46, 16)),
        arrayOf(intArrayOf(13, 145, 115, 3, 146, 116), intArrayOf(2, 74, 46, 29, 75, 47), intArrayOf(42, 54, 24, 1, 55, 25), intArrayOf(23, 45, 15, 28, 46, 16)),
        arrayOf(intArrayOf(17, 145, 115), intArrayOf(10, 74, 46, 23, 75, 47), intArrayOf(10, 54, 24, 35, 55, 25), intArrayOf(19, 45, 15, 35, 46, 16)),
        arrayOf(intArrayOf(17, 145, 115, 1, 146, 116), intArrayOf(14, 74, 46, 21, 75, 47), intArrayOf(29, 54, 24, 19, 55, 25), intArrayOf(11, 45, 15, 46, 46, 16)),
        arrayOf(intArrayOf(13, 145, 115, 6, 146, 116), intArrayOf(14, 74, 46, 23, 75, 47), intArrayOf(44, 54, 24, 7, 55, 25), intArrayOf(59, 46, 16, 1, 47, 17)),
        arrayOf(intArrayOf(12, 151, 121, 7, 152, 122), intArrayOf(12, 75, 47, 26, 76, 48), intArrayOf(39, 54, 24, 14, 55, 25), intArrayOf(22, 45, 15, 41, 46, 16)),
        arrayOf(intArrayOf(6, 151, 121, 14, 152, 122), intArrayOf(6, 75, 47, 34, 76, 48), intArrayOf(46, 54, 24, 10, 55, 25), intArrayOf(2, 45, 15, 64, 46, 16)),
        arrayOf(intArrayOf(17, 152, 122, 4, 153, 123), intArrayOf(29, 74, 46, 14, 75, 47), intArrayOf(49, 54, 24, 10, 55, 25), intArrayOf(24, 45, 15, 46, 46, 16)),
        arrayOf(intArrayOf(4, 152, 122, 18, 153, 123), intArrayOf(13, 74, 46, 32, 75, 47), intArrayOf(48, 54, 24, 14, 55, 25), intArrayOf(42, 45, 15, 32, 46, 16)),
        arrayOf(intArrayOf(20, 147, 117, 4, 148, 118), intArrayOf(40, 75, 47, 7, 76, 48), intArrayOf(43, 54, 24, 22, 55, 25), intArrayOf(10, 45, 15, 67, 46, 16)),
        arrayOf(intArrayOf(19, 148, 118, 6, 149, 119), intArrayOf(18, 75, 47, 31, 76, 48), intArrayOf(34, 54, 24, 34, 55, 25), intArrayOf(20, 45, 15, 61, 46, 16)),
        )

        internal val ALIGNMENT: Array<IntArray> = arrayOf(
        intArrayOf(),
        intArrayOf(6, 18),
        intArrayOf(6, 22),
        intArrayOf(6, 26),
        intArrayOf(6, 30),
        intArrayOf(6, 34),
        intArrayOf(6, 22, 38),
        intArrayOf(6, 24, 42),
        intArrayOf(6, 26, 46),
        intArrayOf(6, 28, 50),
        intArrayOf(6, 30, 54),
        intArrayOf(6, 32, 58),
        intArrayOf(6, 34, 62),
        intArrayOf(6, 26, 46, 66),
        intArrayOf(6, 26, 48, 70),
        intArrayOf(6, 26, 50, 74),
        intArrayOf(6, 30, 54, 78),
        intArrayOf(6, 30, 56, 82),
        intArrayOf(6, 30, 58, 86),
        intArrayOf(6, 34, 62, 90),
        intArrayOf(6, 28, 50, 72, 94),
        intArrayOf(6, 26, 50, 74, 98),
        intArrayOf(6, 30, 54, 78, 102),
        intArrayOf(6, 28, 54, 80, 106),
        intArrayOf(6, 32, 58, 84, 110),
        intArrayOf(6, 30, 58, 86, 114),
        intArrayOf(6, 34, 62, 90, 118),
        intArrayOf(6, 26, 50, 74, 98, 122),
        intArrayOf(6, 30, 54, 78, 102, 126),
        intArrayOf(6, 26, 52, 78, 104, 130),
        intArrayOf(6, 30, 56, 82, 108, 134),
        intArrayOf(6, 34, 60, 86, 112, 138),
        intArrayOf(6, 30, 58, 86, 114, 142),
        intArrayOf(6, 34, 62, 90, 118, 146),
        intArrayOf(6, 30, 54, 78, 102, 126, 150),
        intArrayOf(6, 24, 50, 76, 102, 128, 154),
        intArrayOf(6, 28, 54, 80, 106, 132, 158),
        intArrayOf(6, 32, 58, 84, 110, 136, 162),
        intArrayOf(6, 26, 54, 82, 110, 138, 166),
        intArrayOf(6, 30, 58, 86, 114, 142, 170),
        )
    }

    private class BitBuffer {
        private val bits = ArrayList<Boolean>()
        val length get() = bits.size
        fun put(value: Int, count: Int) { for (i in count - 1 downTo 0) bits += (value ushr i) and 1 == 1 }
        fun toBytes(): ByteArray = ByteArray(bits.size / 8) { i ->
            var v = 0
            for (j in 0 until 8) v = (v shl 1) or (if (bits[i * 8 + j]) 1 else 0)
            v.toByte()
        }
    }

    private object ReedSolomon {
        private val exp = IntArray(512)
        private val log = IntArray(256)

        init {
            var x = 1
            for (i in 0 until 255) {
                exp[i] = x; log[x] = i
                x = x shl 1
                if (x and 0x100 != 0) x = x xor 0x11d
            }
            for (i in 255 until 512) exp[i] = exp[i - 255]
        }

        private fun mul(a: Int, b: Int) = if (a == 0 || b == 0) 0 else exp[log[a] + log[b]]

        private fun generator(degree: Int): IntArray {
            var g = intArrayOf(1)
            for (i in 0 until degree) {
                val next = IntArray(g.size + 1)
                for (j in g.indices) {
                    next[j] = next[j] xor g[j]
                    next[j + 1] = next[j + 1] xor mul(g[j], exp[i])
                }
                g = next
            }
            return g
        }

        fun remainder(data: ByteArray, degree: Int): ByteArray {
            val gen = generator(degree)
            val r = IntArray(degree)
            for (b in data) {
                val factor = (b.toInt() and 0xFF) xor r[0]
                for (i in 0 until degree - 1) r[i] = r[i + 1] xor mul(gen[i + 1], factor)
                r[degree - 1] = mul(gen[degree], factor)
            }
            return ByteArray(degree) { r[it].toByte() }
        }
    }

    private class Matrix(val version: Int, val dark: Array<BooleanArray>, val reserved: Array<BooleanArray>) {
        constructor(version: Int) : this(version, Array(17 + 4 * version) { BooleanArray(17 + 4 * version) }, Array(17 + 4 * version) { BooleanArray(17 + 4 * version) })

        val n = dark.size

        fun copy() = Matrix(version, Array(n) { dark[it].copyOf() }, reserved)

        private fun set(r: Int, c: Int, v: Boolean) { dark[r][c] = v; reserved[r][c] = true }

        fun drawFunctionPatterns() {
            for (i in 0 until n) { set(6, i, i % 2 == 0); set(i, 6, i % 2 == 0) }
            finder(3, 3); finder(3, n - 4); finder(n - 4, 3)
            val pos = ALIGNMENT[version - 1]
            for (r in pos) for (c in pos) {
                val nearFinder = (r == 6 && c == 6) || (r == 6 && c == n - 7) || (r == n - 7 && c == 6)
                if (!nearFinder) alignment(r, c)
            }
            // Reserve format areas (written later) and the dark module.
            for (i in 0..8) { reserve(8, i); reserve(i, 8) }
            for (i in 0 until 8) { reserve(8, n - 1 - i); reserve(n - 1 - i, 8) }
            set(n - 8, 8, true)
            if (version >= 7) for (i in 0 until 6) for (j in 0 until 3) { reserve(i, n - 11 + j); reserve(n - 11 + j, i) }
        }

        private fun reserve(r: Int, c: Int) { reserved[r][c] = true }

        private fun finder(cr: Int, cc: Int) {
            for (dr in -4..4) for (dc in -4..4) {
                val r = cr + dr; val c = cc + dc
                if (r !in 0 until n || c !in 0 until n) continue
                val d = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc))
                set(r, c, d != 2 && d != 4)
            }
        }

        private fun alignment(cr: Int, cc: Int) {
            for (dr in -2..2) for (dc in -2..2) set(cr + dr, cc + dc, maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc)) != 1)
        }

        fun drawCodewords(codewords: ByteArray) {
            var bit = 0
            val total = codewords.size * 8
            var col = n - 1
            var upward = true
            while (col > 0) {
                if (col == 6) col--
                for (i in 0 until n) {
                    val r = if (upward) n - 1 - i else i
                    for (c in listOf(col, col - 1)) {
                        if (reserved[r][c]) continue
                        dark[r][c] = bit < total && ((codewords[bit ushr 3].toInt() ushr (7 - (bit and 7))) and 1) == 1
                        bit++
                    }
                }
                upward = !upward
                col -= 2
            }
        }

        fun applyMask(mask: Int) {
            for (r in 0 until n) for (c in 0 until n) {
                if (reserved[r][c]) continue
                val flip = when (mask) {
                    0 -> (r + c) % 2 == 0
                    1 -> r % 2 == 0
                    2 -> c % 3 == 0
                    3 -> (r + c) % 3 == 0
                    4 -> (r / 2 + c / 3) % 2 == 0
                    5 -> (r * c) % 2 + (r * c) % 3 == 0
                    6 -> ((r * c) % 2 + (r * c) % 3) % 2 == 0
                    else -> ((r + c) % 2 + (r * c) % 3) % 2 == 0
                }
                if (flip) dark[r][c] = !dark[r][c]
            }
        }

        fun drawFormat(ecc: Ecc, mask: Int) {
            val data = (ecc.formatBits shl 3) or mask
            var rem = data
            repeat(10) { rem = (rem shl 1) xor ((rem ushr 9) * 0x537) }
            val bits = ((data shl 10) or rem) xor 0x5412
            fun b(i: Int) = (bits ushr i) and 1 == 1
            for (i in 0..5) dark[i][8] = b(i)
            dark[7][8] = b(6); dark[8][8] = b(7); dark[8][7] = b(8)
            for (i in 9 until 15) dark[8][14 - i] = b(i)
            for (i in 0 until 8) dark[8][n - 1 - i] = b(i)
            for (i in 8 until 15) dark[n - 15 + i][8] = b(i)
            dark[n - 8][8] = true
        }

        fun drawVersion() {
            if (version < 7) return
            var rem = version
            repeat(12) { rem = (rem shl 1) xor ((rem ushr 11) * 0x1f25) }
            val bits = (version shl 12) or rem
            for (i in 0 until 18) {
                val v = (bits ushr i) and 1 == 1
                val a = n - 11 + i % 3
                val b = i / 3
                dark[b][a] = v
                dark[a][b] = v
            }
        }

        /** ISO 18004 §7.8.3 penalty score. */
        fun penalty(): Int {
            var p = 0
            fun line(get: (Int) -> Boolean) {
                var run = 1
                for (i in 1 until n) {
                    if (get(i) == get(i - 1)) run++ else { if (run >= 5) p += run - 2; run = 1 }
                }
                if (run >= 5) p += run - 2
                for (i in 0..n - 11) {
                    val pattern = (0 until 11).map { get(i + it) }
                    if (pattern == P1 || pattern == P2) p += 40
                }
            }
            for (r in 0 until n) line { dark[r][it] }
            for (c in 0 until n) line { dark[it][c] }
            for (r in 0 until n - 1) for (c in 0 until n - 1) {
                val v = dark[r][c]
                if (v == dark[r][c + 1] && v == dark[r + 1][c] && v == dark[r + 1][c + 1]) p += 3
            }
            val darkCount = dark.sumOf { row -> row.count { it } }
            val percent = darkCount * 100 / (n * n)
            p += kotlin.math.abs(percent - 50) / 5 * 10
            return p
        }

        companion object {
            private val P1 = listOf(true, false, true, true, true, false, true, false, false, false, false)
            private val P2 = P1.reversed()
        }
    }
}

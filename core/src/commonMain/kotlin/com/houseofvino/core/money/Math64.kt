package com.houseofvino.core.money

/** Overflow-checked 64-bit arithmetic and exact 128-bit multiply-divide for common code. */
internal object Math64 {

    fun addExact(a: Long, b: Long): Long {
        val r = a + b
        if (((a xor r) and (b xor r)) < 0) throw ArithmeticException("long overflow")
        return r
    }

    fun subtractExact(a: Long, b: Long): Long {
        val r = a - b
        if (((a xor b) and (a xor r)) < 0) throw ArithmeticException("long overflow")
        return r
    }

    fun negateExact(a: Long): Long {
        if (a == Long.MIN_VALUE) throw ArithmeticException("long overflow")
        return -a
    }

    fun multiplyExact(a: Long, b: Long): Long {
        if (a == 0L || b == 0L) return 0L
        val r = a * b
        if (r / b != a || (a == -1L && b == Long.MIN_VALUE) || (b == -1L && a == Long.MIN_VALUE)) {
            throw ArithmeticException("long overflow")
        }
        return r
    }

    /**
     * Computes round_half_even(a * b / d) exactly, using a 128-bit intermediate product.
     * Throws [ArithmeticException] if the result does not fit in a Long.
     */
    fun mulDivHalfEven(a: Long, b: Long, d: Long): Long {
        require(d > 0) { "divisor must be positive" }
        if (a == 0L || b == 0L) return 0L
        val negative = (a < 0) xor (b < 0)
        val ua = absUnsigned(a)
        val ub = absUnsigned(b)
        val (hi, lo) = multiplyUnsigned(ua, ub)
        val (q, r) = divideUnsigned(hi, lo, d.toULong())
        var quotient = q
        // Half-even: compare 2r with d.
        val twiceR = r * 2u
        val rOverflow = r > (ULong.MAX_VALUE / 2u)
        val cmp = if (rOverflow) 1 else twiceR.compareTo(d.toULong())
        if (cmp > 0 || (cmp == 0 && (quotient and 1uL) == 1uL)) quotient += 1u
        if (quotient > Long.MAX_VALUE.toULong()) throw ArithmeticException("long overflow")
        val result = quotient.toLong()
        return if (negative) -result else result
    }

    private fun absUnsigned(x: Long): ULong = if (x < 0) (0uL - x.toULong()) else x.toULong()

    /** Full 64x64 → 128-bit unsigned product as (high, low). */
    private fun multiplyUnsigned(a: ULong, b: ULong): Pair<ULong, ULong> {
        val mask = 0xFFFF_FFFFuL
        val a0 = a and mask
        val a1 = a shr 32
        val b0 = b and mask
        val b1 = b shr 32
        val p00 = a0 * b0
        val p01 = a0 * b1
        val p10 = a1 * b0
        val p11 = a1 * b1
        val middle = (p00 shr 32) + (p01 and mask) + (p10 and mask)
        val lo = (p00 and mask) or (middle shl 32)
        val hi = p11 + (p01 shr 32) + (p10 shr 32) + (middle shr 32)
        return hi to lo
    }

    /** Divides the 128-bit value (hi, lo) by d. Returns (quotient, remainder); quotient must fit in 64 bits. */
    private fun divideUnsigned(hi: ULong, lo: ULong, d: ULong): Pair<ULong, ULong> {
        if (hi >= d) throw ArithmeticException("long overflow")
        var remainder = hi
        var quotient = 0uL
        for (i in 63 downTo 0) {
            val carry = (remainder shr 63) == 1uL
            remainder = (remainder shl 1) or ((lo shr i) and 1uL)
            quotient = quotient shl 1
            if (carry || remainder >= d) {
                remainder -= d
                quotient = quotient or 1uL
            }
        }
        return quotient to remainder
    }
}

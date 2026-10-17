package com.houseofvino.core.crypto

/**
 * ChaCha20 (RFC 8439) and HChaCha20 (draft-irtf-cfrg-xchacha). Pure Kotlin so the same code
 * runs on Android, iOS and the JVM; verified against the published test vectors.
 */
internal object ChaCha20 {
    private val SIGMA = intArrayOf(0x61707865, 0x3320646e, 0x79622d32, 0x6b206574)

    private fun rotl(v: Int, c: Int) = (v shl c) or (v ushr (32 - c))

    private fun quarter(x: IntArray, a: Int, b: Int, c: Int, d: Int) {
        x[a] += x[b]; x[d] = rotl(x[d] xor x[a], 16)
        x[c] += x[d]; x[b] = rotl(x[b] xor x[c], 12)
        x[a] += x[b]; x[d] = rotl(x[d] xor x[a], 8)
        x[c] += x[d]; x[b] = rotl(x[b] xor x[c], 7)
    }

    private fun rounds(x: IntArray) {
        repeat(10) {
            quarter(x, 0, 4, 8, 12); quarter(x, 1, 5, 9, 13); quarter(x, 2, 6, 10, 14); quarter(x, 3, 7, 11, 15)
            quarter(x, 0, 5, 10, 15); quarter(x, 1, 6, 11, 12); quarter(x, 2, 7, 8, 13); quarter(x, 3, 4, 9, 14)
        }
    }

    private fun initialState(key: ByteArray, counter: Int, nonce: ByteArray): IntArray {
        require(key.size == 32) { "ChaCha20 key must be 32 bytes" }
        require(nonce.size == 12) { "ChaCha20 nonce must be 12 bytes" }
        val s = IntArray(16)
        SIGMA.copyInto(s)
        for (i in 0 until 8) s[4 + i] = key.leInt(i * 4)
        s[12] = counter
        for (i in 0 until 3) s[13 + i] = nonce.leInt(i * 4)
        return s
    }

    /** One 64-byte keystream block. */
    fun block(key: ByteArray, counter: Int, nonce: ByteArray): ByteArray {
        val init = initialState(key, counter, nonce)
        val x = init.copyOf()
        rounds(x)
        val out = ByteArray(64)
        for (i in 0 until 16) out.putLeInt(i * 4, x[i] + init[i])
        return out
    }

    /** XORs [input] with the keystream starting at block [counter]. Encrypts and decrypts. */
    fun xor(key: ByteArray, counter: Int, nonce: ByteArray, input: ByteArray): ByteArray {
        val out = ByteArray(input.size)
        var offset = 0
        var ctr = counter
        while (offset < input.size) {
            val ks = block(key, ctr++, nonce)
            val n = minOf(64, input.size - offset)
            for (i in 0 until n) out[offset + i] = (input[offset + i].toInt() xor ks[i].toInt()).toByte()
            offset += n
        }
        return out
    }

    /** HChaCha20: derives a 32-byte subkey from a key and a 16-byte nonce. */
    fun hchacha20(key: ByteArray, nonce16: ByteArray): ByteArray {
        require(key.size == 32 && nonce16.size == 16)
        val x = IntArray(16)
        SIGMA.copyInto(x)
        for (i in 0 until 8) x[4 + i] = key.leInt(i * 4)
        for (i in 0 until 4) x[12 + i] = nonce16.leInt(i * 4)
        rounds(x)
        val out = ByteArray(32)
        for (i in 0 until 4) out.putLeInt(i * 4, x[i])
        for (i in 0 until 4) out.putLeInt(16 + i * 4, x[12 + i])
        return out
    }
}

internal fun ByteArray.leInt(at: Int): Int =
    (this[at].toInt() and 0xFF) or ((this[at + 1].toInt() and 0xFF) shl 8) or
        ((this[at + 2].toInt() and 0xFF) shl 16) or ((this[at + 3].toInt() and 0xFF) shl 24)

internal fun ByteArray.putLeInt(at: Int, v: Int) {
    this[at] = v.toByte(); this[at + 1] = (v ushr 8).toByte(); this[at + 2] = (v ushr 16).toByte(); this[at + 3] = (v ushr 24).toByte()
}

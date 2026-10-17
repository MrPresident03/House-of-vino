package com.houseofvino.core.crypto

/** Thrown when a ciphertext fails authentication (wrong key, corrupted or tampered data). */
class AuthenticationException : Exception("The data couldn't be verified. It was changed, damaged or made with a different key.")

/** ChaCha20-Poly1305 AEAD (RFC 8439 §2.8). */
object ChaCha20Poly1305 {
    const val KEY_BYTES = 32
    const val NONCE_BYTES = 12
    const val TAG_BYTES = 16

    fun seal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray {
        val polyKey = ChaCha20.block(key, 0, nonce).copyOf(32)
        val ct = ChaCha20.xor(key, 1, nonce, plaintext)
        return ct + Poly1305.mac(macData(aad, ct), polyKey)
    }

    fun open(key: ByteArray, nonce: ByteArray, sealed: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray {
        if (sealed.size < TAG_BYTES) throw AuthenticationException()
        val ct = sealed.copyOf(sealed.size - TAG_BYTES)
        val tag = sealed.copyOfRange(sealed.size - TAG_BYTES, sealed.size)
        val polyKey = ChaCha20.block(key, 0, nonce).copyOf(32)
        if (!constantTimeEquals(Poly1305.mac(macData(aad, ct), polyKey), tag)) throw AuthenticationException()
        return ChaCha20.xor(key, 1, nonce, ct)
    }

    private fun macData(aad: ByteArray, ct: ByteArray): ByteArray {
        fun pad(n: Int) = (16 - n % 16) % 16
        val out = ByteArray(aad.size + pad(aad.size) + ct.size + pad(ct.size) + 16)
        aad.copyInto(out, 0)
        val ctAt = aad.size + pad(aad.size)
        ct.copyInto(out, ctAt)
        val lenAt = ctAt + ct.size + pad(ct.size)
        putLeLong(out, lenAt, aad.size.toLong())
        putLeLong(out, lenAt + 8, ct.size.toLong())
        return out
    }

    private fun putLeLong(b: ByteArray, at: Int, v: Long) {
        for (i in 0 until 8) b[at + i] = (v ushr (8 * i)).toByte()
    }
}

/**
 * XChaCha20-Poly1305 (draft-irtf-cfrg-xchacha-03): a 24-byte nonce, safe to choose at random
 * for every message under one long-lived key.
 */
object XChaCha20Poly1305 {
    const val KEY_BYTES = 32
    const val NONCE_BYTES = 24
    const val TAG_BYTES = 16

    fun seal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray {
        val (subkey, n12) = derive(key, nonce)
        return ChaCha20Poly1305.seal(subkey, n12, plaintext, aad)
    }

    fun open(key: ByteArray, nonce: ByteArray, sealed: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray {
        val (subkey, n12) = derive(key, nonce)
        return ChaCha20Poly1305.open(subkey, n12, sealed, aad)
    }

    private fun derive(key: ByteArray, nonce: ByteArray): Pair<ByteArray, ByteArray> {
        require(key.size == KEY_BYTES) { "Key must be 32 bytes" }
        require(nonce.size == NONCE_BYTES) { "Nonce must be 24 bytes" }
        val subkey = ChaCha20.hchacha20(key, nonce.copyOf(16))
        val n12 = ByteArray(12)
        nonce.copyInto(n12, 4, 16, 24)
        return subkey to n12
    }
}

fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
    if (a.size != b.size) return false
    var diff = 0
    for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
    return diff == 0
}

/** Cryptographically secure random bytes from the platform (SecureRandom / SecRandomCopyBytes). */
expect fun secureRandomBytes(count: Int): ByteArray

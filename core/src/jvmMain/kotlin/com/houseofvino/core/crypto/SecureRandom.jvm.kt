package com.houseofvino.core.crypto

private val rng = java.security.SecureRandom()

actual fun secureRandomBytes(count: Int): ByteArray = ByteArray(count).also { rng.nextBytes(it) }

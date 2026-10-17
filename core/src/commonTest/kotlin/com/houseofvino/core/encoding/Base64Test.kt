package com.houseofvino.core.encoding

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Base64Test {
    @Test fun rfc4648Vectors() {
        val v = listOf("" to "", "f" to "Zg==", "fo" to "Zm8=", "foo" to "Zm9v", "foob" to "Zm9vYg==", "fooba" to "Zm9vYmE=", "foobar" to "Zm9vYmFy")
        for ((plain, enc) in v) {
            assertEquals(enc, Base64.encode(plain.encodeToByteArray()))
            assertEquals(plain, Base64.decode(enc)!!.decodeToString())
        }
    }

    @Test fun urlSafeWithoutPaddingAndWhitespaceTolerant() {
        val bytes = byteArrayOf(-5, -1, -65, 0, 62, 63)
        val url = Base64.encode(bytes, urlSafe = true)
        assertEquals("-_-_AD4_", url)
        assertContentEquals(bytes, Base64.decode(url))
        assertContentEquals(bytes, Base64.decode("+/+/\nAD4/"))
    }

    @Test fun randomRoundTrips() {
        val rnd = Random(1)
        repeat(200) {
            val b = rnd.nextBytes(rnd.nextInt(0, 300))
            assertContentEquals(b, Base64.decode(Base64.encode(b)))
            assertContentEquals(b, Base64.decode(Base64.encode(b, urlSafe = true)))
        }
    }

    @Test fun malformedIsRejected() {
        assertNull(Base64.decode("a"))
        assertNull(Base64.decode("ab*c"))
        assertNull(Base64.decode("Zg==Zg"))
    }
}

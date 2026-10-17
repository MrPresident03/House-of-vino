package com.houseofvino.core.security

import com.houseofvino.core.security.KeyGuard.Kind
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyGuardTest {
    // Header/payload built from the published Supabase local-development demo keys' structure;
    // the signature part is irrelevant to classification.
    private fun jwt(payload: String): String {
        fun enc(s: String) = encode(s.encodeToByteArray())
        return enc("""{"alg":"HS256","typ":"JWT"}""") + "." + enc(payload) + ".c2lnbmF0dXJl"
    }

    private fun encode(bytes: ByteArray): String {
        val a = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val sb = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else -1
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else -1
            sb.append(a[b0 shr 2])
            sb.append(a[((b0 and 3) shl 4) or (if (b1 < 0) 0 else b1 shr 4)])
            if (b1 >= 0) sb.append(a[((b1 and 15) shl 2) or (if (b2 < 0) 0 else b2 shr 6)])
            if (b2 >= 0) sb.append(a[b2 and 63])
            i += 3
        }
        return sb.toString()
    }

    @Test fun legacyAnonJwtIsPublishable() {
        assertEquals(Kind.PUBLISHABLE, KeyGuard.classify(jwt("""{"iss":"supabase-demo","role":"anon","exp":1983812996}""")))
    }

    @Test fun legacyServiceRoleJwtIsPrivilegedEvenThoughTheWordIsEncoded() {
        val key = jwt("""{"iss":"supabase-demo","role": "service_role","exp":1983812996}""")
        assertTrue("service_role" !in key)
        assertEquals(Kind.PRIVILEGED, KeyGuard.classify(key))
        assertTrue(KeyGuard.mustNotEmbed(key))
    }

    @Test fun newStyleKeys() {
        assertEquals(Kind.PUBLISHABLE, KeyGuard.classify("sb_publishable_abc123"))
        assertEquals(Kind.PRIVILEGED, KeyGuard.classify("sb_secret_abc123"))
    }

    @Test fun unknownFormatsAreRefused() {
        assertEquals(Kind.UNKNOWN, KeyGuard.classify(""))
        assertEquals(Kind.UNKNOWN, KeyGuard.classify("not-a-key"))
        assertEquals(Kind.UNKNOWN, KeyGuard.classify(jwt("""{"iss":"x"}""")))
        assertTrue(KeyGuard.mustNotEmbed("not-a-key"))
    }

    @Test fun unexpectedRoleIsPrivileged() {
        assertEquals(Kind.PRIVILEGED, KeyGuard.classify(jwt("""{"role":"supabase_admin"}""")))
    }

    @Test fun base64UrlRoundTrip() {
        for (s in listOf("", "f", "fo", "foo", "foob", "fooba", "foobar", "~~~???")) {
            assertContentEquals(s.encodeToByteArray(), KeyGuard.base64UrlDecode(encode(s.encodeToByteArray())), s)
        }
        assertNull(KeyGuard.base64UrlDecode("a"))
        assertNull(KeyGuard.base64UrlDecode("ab*c"))
    }
}

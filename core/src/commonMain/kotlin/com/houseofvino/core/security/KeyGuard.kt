package com.houseofvino.core.security

/**
 * Classifies Supabase API keys so a privileged key can never be shipped in the app.
 *
 * Supabase issues two key families:
 * - new-style opaque keys: `sb_publishable_…` (safe in clients) and `sb_secret_…` (server only);
 * - legacy JWT keys whose payload carries `"role": "anon"` or `"role": "service_role"`.
 *
 * A legacy JWT is base64url-encoded, so the word "service_role" never appears in the key text
 * itself; the payload has to be decoded to tell the two apart.
 */
object KeyGuard {
    enum class Kind { PUBLISHABLE, PRIVILEGED, UNKNOWN }

    fun classify(key: String): Kind {
        val k = key.trim()
        if (k.isEmpty()) return Kind.UNKNOWN
        if (k.startsWith("sb_secret_")) return Kind.PRIVILEGED
        if (k.startsWith("sb_publishable_")) return Kind.PUBLISHABLE
        val parts = k.split('.')
        if (parts.size == 3) {
            val payload = base64UrlDecode(parts[1])?.decodeToString() ?: return Kind.UNKNOWN
            val role = ROLE.find(payload)?.groupValues?.get(1) ?: return Kind.UNKNOWN
            return when (role) {
                "anon", "authenticated" -> Kind.PUBLISHABLE
                else -> Kind.PRIVILEGED // service_role, supabase_admin, or anything unexpected
            }
        }
        return Kind.UNKNOWN
    }

    /** True for anything that must not be embedded in a client build. Unknown formats are refused too. */
    fun mustNotEmbed(key: String): Boolean = classify(key) != Kind.PUBLISHABLE

    private val ROLE = Regex("\"role\"\\s*:\\s*\"([^\"]+)\"")

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    /** RFC 4648 §5 decoding; accepts standard `+`/`/` too, with or without padding. Null when malformed. */
    fun base64UrlDecode(text: String): ByteArray? {
        val clean = text.trimEnd('=').replace('+', '-').replace('/', '_')
        if (clean.length % 4 == 1) return null
        val out = ArrayList<Byte>(clean.length * 3 / 4)
        var buffer = 0
        var bits = 0
        for (c in clean) {
            val v = ALPHABET.indexOf(c)
            if (v < 0) return null
            buffer = (buffer shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.add(((buffer shr bits) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }
}

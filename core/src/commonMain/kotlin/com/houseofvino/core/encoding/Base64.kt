package com.houseofvino.core.encoding

/** RFC 4648 Base64, standard and URL-safe alphabets. Decoding ignores whitespace and padding. */
object Base64 {
    private const val STD = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private const val URL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    fun encode(bytes: ByteArray, urlSafe: Boolean = false, pad: Boolean = !urlSafe): String {
        val a = if (urlSafe) URL else STD
        val sb = StringBuilder((bytes.size + 2) / 3 * 4)
        var i = 0
        while (i + 3 <= bytes.size) {
            val n = ((bytes[i].toInt() and 0xFF) shl 16) or ((bytes[i + 1].toInt() and 0xFF) shl 8) or (bytes[i + 2].toInt() and 0xFF)
            sb.append(a[n ushr 18]).append(a[(n ushr 12) and 63]).append(a[(n ushr 6) and 63]).append(a[n and 63])
            i += 3
        }
        val rest = bytes.size - i
        if (rest == 1) {
            val n = (bytes[i].toInt() and 0xFF) shl 16
            sb.append(a[n ushr 18]).append(a[(n ushr 12) and 63])
            if (pad) sb.append("==")
        } else if (rest == 2) {
            val n = ((bytes[i].toInt() and 0xFF) shl 16) or ((bytes[i + 1].toInt() and 0xFF) shl 8)
            sb.append(a[n ushr 18]).append(a[(n ushr 12) and 63]).append(a[(n ushr 6) and 63])
            if (pad) sb.append('=')
        }
        return sb.toString()
    }

    /** Decodes either alphabet. Returns null for malformed input. */
    fun decode(text: String): ByteArray? {
        val out = ArrayList<Byte>(text.length * 3 / 4)
        var buffer = 0
        var bits = 0
        var padding = false
        for (ch in text) {
            if (ch.isWhitespace()) continue
            if (ch == '=') { padding = true; continue }
            if (padding) return null // data after padding
            val v = when (ch) {
                in 'A'..'Z' -> ch - 'A'
                in 'a'..'z' -> ch - 'a' + 26
                in '0'..'9' -> ch - '0' + 52
                '+', '-' -> 62
                '/', '_' -> 63
                else -> return null
            }
            buffer = (buffer shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.add(((buffer ushr bits) and 0xFF).toByte())
            }
        }
        if (bits >= 6) return null // a lone trailing character can't encode a byte
        return out.toByteArray()
    }
}

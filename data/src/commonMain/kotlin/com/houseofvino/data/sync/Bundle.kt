package com.houseofvino.data.sync

import com.houseofvino.core.crypto.AuthenticationException
import com.houseofvino.core.crypto.Sha256
import com.houseofvino.core.crypto.XChaCha20Poly1305
import com.houseofvino.core.crypto.secureRandomBytes
import com.houseofvino.core.crypto.toHex
import com.houseofvino.core.encoding.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A user-facing problem with a sync file or invite. [message] is shown as is. */
class SyncFileException(message: String) : Exception(message)

@Serializable
data class BundlePhoto(val id: String, val data: String)

/** The plaintext of a bundle (docs/SYNC.md, "Bundles"). */
@Serializable
data class BundlePayload(
    val format: String = FORMAT,
    val v: Int = 1,
    val household: String,
    val from: String,
    val fromName: String,
    val createdAt: Long,
    val vector: Map<String, Long>,
    val changes: List<WireChange>,
    val photos: List<BundlePhoto> = emptyList(),
) {
    companion object { const val FORMAT = "hov-bundle" }
}

/** A household key and its public id. */
class HouseholdKey(val secret: ByteArray) {
    init { require(secret.size == XChaCha20Poly1305.KEY_BYTES) { "Household keys are 32 bytes" } }

    val id: String = keyId(secret)

    /** Three groups of four digits both partners can compare to confirm they share a key. */
    val safetyCode: String
        get() {
            val d = Sha256.digest("hov-safety".encodeToByteArray() + secret)
            var n = 0L
            for (i in 0 until 6) n = (n shl 8) or (d[i].toLong() and 0xFF)
            val digits = (n % 1_000_000_000_000L).toString().padStart(12, '0')
            return digits.chunked(4).joinToString(" ")
        }

    companion object {
        fun generate() = HouseholdKey(secureRandomBytes(XChaCha20Poly1305.KEY_BYTES))
        fun keyId(secret: ByteArray): String = Sha256.digest("hov-keyid".encodeToByteArray() + secret).copyOf(8).toHex()
    }
}

/** Encrypted envelope and text armour for bundles. */
object BundleCodec {
    private val MAGIC = "HOVS".encodeToByteArray()
    private const val VERSION: Byte = 1
    private const val HEADER = 4 + 1 + 8
    const val BEGIN = "-----BEGIN HOUSE OF VINO CHANGES-----"
    const val END = "-----END HOUSE OF VINO CHANGES-----"
    const val FILE_EXTENSION = "hovsync"
    const val MIME_TYPE = "text/plain"

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun seal(payload: BundlePayload, key: HouseholdKey, nonce: ByteArray = secureRandomBytes(XChaCha20Poly1305.NONCE_BYTES)): ByteArray {
        val header = MAGIC + byteArrayOf(VERSION) + com.houseofvino.core.crypto.hexToBytes(key.id)
        val plaintext = json.encodeToString(BundlePayload.serializer(), payload).encodeToByteArray()
        return header + nonce + XChaCha20Poly1305.seal(key.secret, nonce, plaintext, header)
    }

    /** Key id of an envelope, so the caller can pick the right key. */
    fun keyIdOf(envelope: ByteArray): String {
        if (envelope.size < HEADER + XChaCha20Poly1305.NONCE_BYTES + XChaCha20Poly1305.TAG_BYTES ||
            !envelope.copyOf(4).contentEquals(MAGIC)
        ) throw SyncFileException("This isn't a House of Vino sync file.")
        if (envelope[4] != VERSION) throw SyncFileException("This sync file was made by a newer version of House of Vino. Update the app.")
        return envelope.copyOfRange(5, HEADER).toHex()
    }

    fun open(envelope: ByteArray, key: HouseholdKey): BundlePayload {
        if (keyIdOf(envelope) != key.id) throw SyncFileException("This file was made for a different household, or with an old key.")
        val header = envelope.copyOf(HEADER)
        val nonce = envelope.copyOfRange(HEADER, HEADER + XChaCha20Poly1305.NONCE_BYTES)
        val sealed = envelope.copyOfRange(HEADER + XChaCha20Poly1305.NONCE_BYTES, envelope.size)
        val plaintext = try {
            XChaCha20Poly1305.open(key.secret, nonce, sealed, header)
        } catch (e: AuthenticationException) {
            throw SyncFileException("This sync file is damaged or was changed after it was made.")
        }
        val payload = try {
            json.decodeFromString(BundlePayload.serializer(), plaintext.decodeToString())
        } catch (e: Exception) {
            throw SyncFileException("This sync file couldn't be read.")
        }
        if (payload.format != BundlePayload.FORMAT) throw SyncFileException("This isn't a House of Vino sync file.")
        if (payload.v > 1) throw SyncFileException("This sync file was made by a newer version of House of Vino. Update the app.")
        return payload
    }

    fun armor(envelope: ByteArray): String = buildString {
        append("House of Vino: encrypted cellar changes. Open House of Vino › Sharing › Receive changes.\n")
        append(BEGIN).append('\n')
        Base64.encode(envelope).chunked(76).forEach { append(it).append('\n') }
        append(END).append('\n')
    }

    fun dearmor(text: String): ByteArray {
        val start = text.indexOf(BEGIN)
        val end = text.indexOf(END)
        if (start < 0 || end < start) throw SyncFileException("This isn't a House of Vino sync file.")
        return Base64.decode(text.substring(start + BEGIN.length, end))
            ?: throw SyncFileException("This sync file is damaged.")
    }
}

package com.houseofvino.data.remote

import com.houseofvino.core.security.KeyGuard
import com.houseofvino.data.sync.AuthRequiredException
import com.houseofvino.data.sync.PushOp
import com.houseofvino.data.sync.PushResult
import com.houseofvino.data.sync.SyncRemote
import com.houseofvino.data.sync.TransientSyncException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Backend configuration. Only the project URL and the **anon (publishable) key** ship in the app;
 * they are public by design and every request is authorised by row-level security. The
 * service-role key must never appear here (see docs/ADR.md, ADR-07).
 */
data class BackendConfig(val url: String, val anonKey: String) {
    /**
     * HTTPS, or plain HTTP only to a local development stack (`supabase start`), and a key that
     * is recognisably publishable. Anything else leaves the app in local-only mode.
     */
    val isConfigured: Boolean
        get() = (url.startsWith("https://") || LOCAL_DEV.any { url.startsWith(it) }) &&
            KeyGuard.classify(anonKey) == KeyGuard.Kind.PUBLISHABLE

    init {
        // The build scripts refuse privileged keys too; this is the last line of defence.
        require(KeyGuard.classify(anonKey) != KeyGuard.Kind.PRIVILEGED) { "A privileged (secret/service-role) key must never be embedded in the app" }
    }

    override fun toString(): String = "BackendConfig(url=$url, key=${anonKey.take(12)}…)"

    private companion object {
        val LOCAL_DEV = listOf("http://127.0.0.1:", "http://localhost:", "http://10.0.2.2:")
    }
}

fun createBackend(config: BackendConfig): SupabaseClient = createSupabaseClient(config.url, config.anonKey) {
    install(Auth)
    install(Postgrest)
    install(Storage)
    install(Functions)
}

/** Translates Supabase/Ktor failures into the sync engine's error model. */
internal suspend fun <T> remoteCall(block: suspend () -> T): T = try {
    block()
} catch (e: UnauthorizedRestException) {
    throw AuthRequiredException()
} catch (e: HttpRequestException) {
    throw TransientSyncException("Couldn't reach the server", e)
} catch (e: HttpRequestTimeoutException) {
    throw TransientSyncException("The server took too long to respond", e)
} catch (e: RestException) {
    if (e.statusCode >= 500 || e.statusCode == 429) throw TransientSyncException("Server error ${e.statusCode}", e)
    throw e
}

class SupabaseSyncRemote(private val client: SupabaseClient) : SyncRemote {

    override suspend fun push(collectionId: String, ops: List<PushOp>): List<PushResult> = remoteCall {
        val params = buildJsonObject {
            put("p_collection_id", collectionId)
            put("p_ops", buildJsonArray {
                ops.forEach { op ->
                    add(buildJsonObject {
                        put("op_id", op.opId)
                        put("entity", op.entity)
                        put("id", op.id)
                        put("base_rev", op.baseRev?.let(::JsonPrimitive) ?: kotlinx.serialization.json.JsonNull)
                        put("row", op.row)
                    })
                }
            })
        }
        val response = client.postgrest.rpc("sync_push", params).data
        parsePushResults(Json.parseToJsonElement(response).jsonArray)
    }

    override suspend fun pull(collectionId: String, entity: String, afterRev: Long, limit: Int): List<JsonObject> = remoteCall {
        client.from(entity).select {
            filter {
                eq("collection_id", collectionId)
                gt("rev", afterRev)
            }
            order("rev", Order.ASCENDING)
            limit(limit.toLong())
        }.decodeList<JsonObject>()
    }

    companion object {
        fun parsePushResults(array: JsonArray): List<PushResult> = array.map { el ->
            val o = el.jsonObject
            val opId = o["op_id"]!!.jsonPrimitive.content
            when (o["status"]?.jsonPrimitive?.content) {
                "applied" -> PushResult.Applied(opId, o["rev"]!!.jsonPrimitive.longOrNull ?: 0L)
                "conflict" -> PushResult.Conflict(opId, (o["server_row"] as? JsonObject))
                else -> PushResult.Rejected(
                    opId, o["code"]?.jsonPrimitive?.contentOrNull ?: "rejected",
                    o["message"]?.jsonPrimitive?.contentOrNull ?: "Rejected by server",
                    permanent = o["permanent"]?.jsonPrimitive?.contentOrNull != "false",
                )
            }
        }
    }
}

data class Membership(val collectionId: String, val name: String, val role: String, val ownerId: String)

data class AccountUser(val id: String, val email: String?)

/** Sign-in, households and account deletion. */
class AccountService(private val client: SupabaseClient) {

    val user: Flow<AccountUser?> = client.auth.sessionStatus.map { s ->
        (s as? SessionStatus.Authenticated)?.session?.user?.let { AccountUser(it.id, it.email) }
    }

    fun currentUser(): AccountUser? = client.auth.currentUserOrNull()?.let { AccountUser(it.id, it.email) }

    suspend fun signUp(email: String, password: String) = remoteCall {
        client.auth.signUpWith(Email) { this.email = email.trim(); this.password = password }
    }

    suspend fun signIn(email: String, password: String) = remoteCall {
        client.auth.signInWith(Email) { this.email = email.trim(); this.password = password }
    }

    /** Sends a one-time code by email (no password). */
    suspend fun sendCode(email: String) = remoteCall {
        client.auth.signInWith(OTP) { this.email = email.trim(); createUser = true }
    }

    suspend fun verifyCode(email: String, code: String) = remoteCall {
        client.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email.trim(), token = code.trim())
    }

    suspend fun signOut() = remoteCall { client.auth.signOut() }

    /**
     * Ensures the server knows this device's personal collection (same id as local), creating it
     * on first sign-in. Idempotent.
     */
    suspend fun bootstrap(localCollectionId: String, name: String) = remoteCall {
        client.postgrest.rpc("bootstrap_collection", buildJsonObject {
            put("p_collection_id", localCollectionId)
            put("p_name", name)
        })
    }

    suspend fun memberships(): List<Membership> = remoteCall {
        val data = client.postgrest.rpc("my_collections").data
        Json.parseToJsonElement(data).jsonArray.map {
            val o = it.jsonObject
            Membership(
                o["collection_id"]!!.jsonPrimitive.content, o["name"]!!.jsonPrimitive.content,
                o["role"]!!.jsonPrimitive.content, o["owner_id"]!!.jsonPrimitive.content,
            )
        }
    }

    /** Creates an invite; returns the one-time token to share (only its hash is stored). */
    suspend fun invite(collectionId: String, email: String, role: String): String = remoteCall {
        val data = client.postgrest.rpc("create_invite", buildJsonObject {
            put("p_collection_id", collectionId); put("p_email", email.trim().lowercase()); put("p_role", role)
        }).data
        Json.parseToJsonElement(data).jsonPrimitive.content
    }

    suspend fun acceptInvite(token: String): String = remoteCall {
        val data = client.postgrest.rpc("accept_invite", buildJsonObject { put("p_token", token.trim()) }).data
        Json.parseToJsonElement(data).jsonPrimitive.content
    }

    suspend fun removeMember(collectionId: String, userId: String) = remoteCall {
        client.postgrest.rpc("remove_member", buildJsonObject { put("p_collection_id", collectionId); put("p_user_id", userId) })
    }

    /** Irreversible: deletes the account and every collection it owns (server side). */
    suspend fun deleteAccount() = remoteCall {
        client.functions.invoke("delete-account")
        client.auth.signOut()
    }
}

/** Private photo storage: objects live at `{collection_id}/{photo_id}.jpg`. */
class PhotoStorage(private val client: SupabaseClient) {
    suspend fun upload(collectionId: String, photoId: String, bytes: ByteArray): String = remoteCall {
        val path = "$collectionId/$photoId.jpg"
        client.storage.from(BUCKET).upload(path, bytes) { upsert = true }
        path
    }

    suspend fun download(path: String): ByteArray = remoteCall { client.storage.from(BUCKET).downloadAuthenticated(path) }

    companion object { const val BUCKET = "photos" }
}

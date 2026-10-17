package com.houseofvino.data.integrations

import com.houseofvino.core.model.BottleFormats
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.money.Money
import com.houseofvino.core.scan.Confidence
import com.houseofvino.core.scan.Suggested
import com.houseofvino.core.scan.WineSuggestion
import com.houseofvino.core.time.IsoDate
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource

/** A product record from an external database. It is evidence, never ground truth. */
data class ExternalProduct(
    val source: String,
    val code: String?,
    val productName: String?,
    val brand: String?,
    val quantityText: String?,
    val countries: String?,
    val url: String?,
    val imageUrl: String?,
) {
    /** Turns the record into editable suggestions. Brand → producer is a guess, so MEDIUM at best. */
    fun toSuggestion(): WineSuggestion {
        val year = productName?.let { YEAR.find(it)?.value?.toIntOrNull() }?.takeIf { it in VintageYear.MIN_YEAR..VintageYear.MAX_YEAR }
        val name = productName?.let { n -> brand?.let { b -> n.replace(b, "", ignoreCase = true) } ?: n }
            ?.replace(YEAR, "")?.trim(' ', '-', ',')?.takeIf { it.isNotBlank() }
        return WineSuggestion(
            producer = brand?.split(',')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let { Suggested(it, Confidence.MEDIUM, source) },
            name = name?.let { Suggested(it, Confidence.LOW, source) },
            vintage = year?.let { Suggested(VintageYear.Year(it), Confidence.LOW, "$source product name") },
            formatMl = quantityText?.let { BottleFormats.parseVolume(it) }?.let { Suggested(it, Confidence.MEDIUM, source) },
            country = countries?.split(',')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let { Suggested(it, Confidence.LOW, source) },
            barcode = code,
        )
    }

    private companion object { val YEAR = Regex("""\b(19|20)\d\d\b""") }
}

sealed interface LookupResult {
    data class Found(val products: List<ExternalProduct>, val attribution: String) : LookupResult
    data object NotFound : LookupResult
    /** The service couldn't be reached or isn't configured; the UI falls back to manual entry. */
    data class Unavailable(val reason: String) : LookupResult
}

/** Barcode lookup and product discovery. Replaceable (ADR-09). */
interface BarcodeLookupService {
    val name: String
    suspend fun lookup(code: String): LookupResult
    suspend fun search(query: String, limit: Int = 20): LookupResult
}

object UnavailableBarcodeLookup : BarcodeLookupService {
    override val name = "None"
    override suspend fun lookup(code: String) = LookupResult.Unavailable("Barcode lookup isn't available. Enter the details manually.")
    override suspend fun search(query: String, limit: Int) = LookupResult.Unavailable("Online discovery isn't available.")
}

/**
 * Sliding-window request budget. Open Food Facts publishes per-user limits (product reads
 * 15/min, searches 10/min); staying under them keeps the app from being blocked.
 */
class RequestBudget(private val max: Int, private val windowMs: Long, private val nowMs: () -> Long = monotonicMs()) {
    private val mutex = Mutex()
    private val stamps = ArrayDeque<Long>()

    suspend fun tryAcquire(): Boolean = mutex.withLock {
        val t = nowMs()
        while (stamps.isNotEmpty() && t - stamps.first() >= windowMs) stamps.removeFirst()
        if (stamps.size >= max) false else { stamps.addLast(t); true }
    }

    companion object {
        fun monotonicMs(): () -> Long {
            val start = TimeSource.Monotonic.markNow()
            return { start.elapsedNow().inWholeMilliseconds }
        }
    }
}

/**
 * Open Food Facts (https://world.openfoodfacts.org). Open data under the ODbL: results must be
 * attributed, and a database built from them must stay open. Free and rate-limited; requests
 * identify the app with a User-Agent of the form the service asks for:
 * `AppName/Version (contact)`. Set a real contact address before release (README).
 */
class OpenFoodFactsBarcodeLookup(
    private val client: HttpClient,
    private val userAgent: String = DEFAULT_USER_AGENT,
    private val reads: RequestBudget = RequestBudget(max = 12, windowMs = 60_000),
    private val searches: RequestBudget = RequestBudget(max = 8, windowMs = 60_000),
) : BarcodeLookupService {
    override val name = "Open Food Facts"
    private val json = Json { ignoreUnknownKeys = true }

    private fun busy(status: Int): LookupResult? = when (status) {
        429, 503 -> LookupResult.Unavailable(BUSY)
        else -> null
    }

    override suspend fun lookup(code: String): LookupResult {
        val digits = code.filter { it.isDigit() }
        if (digits.length !in 8..14) return LookupResult.NotFound
        if (!reads.tryAcquire()) return LookupResult.Unavailable(LIMITED)
        return try {
            val response = client.get("https://world.openfoodfacts.org/api/v2/product/$digits.json") {
                header("User-Agent", userAgent)
                parameter("fields", "code,product_name,brands,quantity,countries,image_front_url,categories_tags")
            }
            busy(response.status.value)?.let { return it }
            if (!response.status.isSuccess() && response.status.value != 404) {
                return LookupResult.Unavailable("Open Food Facts returned ${response.status.value}")
            }
            val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
            if (body["status"]?.jsonPrimitive?.intOrNull != 1) return LookupResult.NotFound
            val p = body["product"]?.jsonObject ?: return LookupResult.NotFound
            LookupResult.Found(listOf(product(p, digits)), ATTRIBUTION)
        } catch (e: Exception) {
            LookupResult.Unavailable("Couldn't reach Open Food Facts. Check your connection or enter the details manually.")
        }
    }

    override suspend fun search(query: String, limit: Int): LookupResult {
        if (query.trim().length < 3) return LookupResult.NotFound
        if (!searches.tryAcquire()) return LookupResult.Unavailable(LIMITED)
        return try {
            val response = client.get("https://world.openfoodfacts.org/cgi/search.pl") {
                header("User-Agent", userAgent)
                parameter("search_terms", query)
                parameter("tagtype_0", "categories")
                parameter("tag_contains_0", "contains")
                parameter("tag_0", "wines")
                parameter("search_simple", 1)
                parameter("json", 1)
                parameter("page_size", limit)
                parameter("fields", "code,product_name,brands,quantity,countries,image_front_url")
            }
            busy(response.status.value)?.let { return it }
            if (!response.status.isSuccess()) return LookupResult.Unavailable("Open Food Facts returned ${response.status.value}")
            val products = (json.parseToJsonElement(response.bodyAsText()).jsonObject["products"] as? JsonArray).orEmpty()
                .map { product(it.jsonObject, null) }
                .filter { it.productName != null || it.brand != null }
            if (products.isEmpty()) LookupResult.NotFound else LookupResult.Found(products, ATTRIBUTION)
        } catch (e: Exception) {
            LookupResult.Unavailable("Couldn't reach Open Food Facts.")
        }
    }

    private fun product(p: JsonObject, code: String?): ExternalProduct {
        fun s(k: String) = p[k]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        val c = code ?: s("code")
        return ExternalProduct(
            source = name, code = c, productName = s("product_name"), brand = s("brands"), quantityText = s("quantity"),
            countries = s("countries"), url = c?.let { "https://world.openfoodfacts.org/product/$it" }, imageUrl = s("image_front_url"),
        )
    }

    companion object {
        const val ATTRIBUTION = "Product data © Open Food Facts contributors, available under the Open Database Licence (ODbL)."
        const val DEFAULT_USER_AGENT = "HouseOfVino/0.1 (house-of-vino app)"
        const val LIMITED = "Online lookups are limited to a few a minute. Try again shortly, or enter the details manually."
        const val BUSY = "Open Food Facts is busy right now. Try again in a minute, or enter the details manually."

        fun createOrNull(): OpenFoodFactsBarcodeLookup? = runCatching {
            OpenFoodFactsBarcodeLookup(HttpClient { install(HttpTimeout) { requestTimeoutMillis = 10_000 } })
        }.getOrNull()
    }
}

/** Market prices. No licence-clean free source exists, so the shipped implementation is manual-only. */
interface PriceService {
    data class Quote(val amount: Money, val source: String, val asOf: IsoDate, val url: String?)
    val name: String
    suspend fun quotes(producer: String, wine: String?, vintage: VintageYear, formatMl: Int): List<Quote>?
}

object ManualPriceService : PriceService {
    override val name = "Manual entry"
    /** Null means "not supported": the UI asks the user for value, source and date instead. */
    override suspend fun quotes(producer: String, wine: String?, vintage: VintageYear, formatMl: Int): List<PriceService.Quote>? = null
}

/** Wine metadata (critic scores, drinking windows). Manual-only for the same licensing reasons. */
interface WineCatalogService {
    val name: String
    suspend fun details(producer: String, wine: String?, vintage: VintageYear): WineSuggestion?
}

object ManualWineCatalog : WineCatalogService {
    override val name = "Manual entry"
    override suspend fun details(producer: String, wine: String?, vintage: VintageYear): WineSuggestion? = null
}

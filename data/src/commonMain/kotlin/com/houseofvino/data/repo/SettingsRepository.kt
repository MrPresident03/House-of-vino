package com.houseofvino.data.repo

import com.houseofvino.core.model.RatingScale
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.ExchangeRate
import com.houseofvino.core.search.SearchFilters
import com.houseofvino.core.search.SearchQuery
import com.houseofvino.core.search.SearchScope
import com.houseofvino.core.search.SortOrder
import com.houseofvino.core.time.DateOrder
import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.sync.SyncTables
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class ThemePreference { SYSTEM, DARK, LIGHT }

data class Preferences(
    val currency: CurrencyCode,
    val ratingScale: RatingScale,
    val theme: ThemePreference,
    val dateOrder: DateOrder,
    val cellarView: String,
    val windowReminders: Boolean,
    val analyticsOptIn: Boolean,
)

data class SavedSearch(val id: String, val name: String, val query: SearchQuery)

/** Local preferences and per-collection settings that sync (rates, saved searches). */
class SettingsRepository(private val database: LocalDatabase) {
    private val local get() = database.local
    private val json = Json { ignoreUnknownKeys = true }

    fun get(key: String): String? = local.getSetting(key).executeAsOneOrNull()

    fun put(key: String, value: String?) = database.write {
        if (value == null) local.deleteSetting(key) else local.putSetting(key, value)
        touchedLocal("setting")
    }

    fun preferences(): Preferences = Preferences(
        currency = CurrencyCode.ofOrNull(get(KEY_CURRENCY)) ?: CurrencyCode.EUR,
        ratingScale = get(KEY_RATING_SCALE)?.let { runCatching { RatingScale.valueOf(it) }.getOrNull() } ?: RatingScale.HUNDRED,
        theme = get(KEY_THEME)?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() } ?: ThemePreference.SYSTEM,
        dateOrder = get(KEY_DATE_ORDER)?.let { runCatching { DateOrder.valueOf(it) }.getOrNull() } ?: DateOrder.DMY,
        cellarView = get(KEY_CELLAR_VIEW) ?: "list",
        windowReminders = get(KEY_WINDOW_REMINDERS) == "true",
        analyticsOptIn = false, // No analytics SDK is included; kept explicit for the privacy screen.
    )

    fun isOnboarded(): Boolean = get(KEY_ONBOARDED) == "true"

    // Exchange rates (synced, per collection) ---------------------------------------------------

    fun rates(collectionId: String): List<ExchangeRate> =
        database.journal.ratesForCollection(collectionId).executeAsList().mapNotNull { it.toDomain() }

    fun addRate(collectionId: String, rate: ExchangeRate): String = database.write {
        val id = database.newId()
        database.journal.insertRate(id, collectionId, rate.base.code, rate.quote.code, rate.rateE8, rate.asOf.toString(), rate.source.trim(), now, now)
        touched(SyncTables.EXCHANGE_RATE, id)
        id
    }

    fun deleteRate(id: String) = database.write {
        database.journal.softDeleteRate(now, id)
        touched(SyncTables.EXCHANGE_RATE, id)
    }

    // Recent and saved searches -------------------------------------------------------------------

    fun recordRecentQuery(text: String, scope: SearchScope) {
        val t = text.trim()
        if (t.length < 2) return
        database.write {
            local.recordRecentQuery(t, scope.name, now)
            local.trimRecentQueries(scope.name, 20)
            touchedLocal("recent_query")
        }
    }

    fun recentQueries(scope: SearchScope, limit: Long = 8): List<String> =
        local.recentQueries(scope.name, limit).executeAsList().map { it.query_text }

    fun clearRecentQueries() = database.write { local.clearRecentQueries(); touchedLocal("recent_query") }

    fun savedSearches(collectionId: String): List<SavedSearch> = local.savedSearches(collectionId).executeAsList().map {
        SavedSearch(it.id, it.name, SearchQuery(
            text = it.query_text,
            scope = runCatching { SearchScope.valueOf(it.scope) }.getOrDefault(SearchScope.CELLAR),
            filters = decodeFilters(it.filters_json),
            sort = runCatching { SortOrder.valueOf(it.sort) }.getOrDefault(SortOrder.RELEVANCE),
        ))
    }

    fun saveSearch(collectionId: String, name: String, query: SearchQuery): String = database.write {
        require(name.isNotBlank()) { "Give the search a name" }
        val id = database.newId()
        local.insertSavedSearch(id, collectionId, name.trim(), query.scope.name, query.text, encodeFilters(query.filters), query.sort.name, now, now)
        touched(SyncTables.SAVED_SEARCH, id)
        id
    }

    fun deleteSavedSearch(id: String) = database.write {
        local.softDeleteSavedSearch(now, id)
        touched(SyncTables.SAVED_SEARCH, id)
    }

    private fun encodeFilters(f: SearchFilters): String = json.encodeToString(FiltersDto.serializer(), FiltersDto.from(f))
    private fun decodeFilters(s: String): SearchFilters = runCatching { json.decodeFromString(FiltersDto.serializer(), s).toFilters() }.getOrDefault(SearchFilters())

    /** Stable JSON form of [SearchFilters] (enum names, not ordinals). */
    @Serializable
    private data class FiltersDto(
        val colors: List<String> = emptyList(), val styles: List<String> = emptyList(),
        val countries: List<String> = emptyList(), val regions: List<String> = emptyList(),
        val grapes: List<String> = emptyList(), val producers: List<String> = emptyList(),
        val tagIds: List<String> = emptyList(), val locationIds: List<String> = emptyList(),
        val formatsMl: List<Int> = emptyList(), val vintageFrom: Int? = null, val vintageTo: Int? = null,
        val includeNonVintage: Boolean = false, val inStockOnly: Boolean = true, val windows: List<String> = emptyList(),
        val priceMinMinor: Long? = null, val priceMaxMinor: Long? = null, val priceCurrency: String? = null,
        val minRating: Int? = null,
    ) {
        fun toFilters() = SearchFilters(
            colors = colors.mapNotNull { runCatching { com.houseofvino.core.model.WineColor.valueOf(it) }.getOrNull() }.toSet(),
            styles = styles.mapNotNull { runCatching { com.houseofvino.core.model.WineStyle.valueOf(it) }.getOrNull() }.toSet(),
            countries = countries.toSet(), regions = regions.toSet(), grapes = grapes.toSet(), producers = producers.toSet(),
            tagIds = tagIds.toSet(), locationIds = locationIds.toSet(), formatsMl = formatsMl.toSet(),
            vintageFrom = vintageFrom, vintageTo = vintageTo, includeNonVintage = includeNonVintage, inStockOnly = inStockOnly,
            windows = windows.mapNotNull { runCatching { com.houseofvino.core.search.WindowFilter.valueOf(it) }.getOrNull() }.toSet(),
            priceMinMinor = priceMinMinor, priceMaxMinor = priceMaxMinor, priceCurrency = priceCurrency, minRating = minRating,
        )

        companion object {
            fun from(f: SearchFilters) = FiltersDto(
                f.colors.map { it.name }, f.styles.map { it.name }, f.countries.toList(), f.regions.toList(), f.grapes.toList(),
                f.producers.toList(), f.tagIds.toList(), f.locationIds.toList(), f.formatsMl.toList(), f.vintageFrom, f.vintageTo,
                f.includeNonVintage, f.inStockOnly, f.windows.map { it.name }, f.priceMinMinor, f.priceMaxMinor, f.priceCurrency, f.minRating,
            )
        }
    }

    companion object {
        const val KEY_CURRENCY = "currency"
        const val KEY_RATING_SCALE = "rating_scale"
        const val KEY_THEME = "theme"
        const val KEY_DATE_ORDER = "date_order"
        const val KEY_CELLAR_VIEW = "cellar_view"
        const val KEY_WINDOW_REMINDERS = "window_reminders"
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_ACTIVE_COLLECTION = "active_collection"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_DEVICE_NAME = "device_name"
        const val KEY_JOURNAL_READY = "journal_ready"
    }
}

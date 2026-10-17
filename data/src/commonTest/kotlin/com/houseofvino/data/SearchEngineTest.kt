package com.houseofvino.data

import com.houseofvino.core.model.GrapeShare
import com.houseofvino.core.model.LocationKind
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.Money
import com.houseofvino.core.search.SearchFilters
import com.houseofvino.core.search.SearchQuery
import com.houseofvino.core.search.SearchScope
import com.houseofvino.core.search.SortOrder
import com.houseofvino.core.search.WindowFilter
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.repo.LocationInput
import com.houseofvino.data.repo.LotInput
import com.houseofvino.data.repo.TastingInput
import com.houseofvino.data.repo.VintageInput
import com.houseofvino.data.repo.WineInput
import com.houseofvino.data.repo.WineTarget
import com.houseofvino.data.repo.WishlistInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

class SearchEngineTest {
    private val app = TestApp()
    private val c = app.collectionId
    private val year = 2026

    private fun add(
        producer: String, name: String?, vintage: VintageYear, qty: Int = 1, color: WineColor = WineColor.RED,
        region: String? = null, grapes: List<String> = emptyList(), location: String? = null, price: Long? = null,
        from: Int? = null, until: Int? = null, tags: List<String> = emptyList(), style: WineStyle? = null,
    ): String {
        val existing = app.cellar.findWinesByIdentity(c, producer, name).firstOrNull()
        val target = existing?.let { WineTarget.ExistingWine(it.id) }
            ?: WineTarget.NewWine(WineInput(producer, name, color, style, region = region, grapes = grapes.map { GrapeShare(it, null) }))
        return app.cellar.addWine(c, target, VintageInput(vintage, from, until, if (from != null) "test" else null),
            if (qty > 0) LotInput(quantity = qty, locationId = location, unitPrice = price?.let { Money(it, CurrencyCode.EUR) }) else null, tags)
    }

    private fun cellar(text: String = "", filters: SearchFilters = SearchFilters(), sort: SortOrder = SortOrder.RELEVANCE, limit: Int = 50, offset: Int = 0) =
        app.search.searchCellar(c, SearchQuery(text, SearchScope.CELLAR, filters, sort), offset, limit, year, "EUR")

    @Test
    fun matchesAccentInsensitivePrefixesAcrossFields() {
        val m = add("Château Margaux", null, VintageYear.Year(2015), region = "Bordeaux", grapes = listOf("Cabernet Sauvignon"))
        add("Domaine Leflaive", "Puligny-Montrachet", VintageYear.Year(2018), color = WineColor.WHITE, region = "Burgundy", grapes = listOf("Chardonnay"))
        add("Krug", "Grande Cuvée", VintageYear.NonVintage, color = WineColor.WHITE, style = WineStyle.SPARKLING, region = "Champagne")

        assertEquals(listOf(m), cellar("chateau marg").items.map { it.vintageId })
        assertEquals(listOf(m), cellar("CHÂTEAU").items.map { it.vintageId })
        assertEquals(listOf(m), cellar("cabernet").items.map { it.vintageId })
        assertEquals(1, cellar("puligny montrachet").total)
        assertEquals(1, cellar("chardonnay 2018").total)
        assertEquals(1, cellar("cuvee").total)
        assertEquals(1, cellar("nv").total)
        assertEquals(0, cellar("barolo").total)
    }

    @Test
    fun correctsTyposWhenNothingMatches() {
        add("Château Margaux", null, VintageYear.Year(2015))
        add("Giacomo Conterno", "Barolo Monfortino", VintageYear.Year(2013))
        val page = cellar("margeaux")
        assertEquals(1, page.total)
        assertEquals(mapOf("margeaux" to "margaux"), page.corrections)
        val page2 = cellar("monfortnio conterno")
        assertEquals(1, page2.total)
        assertEquals("monfortino", page2.corrections["monfortnio"])
    }

    @Test
    fun structuredFiltersCombine() {
        val rackParent = app.locations.create(c, LocationInput("Cellar", LocationKind.CELLAR))
        val rack = app.locations.create(c, LocationInput("Rack 1", LocationKind.RACK, rackParent))
        val fridge = app.locations.create(c, LocationInput("Fridge", LocationKind.FRIDGE))
        val a = add("Alpha", null, VintageYear.Year(2010), location = rack, price = 5000, from = 2020, until = 2030, tags = listOf("Gift"))
        val b = add("Beta", null, VintageYear.Year(2016), color = WineColor.WHITE, location = fridge, price = 2000, from = 2030, until = 2040)
        val nv = add("Gamma", null, VintageYear.NonVintage, color = WineColor.WHITE, location = fridge, from = 2015, until = 2026)
        add("Delta", null, VintageYear.Year(2019), qty = 0)

        assertEquals(3, cellar().total) // in-stock only by default
        assertEquals(4, cellar(filters = SearchFilters(inStockOnly = false)).total)
        assertEquals(setOf(b, nv), cellar(filters = SearchFilters(colors = setOf(WineColor.WHITE))).items.map { it.vintageId }.toSet())
        assertEquals(listOf(a), cellar(filters = SearchFilters(locationIds = setOf(rackParent))).items.map { it.vintageId }) // subtree
        assertEquals(listOf(a), cellar(filters = SearchFilters(vintageFrom = 2005, vintageTo = 2012)).items.map { it.vintageId })
        assertEquals(setOf(a, nv), cellar(filters = SearchFilters(vintageFrom = 2005, vintageTo = 2012, includeNonVintage = true)).items.map { it.vintageId }.toSet())
        assertEquals(listOf(b), cellar(filters = SearchFilters(windows = setOf(WindowFilter.HOLD))).items.map { it.vintageId })
        assertEquals(listOf(nv), cellar(filters = SearchFilters(windows = setOf(WindowFilter.DRINK_SOON))).items.map { it.vintageId })
        assertEquals(listOf(a), cellar(filters = SearchFilters(priceMinMinor = 3000, priceCurrency = "EUR")).items.map { it.vintageId })
        val gift = app.cellar.allTags(c).single().first.id
        assertEquals(listOf(a), cellar(filters = SearchFilters(tagIds = setOf(gift))).items.map { it.vintageId })
        assertEquals(listOf(a), cellar("vintage:2010").items.map { it.vintageId })
        assertEquals(listOf(nv), cellar("vintage:nv").items.map { it.vintageId })
        assertEquals(setOf(b, nv), cellar("color:white").items.map { it.vintageId }.toSet())
    }

    @Test
    fun sortingAndPagingAreStable() {
        val ids = (1..25).map { i -> add("Producer ${i.toString().padStart(2, '0')}", null, VintageYear.Year(1990 + i), qty = i) }
        val byProducer = (0 until 5).flatMap { p -> cellar(sort = SortOrder.PRODUCER, limit = 5, offset = p * 5).items.map { it.vintageId } }
        assertEquals(ids, byProducer)
        assertEquals(ids.reversed(), cellar(sort = SortOrder.VINTAGE_DESC).items.map { it.vintageId })
        assertEquals(ids.last(), cellar(sort = SortOrder.QUANTITY_DESC).items.first().vintageId)
        assertEquals(25, cellar(limit = 5).total)
    }

    @Test
    fun relevancePrefersIdentityOverNotes() {
        val inNotes = app.cellar.addWine(c, WineTarget.NewWine(WineInput("Some Estate", notes = "Tastes like Margaux")), VintageInput(VintageYear.Year(2010)), LotInput(quantity = 1))
        val inName = add("Château Margaux", null, VintageYear.Year(2010))
        assertEquals(listOf(inName, inNotes), cellar("margaux").items.map { it.vintageId })
    }

    @Test
    fun wishlistAndJournalScopes() {
        val v = add("Ridge", "Monte Bello", VintageYear.Year(2014))
        app.wishlist.add(c, WishlistInput(producer = "Bodegas Vega Sicilia", name = "Único", vintage = VintageYear.Year(2012)))
        app.wishlist.add(c, WishlistInput(vintageId = v))
        app.journal.add(c, v, TastingInput(IsoDate.of(2026, 9, 1), 96, notes = "Cassis, cedar and graphite", occasion = "Anniversary"))
        val wl = app.search.searchWishlist(c, SearchQuery("unico", SearchScope.WISHLIST), setOf("WANTED"), 0, 10)
        assertEquals(1, wl.total)
        assertEquals("Bodegas Vega Sicilia", wl.items.single().producer)
        assertEquals(1, app.search.searchWishlist(c, SearchQuery("monte bello", SearchScope.WISHLIST), setOf("WANTED"), 0, 10).total)
        assertEquals(1, app.search.searchJournal(c, SearchQuery("graphite", SearchScope.JOURNAL), 0, 10).total)
        assertEquals(1, app.search.searchJournal(c, SearchQuery("anniversary ridge", SearchScope.JOURNAL), 0, 10).total)
        assertEquals(0, app.search.searchJournal(c, SearchQuery("unico", SearchScope.JOURNAL), 0, 10).total)
    }

    @Test
    fun indexFollowsEditsAndDeletes() {
        val v = add("Old Name", null, VintageYear.Year(2000), qty = 0)
        val wineId = app.cellar.vintage(v)!!.wineId
        app.cellar.updateWine(wineId, WineInput("New Name"))
        val all = SearchFilters(inStockOnly = false)
        assertEquals(0, cellar("old", all).total)
        assertEquals(1, cellar("new", all).total)
        app.cellar.deleteVintage(v)
        assertEquals(0, cellar("new", all).total)
        app.db.searchIndex.rebuildAll()
        assertEquals(0, cellar("new", all).total)
    }

    @Test
    fun fiveThousandBottleCollectionStaysResponsive() {
        val regions = listOf("Bordeaux", "Burgundy", "Rhône", "Barolo", "Rioja", "Napa", "Mosel", "Champagne")
        val grapes = listOf("Cabernet Sauvignon", "Pinot Noir", "Syrah", "Nebbiolo", "Tempranillo", "Riesling", "Chardonnay")
        val genStart = TimeSource.Monotonic.markNow()
        // 1,250 vintages × 4 bottles, loaded through the real repository code path.
        for (i in 0 until 1250) {
            add("Producer ${i % 400} ${regions[i % regions.size]}", "Cuvée ${i % 7}", VintageYear.Year(1980 + i % 45), qty = 4,
                region = regions[i % regions.size], grapes = listOf(grapes[i % grapes.size]), price = 1000L + i, from = 2000 + i % 40, until = 2030 + i % 30)
        }
        val loadTime = genStart.elapsedNow()
        assertEquals(5000, app.insights.compute(c, year, null, null, IsoDate.of(2026, 10, 17)).totals.bottles)

        val queries = listOf("bordeaux", "pinot", "producer 12", "cuvee 3 1999", "rioja tempranillo", "burgundy pinot noir 2010", "champagne")
        val timings = queries.map { q ->
            val mark = TimeSource.Monotonic.markNow()
            val page = cellar(q, limit = 50)
            assertTrue(page.total > 0, "\"$q\" finds something")
            mark.elapsedNow().inWholeMilliseconds
        }
        val sorted = listOf(SortOrder.PRODUCER, SortOrder.VINTAGE_DESC, SortOrder.PRICE_DESC, SortOrder.DRINK_BY).map { s ->
            val mark = TimeSource.Monotonic.markNow()
            cellar(sort = s, limit = 50, offset = 1000)
            mark.elapsedNow().inWholeMilliseconds
        }
        val filtered = TimeSource.Monotonic.markNow().let { mark ->
            cellar("pinot", SearchFilters(windows = setOf(WindowFilter.READY), vintageFrom = 1990, vintageTo = 2015), limit = 50)
            mark.elapsedNow().inWholeMilliseconds
        }
        println("PERF load=${loadTime.inWholeMilliseconds}ms text=$timings sorted=$sorted filtered=${filtered}ms")
        // Generous ceilings for CI machines; typical results are far lower.
        assertTrue(timings.max() < 1500, "text search p100 ${timings.max()} ms")
        assertTrue(sorted.max() < 1500, "sorted paging p100 ${sorted.max()} ms")
        assertTrue(filtered < 1500, "filtered search $filtered ms")
    }
}

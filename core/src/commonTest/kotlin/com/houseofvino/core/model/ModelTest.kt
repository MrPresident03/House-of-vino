package com.houseofvino.core.model

import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelTest {

    @Test
    fun vintageParsingDistinguishesNvFromUnknown() {
        assertEquals(VintageYear.Year(2015), VintageYear.parse("2015"))
        assertEquals(VintageYear.NonVintage, VintageYear.parse("NV"))
        assertEquals(VintageYear.NonVintage, VintageYear.parse("N.V."))
        assertEquals(VintageYear.NonVintage, VintageYear.parse("Non-Vintage"))
        assertEquals(VintageYear.Unknown, VintageYear.parse(""))
        assertEquals(VintageYear.Unknown, VintageYear.parse(null))
        assertEquals(VintageYear.Unknown, VintageYear.parse("unknown"))
        assertNull(VintageYear.parse("15"))
        assertNull(VintageYear.parse("abcd"))
        assertFailsWith<IllegalArgumentException> { VintageYear.Year(1200) }
        assertEquals(VintageYear.NonVintage, VintageYear.fromStorage("NV", null))
        assertEquals(VintageYear.Year(1990), VintageYear.fromStorage("YEAR", 1990))
    }

    @Test
    fun vintageOrdering() {
        val sorted = listOf(VintageYear.Unknown, VintageYear.Year(2010), VintageYear.NonVintage, VintageYear.Year(1998)).sorted()
        assertEquals(listOf(VintageYear.Year(1998), VintageYear.Year(2010), VintageYear.NonVintage, VintageYear.Unknown), sorted)
    }

    @Test
    fun bottleFormats() {
        assertEquals("Magnum", BottleFormats.nameFor(1500))
        assertEquals("1.8 L", BottleFormats.nameFor(1800))
        assertEquals("70 cl", BottleFormats.nameFor(700))
        assertEquals("Piccolo", BottleFormats.nameFor(187))
        assertEquals("355 ml", BottleFormats.formatVolume(355))
        assertEquals(750, BottleFormats.parseVolume("Contents 750 ml e"))
        assertEquals(750, BottleFormats.parseVolume("75cl"))
        assertEquals(1500, BottleFormats.parseVolume("1,5 L"))
        assertNull(BottleFormats.parseVolume("13.5% vol"))
        assertEquals(200L, BottleFormats.standardEquivalentsCentis(1500, 1))
        assertEquals(50L, BottleFormats.standardEquivalentsCentis(375, 1))
    }

    @Test
    fun ratingsDisplayInEveryScale() {
        assertEquals("94", Ratings.display(94, RatingScale.HUNDRED))
        assertEquals("17.4/20", Ratings.display(87, RatingScale.TWENTY))
        assertEquals("18/20", Ratings.display(90, RatingScale.TWENTY))
        assertEquals("4.5★", Ratings.display(87, RatingScale.FIVE_STARS))
        assertEquals("5★", Ratings.display(100, RatingScale.FIVE_STARS))
        assertEquals(90, Ratings.fromScaleTenths(45, RatingScale.FIVE_STARS))
        assertEquals(88, Ratings.fromScaleTenths(176, RatingScale.TWENTY))
        assertEquals(93, Ratings.fromScaleTenths(930, RatingScale.HUNDRED))
        assertFailsWith<IllegalArgumentException> { Ratings.validate(101) }
    }

    @Test
    fun wishlistRules() {
        assertTrue(WishlistStatus.WANTED.canTransitionTo(WishlistStatus.ORDERED))
        assertTrue(WishlistStatus.ORDERED.canTransitionTo(WishlistStatus.ACQUIRED))
        assertFalse(WishlistStatus.ORDERED.canTransitionTo(WishlistStatus.WATCHING))
        assertFalse(WishlistStatus.ACQUIRED.canTransitionTo(WishlistStatus.CANCELLED))
        assertFailsWith<IllegalArgumentException> {
            WishlistItem("i", "c", null, null, " ", null, null, null, 1, null, WishlistPriority.LOW, WishlistStatus.WANTED, null, null, 0, 0)
        }
        val target = Money(5000, CurrencyCode.EUR)
        val link = MerchantLink("m", "i", "Shop", "https://shop.example", Money(4900, CurrencyCode.EUR), null)
        assertTrue(link.meetsTarget(target))
        assertFalse(link.copy(lastPrice = Money(4900, CurrencyCode.USD)).meetsTarget(target))
        assertFailsWith<IllegalArgumentException> { MerchantLink("m", "i", "Shop", "javascript:alert(1)", null, null) }
    }

    @Test
    fun locationTreeCycleDetection() {
        val parents = mapOf("rack" to "cellar", "shelf" to "rack", "cellar" to null)
        assertTrue(LocationTree.wouldCreateCycle("cellar", "shelf") { parents[it] })
        assertTrue(LocationTree.wouldCreateCycle("rack", "rack") { parents[it] })
        assertFalse(LocationTree.wouldCreateCycle("shelf", "cellar") { parents[it] })
        assertFalse(LocationTree.wouldCreateCycle("shelf", null) { parents[it] })
    }

    @Test
    fun invariantsOnEntities() {
        assertFailsWith<IllegalArgumentException> {
            Vintage("v", "c", "w", VintageYear.Year(2010), 2030, 2020, null, null, null, null, 0, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            Location("l", "c", null, "Rack", LocationKind.RACK, 5, null, 0, null, 0, 0)
        }
        assertFailsWith<IllegalArgumentException> { GrapeShare("Merlot", 120) }
        assertFailsWith<IllegalArgumentException> {
            Lot("l", "c", "v", 750, null, null, null, Money(-1, CurrencyCode.EUR), null, null, null, null, 0, 0)
        }
    }
}

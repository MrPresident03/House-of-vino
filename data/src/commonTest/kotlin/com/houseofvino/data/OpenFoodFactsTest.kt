package com.houseofvino.data

import com.houseofvino.core.scan.Confidence
import com.houseofvino.data.integrations.LookupResult
import com.houseofvino.data.integrations.OpenFoodFactsBarcodeLookup
import com.houseofvino.data.integrations.RequestBudget
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenFoodFactsTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private var now = 0L
    private val requests = mutableListOf<String>()
    private val userAgents = mutableListOf<String?>()

    private fun service(status: HttpStatusCode = HttpStatusCode.OK, body: String = "{}", reads: Int = 12, searches: Int = 8): OpenFoodFactsBarcodeLookup {
        val engine = MockEngine { request ->
            requests += request.url.encodedPath
            userAgents += request.headers[HttpHeaders.UserAgent]
            respond(body, status, json)
        }
        return OpenFoodFactsBarcodeLookup(
            HttpClient(engine), userAgent = "HouseOfVino/test (test@example.com)",
            reads = RequestBudget(reads, 60_000) { now }, searches = RequestBudget(searches, 60_000) { now },
        )
    }

    @Test fun foundProductBecomesCandidatesNeverFacts() = runTest {
        val body = """{"status":1,"product":{"code":"3760123450011","product_name":"Château Exemple 2016","brands":"Château Exemple","quantity":"75 cl","countries":"France"}}"""
        val r = service(body = body).lookup("3760123450011")
        assertIs<LookupResult.Found>(r)
        assertTrue(r.attribution.contains("ODbL"))
        assertEquals("/api/v2/product/3760123450011.json", requests.single())
        assertEquals("HouseOfVino/test (test@example.com)", userAgents.single())
        val s = r.products.single().toSuggestion()
        assertEquals("Château Exemple", s.producer?.value)
        assertEquals(Confidence.MEDIUM, s.producer?.confidence)
        // A year inside a product name is weak evidence: the same barcode is reused across vintages.
        assertEquals(Confidence.LOW, s.vintage?.confidence)
        assertEquals(750, s.formatMl?.value)
    }

    @Test fun unknownCodesAndBadInput() = runTest {
        assertEquals(LookupResult.NotFound, service(body = """{"status":0}""").lookup("3760123450011"))
        assertEquals(LookupResult.NotFound, service(status = HttpStatusCode.NotFound).lookup("3760123450011"))
        val before = requests.size
        assertEquals(LookupResult.NotFound, service().lookup("12ab"))
        assertEquals(before, requests.size, "malformed codes never reach the network")
    }

    @Test fun rateLimitedOrBusyServiceFallsBackToManualEntry() = runTest {
        val r = service(status = HttpStatusCode.TooManyRequests).lookup("3760123450011")
        assertIs<LookupResult.Unavailable>(r)
        assertEquals(OpenFoodFactsBarcodeLookup.BUSY, r.reason)
        val down = service(status = HttpStatusCode.ServiceUnavailable).search("bordeaux")
        assertIs<LookupResult.Unavailable>(down)
    }

    @Test fun clientSideBudgetKeepsUnderPublishedLimits() = runTest {
        val off = service(body = """{"products":[{"code":"1","product_name":"Rioja","brands":"Bodega"}]}""", searches = 2)
        assertIs<LookupResult.Found>(off.search("rioja"))
        assertIs<LookupResult.Found>(off.search("rioja reserva"))
        val third = off.search("rioja gran reserva")
        assertIs<LookupResult.Unavailable>(third)
        assertEquals(OpenFoodFactsBarcodeLookup.LIMITED, third.reason)
        assertEquals(2, requests.size, "the limited call never reached the network")
        now += 60_001
        assertIs<LookupResult.Found>(off.search("rioja gran reserva"))
    }

    @Test fun shortQueriesAreNotSent() = runTest {
        assertEquals(LookupResult.NotFound, service().search("ri"))
        assertTrue(requests.isEmpty())
    }

    @Test fun noYearMeansNoVintageSuggestion() {
        val p = com.houseofvino.data.integrations.ExternalProduct("Open Food Facts", "1", "Champagne Brut", "Maison X", "750 ml", null, null, null)
        assertNull(p.toSuggestion().vintage)
    }
}

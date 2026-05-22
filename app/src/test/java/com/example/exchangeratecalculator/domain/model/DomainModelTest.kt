package com.example.exchangeratecalculator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DomainModelTest {
    private fun ticker(fetchedAtEpochMs: Long = System.currentTimeMillis()) =
        RateTicker(
            book = "usdc_mxn",
            ask = BigDecimal("17.50"),
            bid = BigDecimal("17.40"),
            fetchedAtEpochMs = fetchedAtEpochMs,
        )

    @Test
    fun isStale_pastThreshold() {
        val t = ticker(fetchedAtEpochMs = System.currentTimeMillis() - STALE_THRESHOLD_MS - 1_000L)
        assertTrue(t.isStale)
    }

    @Test
    fun isStale_freshFetch() {
        val t = ticker(fetchedAtEpochMs = System.currentTimeMillis())
        assertFalse(t.isStale)
    }

    @Test
    fun appSettings_defaults() {
        val settings = AppSettings()
        assertEquals("MXN", settings.selectedFiatCode)
        assertFalse(settings.isSwapped)
    }

    @Test
    fun rateResource_exhaustiveness() {
        val resources: List<RateResource> =
            listOf(
                RateResource.Loading,
                RateResource.Available(ticker()),
                RateResource.Unavailable("no data"),
            )
        resources.forEach { resource ->
            val label =
                when (resource) {
                    is RateResource.Loading -> "loading"
                    is RateResource.Available -> "available"
                    is RateResource.Unavailable -> "unavailable"
                }
            assertTrue(label.isNotEmpty())
        }
    }

    @Test
    fun usdcCurrency_sentinel() {
        assertEquals("USDC", USDC_CURRENCY.code)
        assertTrue(USDC_CURRENCY.isBase)
    }
}

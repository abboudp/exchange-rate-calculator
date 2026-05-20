package com.example.exchangeratecalculator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DomainModelTest {

    private fun ticker(expiresAtEpochMs: Long) = RateTicker(
        book = "usdc_mxn",
        ask = BigDecimal("17.50"),
        bid = BigDecimal("17.40"),
        fetchedAtEpochMs = System.currentTimeMillis(),
        expiresAtEpochMs = expiresAtEpochMs,
    )

    @Test
    fun isStale_expired() {
        val t = ticker(expiresAtEpochMs = System.currentTimeMillis() - 1)
        assertTrue(t.isStale)
    }

    @Test
    fun isStale_fresh() {
        val t = ticker(expiresAtEpochMs = System.currentTimeMillis() + 3_600_000)
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
        val resources: List<RateResource> = listOf(
            RateResource.Loading,
            RateResource.Fresh(ticker(0)),
            RateResource.Stale(ticker(0)),
            RateResource.Unavailable("no data"),
        )
        resources.forEach { resource ->
            val label = when (resource) {
                is RateResource.Loading -> "loading"
                is RateResource.Fresh -> "fresh"
                is RateResource.Stale -> "stale"
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

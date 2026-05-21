package com.example.exchangeratecalculator.data.local

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class RateTickerEntityTest {
    @Test
    fun toDomain_preservesBigDecimalPrecision() {
        val entity =
            RateTickerEntity(
                book = "usdc_mxn",
                ask = "18.4105000000",
                bid = "18.4069700000",
                fetchedAtEpochMs = 1_000L,
                expiresAtEpochMs = 301_000L,
            )

        val ticker = entity.toDomain()

        assertEquals(BigDecimal("18.4105000000"), ticker.ask)
        assertEquals(BigDecimal("18.4069700000"), ticker.bid)
        assertEquals("usdc_mxn", ticker.book)
        assertEquals(1_000L, ticker.fetchedAtEpochMs)
        assertEquals(301_000L, ticker.expiresAtEpochMs)
    }
}

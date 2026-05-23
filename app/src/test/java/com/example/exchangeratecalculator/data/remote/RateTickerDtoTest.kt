package com.example.exchangeratecalculator.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RateTickerDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parse_happyPath_preservesDecimalStrings() {
        val raw = """{"ask":"18.4105","bid":"18.4069","book":"usdc_mxn","date":"2025-10-20T20:14:57.361483956"}"""

        val dto = json.decodeFromString<RateTickerDto>(raw)

        assertEquals("18.4105", dto.ask)
        assertEquals("18.4069", dto.bid)
        assertEquals("usdc_mxn", dto.book)
    }

    @Test
    fun parse_unknownField_isIgnored() {
        val raw = """{"ask":"1.0","bid":"1.0","book":"usdc_mxn","date":"x","foo":"bar"}"""

        val dto = json.decodeFromString<RateTickerDto>(raw)

        assertEquals("usdc_mxn", dto.book)
    }

    @Test
    fun fallbackCurrencies_queryCodes_joinsWithComma() {
        assertEquals("MXN,ARS,BRL,COP", FallbackCurrenciesProvider.queryCodes)
    }

    private fun dto(ask: String = "18.41", bid: String = "18.40") =
        RateTickerDto(ask = ask, bid = bid, book = "usdc_mxn", date = "2026-05-23")

    @Test
    fun `toEntity returns entity for valid positive decimals`() {
        val entity = dto(ask = "18.41", bid = "18.40").toEntity(fetchedAtEpochMs = 1000L)
        assertNotNull(entity)
    }

    @Test
    fun `toEntity returns null when ask is not a decimal`() {
        val entity = dto(ask = "N/A", bid = "18.40").toEntity(fetchedAtEpochMs = 1000L)
        assertNull(entity)
    }

    @Test
    fun `toEntity returns null when bid is empty`() {
        val entity = dto(ask = "18.41", bid = "").toEntity(fetchedAtEpochMs = 1000L)
        assertNull(entity)
    }

    @Test
    fun `toEntity returns null when ask is zero`() {
        val entity = dto(ask = "0", bid = "18.40").toEntity(fetchedAtEpochMs = 1000L)
        assertNull(entity)
    }

    @Test
    fun `toEntity returns null when bid is negative`() {
        val entity = dto(ask = "18.41", bid = "-1.00").toEntity(fetchedAtEpochMs = 1000L)
        assertNull(entity)
    }

    @Test
    fun `toEntity stores ask and bid as canonical plain string`() {
        val entity = dto(ask = "18.410", bid = "18.400").toEntity(fetchedAtEpochMs = 1000L)!!
        assertEquals("18.410", entity.ask)
        assertEquals("18.400", entity.bid)
    }
}

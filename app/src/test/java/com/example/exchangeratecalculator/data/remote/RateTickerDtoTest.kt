package com.example.exchangeratecalculator.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
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
}

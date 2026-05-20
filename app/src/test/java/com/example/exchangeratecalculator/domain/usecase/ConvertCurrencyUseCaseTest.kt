package com.example.exchangeratecalculator.domain.usecase

import com.example.exchangeratecalculator.domain.model.RateTicker
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ConvertCurrencyUseCaseTest {

    private val useCase = ConvertCurrencyUseCase()

    private fun ticker(
        ask: String = "18.41",
        bid: String = "18.40",
        book: String = "usdc_mxn",
    ) = RateTicker(
        book = book,
        ask = BigDecimal(ask),
        bid = BigDecimal(bid),
        fetchedAtEpochMs = 0L,
        expiresAtEpochMs = Long.MAX_VALUE,
    )

    @Test
    fun usdcToMxn_usesBid() {
        val quote = useCase(
            inputText = "100",
            fromCode = "USDC",
            toCode = "MXN",
            ticker = ticker(ask = "18.41", bid = "18.40"),
        )
        assertEquals(0, BigDecimal("1840.00").compareTo(quote.outputAmount))
        assertEquals(2, quote.outputAmount.scale())
    }

    @Test
    fun mxnToUsdc_usesAsk() {
        val quote = useCase(
            inputText = "1841",
            fromCode = "MXN",
            toCode = "USDC",
            ticker = ticker(ask = "18.41", bid = "18.40"),
        )
        assertEquals(0, BigDecimal("100.00").compareTo(quote.outputAmount))
        assertEquals(2, quote.outputAmount.scale())
    }

    @Test
    fun identityConversion_returnsInputScaled() {
        val quote = useCase(
            inputText = "50",
            fromCode = "USDC",
            toCode = "USDC",
            ticker = ticker(),
        )
        assertEquals(0, BigDecimal("50.00").compareTo(quote.outputAmount))
    }

    @Test
    fun emptyInput_returnsZero() {
        val quote = useCase(
            inputText = "",
            fromCode = "USDC",
            toCode = "MXN",
            ticker = ticker(),
        )
        assertEquals(0, BigDecimal.ZERO.compareTo(quote.outputAmount))
    }

    @Test
    fun unparseableInput_returnsZero() {
        val quote = useCase(
            inputText = "abc",
            fromCode = "USDC",
            toCode = "MXN",
            ticker = ticker(),
        )
        assertEquals(0, BigDecimal.ZERO.compareTo(quote.outputAmount))
    }

    @Test
    fun nullTicker_returnsZero_noCrash() {
        val quote = useCase(
            inputText = "100",
            fromCode = "USDC",
            toCode = "MXN",
            ticker = null,
        )
        assertEquals(0, BigDecimal.ZERO.compareTo(quote.outputAmount))
    }

    @Test
    fun zeroAsk_mxnToUsdc_returnsZero_noCrash() {
        val quote = useCase(
            inputText = "100",
            fromCode = "MXN",
            toCode = "USDC",
            ticker = ticker(ask = "0", bid = "18.40"),
        )
        assertEquals(0, BigDecimal.ZERO.compareTo(quote.outputAmount))
    }

    @Test
    fun zeroBid_usdcToMxn_returnsZero_noCrash() {
        val quote = useCase(
            inputText = "100",
            fromCode = "USDC",
            toCode = "MXN",
            ticker = ticker(ask = "18.41", bid = "0"),
        )
        assertEquals(0, BigDecimal.ZERO.compareTo(quote.outputAmount))
    }

    @Test
    fun bigDecimalPrecision_smallAmountDoesNotUnderflow() {
        val quote = useCase(
            inputText = "0.01",
            fromCode = "MXN",
            toCode = "USDC",
            ticker = ticker(ask = "0.50", bid = "0.49"),
        )
        assertEquals(0, BigDecimal("0.02").compareTo(quote.outputAmount))
    }

    @Test
    fun negativeInput_returnsZero() {
        val quote = useCase(
            inputText = "-5",
            fromCode = "USDC",
            toCode = "MXN",
            ticker = ticker(),
        )
        assertEquals(0, BigDecimal.ZERO.compareTo(quote.outputAmount))
    }
}

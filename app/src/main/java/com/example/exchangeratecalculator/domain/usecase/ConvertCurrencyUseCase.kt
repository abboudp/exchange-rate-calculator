package com.example.exchangeratecalculator.domain.usecase

import com.example.exchangeratecalculator.core.precision.scaleForDisplay
import com.example.exchangeratecalculator.core.precision.toBigDecimalOrZero
import com.example.exchangeratecalculator.domain.model.ConversionQuote
import com.example.exchangeratecalculator.domain.model.RateTicker
import com.example.exchangeratecalculator.domain.model.USDC_CURRENCY
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class ConvertCurrencyUseCase @Inject constructor() {

    operator fun invoke(
        inputText: String,
        fromCode: String,
        toCode: String,
        ticker: RateTicker?,
    ): ConversionQuote {
        val fromAmount = parseAmount(inputText)
        val rate = tickerRateFor(fromCode, toCode, ticker)
        val outputAmount = convertAmount(fromAmount, fromCode, rate)

        return ConversionQuote(
            fromCode = fromCode,
            toCode = toCode,
            inputAmount = fromAmount,
            outputAmount = outputAmount,
            rate = rate,
        )
    }

    private fun parseAmount(inputText: String): BigDecimal =
        inputText.toBigDecimalOrZero().coerceAtLeast(BigDecimal.ZERO)

    private fun tickerRateFor(
        fromCode: String,
        toCode: String,
        ticker: RateTicker?,
    ): BigDecimal {
        if (fromCode == toCode) return BigDecimal.ONE
        if (ticker == null) return BigDecimal.ZERO

        // For a usdc_fiat book, selling USDC receives bid; buying USDC pays ask.
        val isSellingUsdc = fromCode == USDC_CURRENCY.code
        return if (isSellingUsdc) ticker.bid else ticker.ask
    }

    private fun convertAmount(
        fromAmount: BigDecimal,
        fromCode: String,
        rate: BigDecimal,
    ): BigDecimal {
        if (rate <= BigDecimal.ZERO) return displayAmount(BigDecimal.ZERO)
        val convertedAmount = if (fromCode == USDC_CURRENCY.code) {
            fromAmount.multiply(rate)
        } else {
            fromAmount.divide(rate, RATE_SCALE, RoundingMode.HALF_UP)
        }
        return displayAmount(convertedAmount)
    }

    private fun displayAmount(amount: BigDecimal): BigDecimal =
        amount.scaleForDisplay(DISPLAY_SCALE)

    companion object {
        private const val RATE_SCALE = 8
        private const val DISPLAY_SCALE = 2
    }
}

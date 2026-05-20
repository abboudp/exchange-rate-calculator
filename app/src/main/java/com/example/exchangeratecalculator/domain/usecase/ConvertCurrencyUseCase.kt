package com.example.exchangeratecalculator.domain.usecase

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
        val toAmount = computeToAmount(fromAmount, fromCode, toCode, ticker)
        val rate = effectiveRate(fromCode, toCode, ticker)
        return ConversionQuote(
            fromCode = fromCode,
            toCode = toCode,
            inputAmount = fromAmount,
            outputAmount = toAmount,
            rate = rate,
        )
    }

    private fun parseAmount(inputText: String): BigDecimal {
        if (inputText.isBlank()) return BigDecimal.ZERO
        return try {
            val parsed = BigDecimal(inputText.trim())
            if (parsed < BigDecimal.ZERO) BigDecimal.ZERO else parsed
        } catch (_: NumberFormatException) {
            BigDecimal.ZERO
        }
    }

    private fun computeToAmount(
        fromAmount: BigDecimal,
        fromCode: String,
        toCode: String,
        ticker: RateTicker?,
    ): BigDecimal {
        if (fromCode == toCode) return fromAmount.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
        if (ticker == null) return displayZero()
        return if (fromCode == USDC_CURRENCY.code) {
            if (ticker.bid <= BigDecimal.ZERO) displayZero()
            else fromAmount.multiply(ticker.bid).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
        } else {
            if (ticker.ask <= BigDecimal.ZERO) displayZero()
            else fromAmount
                .divide(ticker.ask, RATE_SCALE, RoundingMode.HALF_UP)
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
        }
    }

    private fun effectiveRate(
        fromCode: String,
        toCode: String,
        ticker: RateTicker?,
    ): BigDecimal {
        if (fromCode == toCode) return BigDecimal.ONE
        ticker ?: return BigDecimal.ZERO
        return if (fromCode == USDC_CURRENCY.code) ticker.bid else ticker.ask
    }

    private fun displayZero(): BigDecimal = BigDecimal.ZERO.setScale(DISPLAY_SCALE)

    companion object {
        private const val RATE_SCALE = 8
        private const val DISPLAY_SCALE = 2
    }
}

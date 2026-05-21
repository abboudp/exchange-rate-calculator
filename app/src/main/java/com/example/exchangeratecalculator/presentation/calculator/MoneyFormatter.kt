package com.example.exchangeratecalculator.presentation.calculator

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object MoneyFormatter {
    private const val USDC_CODE = "USDC"
    private const val USDC_DISPLAY = "USDc"
    private const val DOLLAR_PREFIX = "$"
    private const val CURSOR = "|"
    private const val AMOUNT_FRACTION_DIGITS = 2
    private const val RATE_MAX_FRACTION_DIGITS = 4

    private val amountFormat: NumberFormat =
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = AMOUNT_FRACTION_DIGITS
            maximumFractionDigits = AMOUNT_FRACTION_DIGITS
            isGroupingUsed = true
            roundingMode = RoundingMode.HALF_UP
        }

    private val rateFormat: NumberFormat =
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = RATE_MAX_FRACTION_DIGITS
            isGroupingUsed = true
            roundingMode = RoundingMode.HALF_UP
        }

    fun formatAmount(amount: BigDecimal): String = amountFormat.format(amount)

    fun formatRate(
        ask: BigDecimal,
        fromCode: String,
        toCode: String,
    ): String = "1 ${displayCode(fromCode)} = ${rateFormat.format(ask)} ${displayCode(toCode)}"

    fun formatAmountDisplay(
        rawText: String,
        isActive: Boolean,
    ): String {
        if (rawText.isEmpty()) return ""
        if (isActive) return DOLLAR_PREFIX + rawText + CURSOR
        val amount = rawText.toBigDecimalOrNull() ?: return ""
        return DOLLAR_PREFIX + formatAmount(amount)
    }

    private fun displayCode(code: String): String = if (code == USDC_CODE) USDC_DISPLAY else code
}

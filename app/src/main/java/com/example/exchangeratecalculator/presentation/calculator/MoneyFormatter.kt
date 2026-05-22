package com.example.exchangeratecalculator.presentation.calculator

import com.example.exchangeratecalculator.domain.model.USDC_CURRENCY
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object MoneyFormatter {
    private const val USDC_DISPLAY = "USDc"
    private const val DOLLAR_PREFIX = "$"
    private const val AMOUNT_FRACTION_DIGITS = 2
    private const val RATE_MAX_FRACTION_DIGITS = 4
    private const val DECIMAL_SEPARATOR = '.'

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

    private val integerGroupingFormat: NumberFormat =
        NumberFormat.getNumberInstance(Locale.US).apply {
            isGroupingUsed = true
            maximumFractionDigits = 0
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
        if (isActive) return DOLLAR_PREFIX + groupIntegerPart(rawText)
        val amount = rawText.toBigDecimalOrNull() ?: return ""
        return DOLLAR_PREFIX + formatAmount(amount)
    }

    private fun groupIntegerPart(rawText: String): String {
        val dotIndex = rawText.indexOf(DECIMAL_SEPARATOR)
        if (dotIndex == -1) return groupInteger(rawText)
        return groupInteger(rawText.substring(0, dotIndex)) + rawText.substring(dotIndex)
    }

    private fun groupInteger(text: String): String = text.toBigDecimalOrNull()?.let { integerGroupingFormat.format(it) } ?: text

    private fun displayCode(code: String): String = if (code == USDC_CURRENCY.code) USDC_DISPLAY else code
}

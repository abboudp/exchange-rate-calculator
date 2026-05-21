package com.example.exchangeratecalculator.presentation.calculator

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoneyFormatterTest {
    @Test
    fun formatAmount_roundsHalfUp() {
        assertEquals("1,841.01", MoneyFormatter.formatAmount(BigDecimal("1841.005")))
    }

    @Test
    fun formatAmount_zero_padsTwoDecimals() {
        assertEquals("0.00", MoneyFormatter.formatAmount(BigDecimal.ZERO))
    }

    @Test
    fun formatAmount_millionsGroupedWithCommasAndPaddedDecimals() {
        assertEquals("1,000,000.00", MoneyFormatter.formatAmount(BigDecimal("1000000")))
    }

    @Test
    fun formatAmount_smallValueRoundsUp() {
        assertEquals("0.01", MoneyFormatter.formatAmount(BigDecimal("0.005")))
    }

    @Test
    fun formatRate_usdcToMxn_keepsFourDecimalScale() {
        assertEquals(
            "1 USDc = 18.4105 MXN",
            MoneyFormatter.formatRate(BigDecimal("18.4105"), "USDC", "MXN"),
        )
    }

    @Test
    fun formatRate_usdcToCop_keepsTwoDecimalScaleWithGrouping() {
        assertEquals(
            "1 USDc = 3,832.42 COP",
            MoneyFormatter.formatRate(BigDecimal("3832.42"), "USDC", "COP"),
        )
    }

    @Test
    fun formatAmountDisplay_active_preservesRawWithCursor() {
        assertEquals("\$9999|", MoneyFormatter.formatAmountDisplay("9999", isActive = true))
    }

    @Test
    fun formatAmountDisplay_active_preservesTrailingDecimal() {
        assertEquals("\$12.|", MoneyFormatter.formatAmountDisplay("12.", isActive = true))
    }

    @Test
    fun formatAmountDisplay_inactive_formatsWithCommas() {
        assertEquals("\$184,065.59", MoneyFormatter.formatAmountDisplay("184065.59", isActive = false))
    }

    @Test
    fun formatAmountDisplay_emptyActive_returnsEmpty() {
        assertEquals("", MoneyFormatter.formatAmountDisplay("", isActive = true))
    }

    @Test
    fun formatAmountDisplay_emptyInactive_returnsEmpty() {
        assertEquals("", MoneyFormatter.formatAmountDisplay("", isActive = false))
    }
}

package com.example.exchangeratecalculator.core.precision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class BigDecimalExtTest {
    @Test
    fun toBigDecimalOrZero_empty_returnsZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo("".toBigDecimalOrZero()))
    }

    @Test
    fun toBigDecimalOrZero_blank_returnsZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo("   ".toBigDecimalOrZero()))
    }

    @Test
    fun toBigDecimalOrZero_unparseable_returnsZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo("abc".toBigDecimalOrZero()))
    }

    @Test
    fun toBigDecimalOrZero_validDecimal_returnsValue() {
        assertEquals(BigDecimal("18.4105"), "18.4105".toBigDecimalOrZero())
    }

    @Test
    fun scaleForDisplay_roundsHalfUp() {
        assertEquals(BigDecimal("1.01"), BigDecimal("1.005").scaleForDisplay(2))
    }

    @Test
    fun scaleForDisplay_roundsDownBelowHalf() {
        assertEquals(BigDecimal("1.00"), BigDecimal("1.004").scaleForDisplay(2))
    }

    @Test
    fun scaleForDisplay_defaultScaleIsTwo() {
        assertEquals(BigDecimal("1.01"), BigDecimal("1.005").scaleForDisplay())
    }

    @Test
    fun isPositive_smallPositive_true() {
        assertTrue(BigDecimal("0.01").isPositive())
    }

    @Test
    fun isPositive_zero_false() {
        assertFalse(BigDecimal.ZERO.isPositive())
    }

    @Test
    fun isPositive_negative_false() {
        assertFalse(BigDecimal("-1").isPositive())
    }
}

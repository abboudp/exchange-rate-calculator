package com.example.exchangeratecalculator.presentation.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputNormalizerTest {
    @Test
    fun onDigit_appendsToNumber() {
        assertEquals("123", InputNormalizer.onDigit("12", '3'))
    }

    @Test
    fun onDigit_appendsToEmpty() {
        assertEquals("5", InputNormalizer.onDigit("", '5'))
    }

    @Test
    fun onDigit_blocksThirdDecimalDigit() {
        assertEquals("1.23", InputNormalizer.onDigit("1.23", '4'))
    }

    @Test
    fun onDigit_allowsSecondDecimalDigit() {
        assertEquals("1.23", InputNormalizer.onDigit("1.2", '3'))
    }

    @Test
    fun onDigit_capsIntegerAtTenDigits() {
        assertEquals("9999999999", InputNormalizer.onDigit("9999999999", '1'))
    }

    @Test
    fun onDecimal_onEmpty_returnsDot() {
        assertEquals(".", InputNormalizer.onDecimal(""))
    }

    @Test
    fun onDecimal_onNumber_appendsDot() {
        assertEquals("12.", InputNormalizer.onDecimal("12"))
    }

    @Test
    fun onDecimal_duplicateBlocked() {
        assertEquals("12.", InputNormalizer.onDecimal("12."))
    }

    @Test
    fun onBackspace_removesLastChar() {
        assertEquals("12", InputNormalizer.onBackspace("123"))
    }

    @Test
    fun onBackspace_onEmpty_returnsEmpty() {
        assertEquals("", InputNormalizer.onBackspace(""))
    }

    @Test
    fun onBackspace_removesTrailingDecimal() {
        assertEquals("12", InputNormalizer.onBackspace("12."))
    }

    @Test
    fun normalize_replacesCommaWithDot() {
        assertEquals("12.5", InputNormalizer.normalize("12,5"))
    }

    @Test
    fun normalize_stripsInvalidChars() {
        assertEquals("12", InputNormalizer.normalize("1a2b"))
    }

    @Test
    fun normalize_collapsesMultipleDotsToFirst() {
        assertEquals("1.23", InputNormalizer.normalize("1.2.3"))
    }

    @Test
    fun canInsertDecimal_withDot_false() {
        assertFalse(InputNormalizer.canInsertDecimal("12."))
    }

    @Test
    fun canInsertDecimal_withoutDot_true() {
        assertTrue(InputNormalizer.canInsertDecimal("12"))
    }

    @Test
    fun canBackspace_nonEmpty_true() {
        assertTrue(InputNormalizer.canBackspace("1"))
    }

    @Test
    fun canBackspace_empty_false() {
        assertFalse(InputNormalizer.canBackspace(""))
    }
}

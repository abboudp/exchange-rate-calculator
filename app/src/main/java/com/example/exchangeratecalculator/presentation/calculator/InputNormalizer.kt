package com.example.exchangeratecalculator.presentation.calculator

object InputNormalizer {
    private const val MAX_DECIMAL_PLACES = 2

    // Structural ceiling — high enough to comfortably hold any realistic fiat
    // representation of the USDC notional cap (e.g. 1.5B ARS at $1M USDC).
    // The economic cap lives in the ViewModel as a USDC-equivalent check.
    private const val MAX_INTEGER_DIGITS = 15
    private const val DECIMAL_SEPARATOR = '.'

    fun onDigit(
        current: String,
        digit: Char,
    ): String {
        // Leading-zero rule: never let "0" grow into "00…"; a non-zero replaces
        // it so the user can fix a fat-finger tap.
        if (current == "0") return if (digit == '0') current else digit.toString()

        val dotIndex = current.indexOf(DECIMAL_SEPARATOR)
        if (dotIndex == -1) {
            if (current.length >= MAX_INTEGER_DIGITS) return current
        } else {
            val decimalDigits = current.length - dotIndex - 1
            if (decimalDigits >= MAX_DECIMAL_PLACES) return current
        }
        return current + digit
    }

    fun onDecimal(current: String): String {
        if (current.contains(DECIMAL_SEPARATOR)) return current
        // Empty + "." should display as "0." not just "."
        return if (current.isEmpty()) "0$DECIMAL_SEPARATOR" else current + DECIMAL_SEPARATOR
    }

    fun onBackspace(current: String): String = if (current.isEmpty()) current else current.dropLast(1)

    fun canInsertDecimal(current: String): Boolean = !current.contains(DECIMAL_SEPARATOR)

    fun canBackspace(current: String): Boolean = current.isNotEmpty()

    fun normalize(raw: String): String {
        val cleaned =
            raw
                .replace(',', DECIMAL_SEPARATOR)
                .filter { it.isDigit() || it == DECIMAL_SEPARATOR }
        val firstDot = cleaned.indexOf(DECIMAL_SEPARATOR)
        return if (firstDot == -1) {
            cleaned
        } else {
            cleaned.substring(0, firstDot + 1) +
                cleaned.substring(firstDot + 1).replace(DECIMAL_SEPARATOR.toString(), "")
        }
    }
}

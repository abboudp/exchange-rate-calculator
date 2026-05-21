package com.example.exchangeratecalculator.core.precision

import java.math.BigDecimal
import java.math.RoundingMode

fun String.toBigDecimalOrZero(): BigDecimal = trim().toBigDecimalOrNull() ?: BigDecimal.ZERO

fun BigDecimal.scaleForDisplay(scale: Int = 2): BigDecimal = setScale(scale, RoundingMode.HALF_UP)

fun BigDecimal.isPositive(): Boolean = signum() > 0

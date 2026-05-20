package com.example.exchangeratecalculator.domain.model

import java.math.BigDecimal

data class ConversionQuote(
    val fromCode: String,
    val toCode: String,
    val inputAmount: BigDecimal,
    val outputAmount: BigDecimal,
    val rate: BigDecimal,
)

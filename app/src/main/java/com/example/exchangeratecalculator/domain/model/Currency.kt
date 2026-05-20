package com.example.exchangeratecalculator.domain.model

data class Currency(
    val code: String,
    val isBase: Boolean,
)

val USDC_CURRENCY = Currency(code = "USDC", isBase = true)

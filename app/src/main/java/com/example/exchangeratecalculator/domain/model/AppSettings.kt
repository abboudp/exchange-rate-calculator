package com.example.exchangeratecalculator.domain.model

const val DEFAULT_FIAT_CODE = "MXN"

data class AppSettings(
    val selectedFiatCode: String = DEFAULT_FIAT_CODE,
    val isSwapped: Boolean = false,
)

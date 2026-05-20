package com.example.exchangeratecalculator.domain.model

data class AppSettings(
    val selectedFiatCode: String = "MXN",
    val isSwapped: Boolean = false,
)

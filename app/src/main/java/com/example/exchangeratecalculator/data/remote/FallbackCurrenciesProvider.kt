package com.example.exchangeratecalculator.data.remote

import com.example.exchangeratecalculator.domain.model.Currency

object FallbackCurrenciesProvider {
    val currencies: List<Currency> = listOf(
        Currency(code = "MXN", isBase = false),
        Currency(code = "ARS", isBase = false),
        Currency(code = "BRL", isBase = false),
        Currency(code = "COP", isBase = false),
    )

    val queryCodes: String = currencies.joinToString(separator = ",") { it.code }
}

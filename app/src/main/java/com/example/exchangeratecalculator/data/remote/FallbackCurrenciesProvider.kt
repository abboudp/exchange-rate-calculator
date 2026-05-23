package com.example.exchangeratecalculator.data.remote

import com.example.exchangeratecalculator.domain.model.Currency

object FallbackCurrenciesProvider {
    val currencies: List<Currency> =
        SupportedCurrency.entries.map { Currency(code = it.code, isBase = false) }

    val queryCodes: String = currencies.joinToString(separator = ",") { it.code }
}

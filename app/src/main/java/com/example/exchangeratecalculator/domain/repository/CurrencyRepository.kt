package com.example.exchangeratecalculator.domain.repository

import com.example.exchangeratecalculator.domain.model.Currency

interface CurrencyRepository {
    suspend fun getAvailableCurrencies(): List<Currency>
}

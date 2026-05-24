package com.example.exchangeratecalculator.domain.repository

import com.example.exchangeratecalculator.domain.model.Currency
import kotlinx.coroutines.flow.Flow

interface CurrencyRepository {
    fun observeAvailableCurrencies(): Flow<List<Currency>>
}

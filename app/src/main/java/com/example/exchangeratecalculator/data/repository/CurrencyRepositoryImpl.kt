package com.example.exchangeratecalculator.data.repository

import com.example.exchangeratecalculator.data.remote.FallbackCurrenciesProvider
import com.example.exchangeratecalculator.domain.model.Currency
import com.example.exchangeratecalculator.domain.repository.CurrencyRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepositoryImpl
    @Inject
    constructor() : CurrencyRepository {
        override suspend fun getAvailableCurrencies(): List<Currency> = FallbackCurrenciesProvider.currencies
    }

package com.example.exchangeratecalculator.domain.usecase

import com.example.exchangeratecalculator.domain.model.Currency
import com.example.exchangeratecalculator.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAvailableCurrenciesUseCase
    @Inject
    constructor(
        private val repository: CurrencyRepository,
    ) {
        operator fun invoke(): Flow<List<Currency>> = repository.observeAvailableCurrencies()
    }

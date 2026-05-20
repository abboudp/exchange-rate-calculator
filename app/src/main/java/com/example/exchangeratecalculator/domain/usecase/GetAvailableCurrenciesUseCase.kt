package com.example.exchangeratecalculator.domain.usecase

import com.example.exchangeratecalculator.domain.model.Currency
import com.example.exchangeratecalculator.domain.repository.CurrencyRepository
import javax.inject.Inject

class GetAvailableCurrenciesUseCase @Inject constructor(
    private val repository: CurrencyRepository,
) {
    suspend operator fun invoke(): List<Currency> = repository.getAvailableCurrencies()
}

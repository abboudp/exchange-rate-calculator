package com.example.exchangeratecalculator.data.repository

import com.example.exchangeratecalculator.data.local.SettingsDataStore
import com.example.exchangeratecalculator.data.remote.DolarApi
import com.example.exchangeratecalculator.data.remote.FallbackCurrenciesProvider
import com.example.exchangeratecalculator.di.ApplicationScope
import com.example.exchangeratecalculator.domain.model.Currency
import com.example.exchangeratecalculator.domain.repository.CurrencyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepositoryImpl
    @Inject
    constructor(
        private val api: DolarApi,
        private val settingsDataStore: SettingsDataStore,
        @param:ApplicationScope private val appScope: CoroutineScope,
    ) : CurrencyRepository {
        private val currencies: StateFlow<List<Currency>> =
            flow {
                val cached = settingsDataStore.getCurrencyCodes()
                emit(
                    if (cached.isNotEmpty()) {
                        cached.map { Currency(code = it, isBase = false) }
                    } else {
                        FallbackCurrenciesProvider.currencies
                    },
                )

                val fetched =
                    runCatching {
                        withTimeout(5_000) { api.getAvailableCurrencies() }
                    }.getOrNull()

                if (!fetched.isNullOrEmpty()) {
                    settingsDataStore.saveCurrencyCodes(fetched)
                    emit(fetched.map { Currency(code = it, isBase = false) })
                }
            }.stateIn(
                scope = appScope,
                started = SharingStarted.Eagerly,
                initialValue = FallbackCurrenciesProvider.currencies,
            )

        override fun observeAvailableCurrencies(): Flow<List<Currency>> = currencies
    }

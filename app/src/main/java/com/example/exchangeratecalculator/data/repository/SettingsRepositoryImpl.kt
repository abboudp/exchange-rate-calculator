package com.example.exchangeratecalculator.data.repository

import com.example.exchangeratecalculator.data.local.SettingsDataStore
import com.example.exchangeratecalculator.domain.model.AppSettings
import com.example.exchangeratecalculator.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl
    @Inject
    constructor(
        private val dataStore: SettingsDataStore,
    ) : SettingsRepository {
        override fun observeSettings(): Flow<AppSettings> = dataStore.observeSettings()

        override suspend fun updateSelectedCurrency(code: String) {
            dataStore.updateSelectedCurrency(code)
        }

        override suspend fun updateSwapState(isSwapped: Boolean) {
            dataStore.updateSwapState(isSwapped)
        }
    }

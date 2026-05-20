package com.example.exchangeratecalculator.domain.repository

import com.example.exchangeratecalculator.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>

    suspend fun updateSelectedCurrency(code: String)

    suspend fun updateSwapState(isSwapped: Boolean)
}

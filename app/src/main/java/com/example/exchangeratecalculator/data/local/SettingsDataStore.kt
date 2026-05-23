package com.example.exchangeratecalculator.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.exchangeratecalculator.domain.model.AppSettings
import com.example.exchangeratecalculator.domain.model.DEFAULT_FIAT_CODE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore
    @Inject
    constructor(
        private val store: DataStore<Preferences>,
    ) {
        fun observeSettings(): Flow<AppSettings> =
            store.data.map { prefs ->
                AppSettings(
                    selectedFiatCode = prefs[KEY_SELECTED_FIAT_CODE] ?: DEFAULTS.selectedFiatCode,
                    isSwapped = prefs[KEY_IS_SWAPPED] ?: DEFAULTS.isSwapped,
                )
            }

        suspend fun updateSelectedCurrency(code: String) {
            store.edit { prefs -> prefs[KEY_SELECTED_FIAT_CODE] = code }
        }

        suspend fun updateSwapState(isSwapped: Boolean) {
            store.edit { prefs -> prefs[KEY_IS_SWAPPED] = isSwapped }
        }

        companion object {
            private val KEY_SELECTED_FIAT_CODE = stringPreferencesKey("selected_fiat_code")
            private val KEY_IS_SWAPPED = booleanPreferencesKey("is_swapped")
            private val DEFAULTS = AppSettings(selectedFiatCode = DEFAULT_FIAT_CODE)
        }
    }

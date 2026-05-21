package com.example.exchangeratecalculator.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsDataStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun newStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { tempFolder.newFile("test.preferences_pb") },
    )

    @Test
    fun defaults_whenNothingPersisted() = runBlocking {
        val settings = SettingsDataStore(newStore()).observeSettings().first()

        assertEquals("MXN", settings.selectedFiatCode)
        assertFalse(settings.isSwapped)
    }

    @Test
    fun updateSelectedCurrency_isPersisted() = runBlocking {
        val dataStore = SettingsDataStore(newStore())

        dataStore.updateSelectedCurrency("ARS")

        assertEquals("ARS", dataStore.observeSettings().first().selectedFiatCode)
    }

    @Test
    fun updateSwapState_isPersisted() = runBlocking {
        val dataStore = SettingsDataStore(newStore())

        dataStore.updateSwapState(true)

        assertTrue(dataStore.observeSettings().first().isSwapped)
    }
}

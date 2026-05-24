package com.example.exchangeratecalculator.data.repository

import com.example.exchangeratecalculator.data.local.SettingsDataStore
import com.example.exchangeratecalculator.data.remote.DolarApi
import com.example.exchangeratecalculator.data.remote.FallbackCurrenciesProvider
import com.example.exchangeratecalculator.data.remote.RateTickerDto
import com.example.exchangeratecalculator.domain.model.Currency
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyRepositoryImplTest {
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val mockDataStore = mockk<SettingsDataStore>()

    @Before
    fun setUp() {
        // Provide a safe default so individual tests only stub what they care about.
        coEvery { mockDataStore.getCurrencyCodes() } returns emptyList()
        coEvery { mockDataStore.saveCurrencyCodes(any()) } just Runs
    }

    @Test
    fun noCacheApiFails_emitsFallback() =
        runBlocking {
            val repo = newRepo(api = FakeCurrencyApi(error = IOException("unavailable")))

            val result = repo.observeAvailableCurrencies().first()

            assertEquals(FallbackCurrenciesProvider.currencies, result)
        }

    @Test
    fun noCacheApiSucceeds_emitsApiResult() =
        runBlocking {
            val repo = newRepo(api = FakeCurrencyApi(response = listOf("MXN", "ARS")))

            val result = repo.observeAvailableCurrencies().first()

            assertEquals(listOf(Currency("MXN", false), Currency("ARS", false)), result)
        }

    @Test
    fun noCacheApiSucceeds_savesCodestoDataStore() =
        runBlocking {
            val slot = slot<List<String>>()
            coEvery { mockDataStore.saveCurrencyCodes(capture(slot)) } just Runs
            newRepo(api = FakeCurrencyApi(response = listOf("MXN", "ARS")))

            assertEquals(listOf("MXN", "ARS"), slot.captured)
        }

    @Test
    fun cacheExists_apiFails_emitsCache() =
        runBlocking {
            coEvery { mockDataStore.getCurrencyCodes() } returns listOf("MXN", "BRL")
            val repo = newRepo(api = FakeCurrencyApi(error = IOException()))

            val result = repo.observeAvailableCurrencies().first()

            assertEquals(listOf(Currency("MXN", false), Currency("BRL", false)), result)
        }

    @Test
    fun apiReturnsEmptyList_doesNotSaveCodes() =
        runBlocking {
            newRepo(api = FakeCurrencyApi(response = emptyList()))

            coVerify(exactly = 0) { mockDataStore.saveCurrencyCodes(any()) }
        }

    private fun newRepo(
        api: DolarApi = FakeCurrencyApi(error = IOException("unavailable")),
        dataStore: SettingsDataStore = mockDataStore,
    ) = CurrencyRepositoryImpl(api, dataStore, testScope)

    private class FakeCurrencyApi(
        private val response: List<String> = emptyList(),
        private val error: Exception? = null,
    ) : DolarApi {
        override suspend fun getTickers(currencies: String): List<RateTickerDto> = emptyList()

        override suspend fun getAvailableCurrencies(): List<String> {
            error?.let { throw it }
            return response
        }
    }
}

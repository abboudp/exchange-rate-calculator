package com.example.exchangeratecalculator.data.repository

import com.example.exchangeratecalculator.core.coroutine.DispatcherProvider
import com.example.exchangeratecalculator.data.local.RateTickerDao
import com.example.exchangeratecalculator.data.local.RateTickerEntity
import com.example.exchangeratecalculator.data.remote.DolarApi
import com.example.exchangeratecalculator.data.remote.FallbackCurrenciesProvider
import com.example.exchangeratecalculator.data.remote.RateTickerDto
import com.example.exchangeratecalculator.domain.model.Currency
import com.example.exchangeratecalculator.domain.model.RateResource
import com.example.exchangeratecalculator.domain.repository.CurrencyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class RateRepositoryImplTest {
    private val sampleDto =
        RateTickerDto(
            ask = "18.4105",
            bid = "18.4069",
            book = "usdc_mxn",
            date = "2026-05-20T12:00:00",
        )

    @Test
    fun observeRateTicker_emitsLoadingFirst() =
        runTest {
            val repo = newRepo(api = FakeApi(response = emptyList()))

            val first = repo.observeRateTicker("MXN").first()

            assertEquals(RateResource.Loading, first)
        }

    @Test
    fun observeRateTicker_apiSuccess_upsertsWithLowercaseBook() =
        runTest {
            val dao = FakeDao()
            val repo = newRepo(api = FakeApi(response = listOf(sampleDto)), dao = dao)

            val first = repo.observeRateTicker("MXN").first()

            assertTrue("expected Available after upsert", first is RateResource.Available)
            assertTrue(dao.upserts.isNotEmpty())
            assertTrue(dao.upserts.all { it.book == "usdc_mxn" })
        }

    @Test
    fun observeRateTicker_apiSuccess_upsertsAllReturnedBooks() =
        runTest {
            val dao = FakeDao()
            val books = listOf("usdc_mxn", "usdc_ars", "usdc_brl", "usdc_cop")
            val response = books.map { sampleDto.copy(book = it) }
            val repo = newRepo(api = FakeApi(response = response), dao = dao)

            repo.observeRateTicker("MXN").take(2).toList()

            assertEquals(books.toSet(), dao.upserts.map { it.book }.toSet())
        }

    @Test
    fun observeRateTicker_cachedEntity_emitsAvailable() =
        runTest {
            val dao = FakeDao().apply { ticker.value = entity() }
            val repo = newRepo(dao = dao)

            val first = repo.observeRateTicker("MXN").first()

            assertTrue(first is RateResource.Available)
        }

    @Test
    fun observeRateTicker_networkFailure_servesCachedWithoutCrashing() =
        runTest {
            val dao = FakeDao().apply { ticker.value = entity() }
            val repo = newRepo(api = FakeApi(error = IOException("offline")), dao = dao)

            val first = repo.observeRateTicker("MXN").first()

            assertTrue(first is RateResource.Available)
            assertEquals(0, dao.upserts.size)
        }

    @Test
    fun fetchAllTickers_usesQueryCodesFromCurrencyRepository() =
        runTest {
            val customCurrencies = listOf(Currency("MXN", false), Currency("ARS", false))
            val capturedQueries = mutableListOf<String>()
            val capturingApi =
                object : DolarApi {
                    override suspend fun getTickers(currencies: String): List<RateTickerDto> {
                        capturedQueries += currencies
                        return emptyList()
                    }

                    override suspend fun getAvailableCurrencies(): List<String> = emptyList()
                }
            val repo =
                newRepo(
                    api = capturingApi,
                    currencyRepository = FakeCurrencyRepository(customCurrencies),
                )

            repo.observeRateTicker("MXN").take(2).toList()

            assertTrue("expected query 'MXN,ARS' but got $capturedQueries", capturedQueries.any { it == "MXN,ARS" })
        }

    @Test
    fun batchFails_individualSucceeds_upsertsPartialResults() =
        runTest {
            val dao = FakeDao()
            val repo =
                newRepo(
                    api = BatchFailingApi(individualResponse = listOf(sampleDto)),
                    dao = dao,
                    currencyRepository = FakeCurrencyRepository(listOf(Currency("MXN", false), Currency("ARS", false))),
                )

            repo.observeRateTicker("MXN").take(2).toList()

            assertTrue("expected upsert after individual fallback", dao.upserts.isNotEmpty())
        }

    @Test
    fun batchFails_individualNetworkFail_marksOffline() =
        runTest {
            val repo =
                newRepo(
                    api = BatchFailingApi(individualError = IOException("no network")),
                    currencyRepository = FakeCurrencyRepository(listOf(Currency("MXN", false))),
                )

            val results = repo.observeRateTicker("MXN").take(2).toList()

            assertTrue(
                "expected Unavailable(OFFLINE) but got $results",
                results.any {
                    it is RateResource.Unavailable &&
                        it.reason == RateResource.UnavailableReason.OFFLINE
                },
            )
        }

    @Test
    fun batchFails_individualHttpFail_marksRateUnavailable() =
        runTest {
            val repo =
                newRepo(
                    api = BatchFailingApi(individualError = RuntimeException("404 not found")),
                    currencyRepository = FakeCurrencyRepository(listOf(Currency("MXN", false))),
                )

            val results = repo.observeRateTicker("MXN").take(2).toList()

            assertTrue(
                "expected Unavailable(RATE_UNAVAILABLE) but got $results",
                results.any {
                    it is RateResource.Unavailable &&
                        it.reason == RateResource.UnavailableReason.RATE_UNAVAILABLE
                },
            )
        }

    private fun TestScope.newRepo(
        api: DolarApi = FakeApi(response = emptyList()),
        dao: FakeDao = FakeDao(),
        currencyRepository: CurrencyRepository = FakeCurrencyRepository(),
    ): RateRepositoryImpl =
        RateRepositoryImpl(
            api = api,
            dao = dao,
            dispatchers = TestDispatcherProvider(UnconfinedTestDispatcher(testScheduler)),
            currencyRepository = currencyRepository,
            appScope = backgroundScope,
        )

    private fun entity(
        book: String = "usdc_mxn",
        ask: String = "18.4105",
        bid: String = "18.4069",
        fetchedAtEpochMs: Long = 0L,
    ) = RateTickerEntity(book, ask, bid, fetchedAtEpochMs)

    private class FakeApi(
        private val response: List<RateTickerDto> = emptyList(),
        private val error: Exception? = null,
    ) : DolarApi {
        override suspend fun getTickers(currencies: String): List<RateTickerDto> {
            error?.let { throw it }
            return response
        }

        override suspend fun getAvailableCurrencies(): List<String> = emptyList()
    }

    private class BatchFailingApi(
        private val individualResponse: List<RateTickerDto> = emptyList(),
        private val individualError: Exception? = null,
    ) : DolarApi {
        override suspend fun getTickers(currencies: String): List<RateTickerDto> {
            if (currencies.contains(",")) throw IOException("batch unavailable")
            individualError?.let { throw it }
            return individualResponse
        }

        override suspend fun getAvailableCurrencies(): List<String> = emptyList()
    }

    private class FakeDao : RateTickerDao {
        val ticker = MutableStateFlow<RateTickerEntity?>(null)
        val upserts = mutableListOf<RateTickerEntity>()

        override fun observeTicker(book: String): Flow<RateTickerEntity?> = ticker

        override suspend fun upsertTicker(entity: RateTickerEntity) {
            upserts += entity
            ticker.value = entity
        }
    }

    private class FakeCurrencyRepository(
        private val currencies: List<Currency> = FallbackCurrenciesProvider.currencies,
    ) : CurrencyRepository {
        override fun observeAvailableCurrencies(): Flow<List<Currency>> = flowOf(currencies)
    }

    private class TestDispatcherProvider(
        private val dispatcher: CoroutineDispatcher,
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
    }
}

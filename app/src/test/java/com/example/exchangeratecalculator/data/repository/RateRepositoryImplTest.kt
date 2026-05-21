package com.example.exchangeratecalculator.data.repository

import com.example.exchangeratecalculator.core.coroutine.DispatcherProvider
import com.example.exchangeratecalculator.data.local.RateTickerDao
import com.example.exchangeratecalculator.data.local.RateTickerEntity
import com.example.exchangeratecalculator.data.remote.DolarApi
import com.example.exchangeratecalculator.data.remote.RateTickerDto
import com.example.exchangeratecalculator.domain.model.RateResource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

    private val sampleDto = RateTickerDto(
        ask = "18.4105",
        bid = "18.4069",
        book = "usdc_mxn",
        date = "2026-05-20T12:00:00",
    )

    @Test
    fun observeRateTicker_emitsLoadingFirst() = runTest {
        val repo = newRepo(api = FakeApi(response = emptyList()))

        val first = repo.observeRateTicker("MXN").first()

        assertEquals(RateResource.Loading, first)
    }

    @Test
    fun observeRateTicker_apiSuccess_upsertsWithLowercaseBookAndComputedExpiry() = runTest {
        val dao = FakeDao()
        val repo = newRepo(api = FakeApi(response = listOf(sampleDto)), dao = dao)

        val emissions = repo.observeRateTicker("MXN").take(2).toList()

        assertEquals(RateResource.Loading, emissions[0])
        assertTrue("expected Fresh after upsert", emissions[1] is RateResource.Fresh)
        assertEquals(1, dao.upserts.size)
        val upserted = dao.upserts.single()
        assertEquals("usdc_mxn", upserted.book)
        assertEquals(RateRepositoryImpl.TTL_MS, upserted.expiresAtEpochMs - upserted.fetchedAtEpochMs)
    }

    @Test
    fun observeRateTicker_apiSuccess_upsertsAllReturnedBooks() = runTest {
        val dao = FakeDao()
        val books = listOf("usdc_mxn", "usdc_ars", "usdc_brl", "usdc_cop")
        val response = books.map { sampleDto.copy(book = it) }
        val repo = newRepo(api = FakeApi(response = response), dao = dao)

        repo.observeRateTicker("MXN").take(2).toList()

        assertEquals(books.toSet(), dao.upserts.map { it.book }.toSet())
    }

    @Test
    fun observeRateTicker_freshCachedEntity_emitsFresh() = runTest {
        val dao = FakeDao().apply {
            ticker.value = entity(expiresAtEpochMs = System.currentTimeMillis() + 60_000L)
        }
        val repo = newRepo(dao = dao)

        val emissions = repo.observeRateTicker("MXN").take(2).toList()

        assertEquals(RateResource.Loading, emissions[0])
        assertTrue("expected Fresh but got ${emissions[1]}", emissions[1] is RateResource.Fresh)
    }

    @Test
    fun observeRateTicker_staleCachedEntity_emitsStale() = runTest {
        val dao = FakeDao().apply {
            ticker.value = entity(expiresAtEpochMs = System.currentTimeMillis() - 60_000L)
        }
        val repo = newRepo(dao = dao)

        val emissions = repo.observeRateTicker("MXN").take(2).toList()

        assertEquals(RateResource.Loading, emissions[0])
        assertTrue("expected Stale but got ${emissions[1]}", emissions[1] is RateResource.Stale)
    }

    @Test
    fun observeRateTicker_networkFailure_servesCachedStaleWithoutCrashing() = runTest {
        val dao = FakeDao().apply {
            ticker.value = entity(expiresAtEpochMs = System.currentTimeMillis() - 60_000L)
        }
        val repo = newRepo(api = FakeApi(error = IOException("offline")), dao = dao)

        val emissions = repo.observeRateTicker("MXN").take(2).toList()

        assertEquals(RateResource.Loading, emissions[0])
        assertTrue(emissions[1] is RateResource.Stale)
        assertEquals(0, dao.upserts.size)
    }

    private fun TestScope.newRepo(
        api: DolarApi = FakeApi(response = emptyList()),
        dao: FakeDao = FakeDao(),
    ): RateRepositoryImpl = RateRepositoryImpl(
        api = api,
        dao = dao,
        dispatchers = TestDispatcherProvider(UnconfinedTestDispatcher(testScheduler)),
    )

    private fun entity(
        book: String = "usdc_mxn",
        ask: String = "18.4105",
        bid: String = "18.4069",
        fetchedAtEpochMs: Long = 0L,
        expiresAtEpochMs: Long,
    ) = RateTickerEntity(book, ask, bid, fetchedAtEpochMs, expiresAtEpochMs)

    private class FakeApi(
        private val response: List<RateTickerDto> = emptyList(),
        private val error: Exception? = null,
    ) : DolarApi {
        override suspend fun getTickers(currencies: String): List<RateTickerDto> {
            error?.let { throw it }
            return response
        }
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

    private class TestDispatcherProvider(
        private val dispatcher: CoroutineDispatcher,
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
    }
}

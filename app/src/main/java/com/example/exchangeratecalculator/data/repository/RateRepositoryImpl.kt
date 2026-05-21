package com.example.exchangeratecalculator.data.repository

import com.example.exchangeratecalculator.core.coroutine.DispatcherProvider
import com.example.exchangeratecalculator.data.local.RateTickerDao
import com.example.exchangeratecalculator.data.local.toDomain
import com.example.exchangeratecalculator.data.remote.DolarApi
import com.example.exchangeratecalculator.data.remote.FallbackCurrenciesProvider
import com.example.exchangeratecalculator.data.remote.toEntity
import com.example.exchangeratecalculator.domain.model.RateResource
import com.example.exchangeratecalculator.domain.model.isStale
import com.example.exchangeratecalculator.domain.repository.RateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateRepositoryImpl @Inject constructor(
    private val api: DolarApi,
    private val dao: RateTickerDao,
    private val dispatchers: DispatcherProvider,
) : RateRepository {

    override fun observeRateTicker(fiatCode: String): Flow<RateResource> = channelFlow {
        val book = "usdc_${fiatCode.lowercase()}"
        send(RateResource.Loading)

        launch {
            while (isActive) {
                try {
                    val dtos = api.getTickers(FallbackCurrenciesProvider.queryCodes)
                    dtos.firstOrNull { it.book == book }?.let { dto ->
                        val now = System.currentTimeMillis()
                        dao.upsertTicker(
                            dto.toEntity(
                                fetchedAtEpochMs = now,
                                expiresAtEpochMs = now + TTL_MS,
                            ),
                        )
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
                delay(POLL_INTERVAL_MS)
            }
        }

        dao.observeTicker(book).collect { entity ->
            if (entity != null) {
                val ticker = entity.toDomain()
                send(if (ticker.isStale) RateResource.Stale(ticker) else RateResource.Fresh(ticker))
            }
        }
    }.flowOn(dispatchers.io)

    companion object {
        const val TTL_MS = 5 * 60 * 1000L
        const val POLL_INTERVAL_MS = 30_000L
    }
}

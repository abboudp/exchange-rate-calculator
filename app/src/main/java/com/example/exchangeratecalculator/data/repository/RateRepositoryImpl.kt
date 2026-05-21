package com.example.exchangeratecalculator.data.repository

import com.example.exchangeratecalculator.core.coroutine.DispatcherProvider
import com.example.exchangeratecalculator.data.local.RateTickerDao
import com.example.exchangeratecalculator.data.local.RateTickerEntity
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateRepositoryImpl
    @Inject
    constructor(
        private val api: DolarApi,
        private val dao: RateTickerDao,
        private val dispatchers: DispatcherProvider,
    ) : RateRepository {
        override fun observeRateTicker(fiatCode: String): Flow<RateResource> =
            channelFlow {
                val book = "usdc_${fiatCode.lowercase()}"
                send(RateResource.Loading)
                // Tracks whether the most recent poll attempt failed. Reads happen on
                // the channelFlow's downstream emit path; writes happen in the poll
                // loop launched below. Atomic to avoid a torn read across threads.
                val pollFailedRecently = AtomicBoolean(false)

                launch {
                    while (isActive) {
                        try {
                            val dtos = api.getTickers(FallbackCurrenciesProvider.queryCodes)
                            val now = System.currentTimeMillis()
                            dtos.forEach { dto ->
                                dao.upsertTicker(
                                    dto.toEntity(
                                        fetchedAtEpochMs = now,
                                        expiresAtEpochMs = now + TTL_MS,
                                    ),
                                )
                            }
                            pollFailedRecently.set(false)
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            pollFailedRecently.set(true)
                        }
                        delay(POLL_INTERVAL_MS)
                    }
                }

                // Re-emits periodically so isStale (a getter that compares
                // System.currentTimeMillis() against the stored expiresAtEpochMs)
                // is re-evaluated even when Room hasn't written a new row. Without
                // this, airplane-mode users would stay on Fresh forever.
                val staleCheckTicks: Flow<Unit> =
                    flow {
                        while (true) {
                            emit(Unit)
                            delay(STALE_RECHECK_INTERVAL_MS)
                        }
                    }

                combine(dao.observeTicker(book), staleCheckTicks) { entity, _ -> entity }
                    .collect { entity ->
                        if (entity != null) {
                            send(entity.toResource(pollFailedRecently.get()))
                        }
                    }
            }.flowOn(dispatchers.io)

        private fun RateTickerEntity.toResource(pollFailed: Boolean): RateResource {
            val ticker = toDomain()
            return when {
                ticker.isStale -> RateResource.Stale(ticker)
                pollFailed -> RateResource.Degraded(ticker)
                else -> RateResource.Fresh(ticker)
            }
        }

        companion object {
            const val TTL_MS = 5 * 60 * 1000L
            const val POLL_INTERVAL_MS = 30_000L
            const val STALE_RECHECK_INTERVAL_MS = 10_000L
        }
    }

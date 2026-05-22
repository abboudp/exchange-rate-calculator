package com.example.exchangeratecalculator.data.repository

import com.example.exchangeratecalculator.core.coroutine.DispatcherProvider
import com.example.exchangeratecalculator.data.local.RateTickerDao
import com.example.exchangeratecalculator.data.local.toDomain
import com.example.exchangeratecalculator.data.remote.DolarApi
import com.example.exchangeratecalculator.data.remote.FallbackCurrenciesProvider
import com.example.exchangeratecalculator.data.remote.toEntity
import com.example.exchangeratecalculator.domain.model.RateResource
import com.example.exchangeratecalculator.domain.repository.RateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
                // Flag whether the most recent poll attempt failed. Used only
                // to distinguish "still trying" from "offline with no cache"
                // — once a cached row exists we serve it regardless of this.
                val pollFailedFlow = MutableStateFlow(false)

                launch {
                    while (isActive) {
                        val succeeded =
                            try {
                                val dtos = api.getTickers(FallbackCurrenciesProvider.queryCodes)
                                val now = System.currentTimeMillis()
                                dtos.forEach { dto -> dao.upsertTicker(dto.toEntity(fetchedAtEpochMs = now)) }
                                pollFailedFlow.value = false
                                true
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                pollFailedFlow.value = true
                                false
                            }
                        // After a failed poll, retry quickly so reconnection
                        // recovers within seconds instead of a full poll cycle.
                        delay(if (succeeded) POLL_INTERVAL_MS else RETRY_INTERVAL_MS)
                    }
                }

                combine(dao.observeTicker(book), pollFailedFlow) { entity, failed ->
                    entity to failed
                }.collect { (entity, failed) ->
                    when {
                        entity != null -> send(RateResource.Available(entity.toDomain()))
                        failed -> send(RateResource.Unavailable("offline"))
                        // else: pre-first-poll with no cache — stay Loading.
                    }
                }
            }.flowOn(dispatchers.io)

        companion object {
            const val POLL_INTERVAL_MS = 60_000L
            const val RETRY_INTERVAL_MS = 5_000L
        }
    }

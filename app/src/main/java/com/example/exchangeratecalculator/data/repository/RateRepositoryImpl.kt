package com.example.exchangeratecalculator.data.repository

import android.util.Log
import com.example.exchangeratecalculator.core.coroutine.DispatcherProvider
import com.example.exchangeratecalculator.data.local.RateTickerDao
import com.example.exchangeratecalculator.data.local.toDomain
import com.example.exchangeratecalculator.data.remote.DolarApi
import com.example.exchangeratecalculator.data.remote.FallbackCurrenciesProvider
import com.example.exchangeratecalculator.data.remote.toEntity
import com.example.exchangeratecalculator.di.ApplicationScope
import com.example.exchangeratecalculator.domain.model.RateResource
import com.example.exchangeratecalculator.domain.repository.RateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
        @param:ApplicationScope private val appScope: CoroutineScope,
    ) : RateRepository {
        init {
            appScope.launch(dispatchers.io) {
                try {
                    fetchAllTickers()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Startup fetch failed", e)
                }
            }
        }

        override fun observeRateTicker(fiatCode: String): Flow<RateResource> =
            channelFlow {
                val book = "usdc_${fiatCode.lowercase()}"
                val pollFailedFlow = MutableStateFlow(false)

                val initialEntity = dao.observeTicker(book).first()
                send(
                    if (initialEntity != null) {
                        RateResource.Available(initialEntity.toDomain())
                    } else {
                        RateResource.Loading
                    },
                )

                launch {
                    while (isActive) {
                        val succeeded =
                            try {
                                fetchAllTickers()
                                pollFailedFlow.value = false
                                true
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                Log.e(TAG, "Rate fetch failed", e)
                                pollFailedFlow.value = true
                                false
                            }
                        delay(if (succeeded) POLL_INTERVAL_MS else RETRY_INTERVAL_MS)
                    }
                }

                combine(dao.observeTicker(book), pollFailedFlow) { entity, failed ->
                    entity to failed
                }.collect { (entity, failed) ->
                    when {
                        entity != null -> send(RateResource.Available(entity.toDomain()))
                        failed -> send(RateResource.Unavailable("offline"))
                    }
                }
            }.flowOn(dispatchers.io)

        private suspend fun fetchAllTickers() {
            val dtos = api.getTickers(FallbackCurrenciesProvider.queryCodes)
            val now = System.currentTimeMillis()
            dtos.forEach { dto -> dto.toEntity(fetchedAtEpochMs = now)?.let { dao.upsertTicker(it) } }
        }

        companion object {
            private const val TAG = "RateRepo"
            const val POLL_INTERVAL_MS = 60_000L
            const val RETRY_INTERVAL_MS = 5_000L
        }
    }

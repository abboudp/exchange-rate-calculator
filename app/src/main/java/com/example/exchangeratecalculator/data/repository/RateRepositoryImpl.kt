package com.example.exchangeratecalculator.data.repository

import android.util.Log
import com.example.exchangeratecalculator.core.coroutine.DispatcherProvider
import com.example.exchangeratecalculator.data.local.RateTickerDao
import com.example.exchangeratecalculator.data.local.toDomain
import com.example.exchangeratecalculator.data.remote.DolarApi
import com.example.exchangeratecalculator.data.remote.toEntity
import com.example.exchangeratecalculator.di.ApplicationScope
import com.example.exchangeratecalculator.domain.model.RateResource
import com.example.exchangeratecalculator.domain.repository.CurrencyRepository
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
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateRepositoryImpl
    @Inject
    constructor(
        private val api: DolarApi,
        private val dao: RateTickerDao,
        private val dispatchers: DispatcherProvider,
        private val currencyRepository: CurrencyRepository,
        @param:ApplicationScope private val appScope: CoroutineScope,
    ) : RateRepository {
        init {
            appScope.launch(dispatchers.io) {
                fetchAllTickers()
            }
        }

        override fun observeRateTicker(fiatCode: String): Flow<RateResource> =
            channelFlow {
                val book = "usdc_${fiatCode.lowercase()}"
                val pollFailureFlow = MutableStateFlow<RateResource.UnavailableReason?>(null)

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
                        val failures = fetchAllTickers()
                        val myFailure = failures[fiatCode.uppercase()]
                        pollFailureFlow.value = myFailure
                        delay(
                            if (myFailure == RateResource.UnavailableReason.OFFLINE) {
                                RETRY_INTERVAL_MS
                            } else {
                                POLL_INTERVAL_MS
                            },
                        )
                    }
                }

                combine(dao.observeTicker(book), pollFailureFlow) { entity, failureReason ->
                    entity to failureReason
                }.collect { (entity, failureReason) ->
                    when {
                        entity != null -> send(RateResource.Available(entity.toDomain()))
                        failureReason != null -> send(RateResource.Unavailable(failureReason))
                    }
                }
            }.flowOn(dispatchers.io)

        private suspend fun fetchAllTickers(): Map<String, RateResource.UnavailableReason> {
            val currencies =
                currencyRepository.observeAvailableCurrencies()
                    .first()
                    .filter { !it.isBase }
            val codes = currencies.joinToString(",") { it.code }
            val now = System.currentTimeMillis()

            return try {
                val dtos = api.getTickers(codes)
                dtos.forEach { dto -> dto.toEntity(fetchedAtEpochMs = now)?.let { dao.upsertTicker(it) } }
                emptyMap()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Batch fetch failed, trying individually", e)
                val failures = mutableMapOf<String, RateResource.UnavailableReason>()
                currencies.forEach { currency ->
                    try {
                        val dtos = api.getTickers(currency.code)
                        dtos.forEach { dto -> dto.toEntity(fetchedAtEpochMs = now)?.let { dao.upsertTicker(it) } }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (ioe: IOException) {
                        failures[currency.code] = RateResource.UnavailableReason.OFFLINE
                    } catch (e: Exception) {
                        failures[currency.code] = RateResource.UnavailableReason.RATE_UNAVAILABLE
                    }
                }
                failures
            }
        }

        companion object {
            private const val TAG = "RateRepo"
            const val POLL_INTERVAL_MS = 60_000L
            const val RETRY_INTERVAL_MS = 5_000L
        }
    }

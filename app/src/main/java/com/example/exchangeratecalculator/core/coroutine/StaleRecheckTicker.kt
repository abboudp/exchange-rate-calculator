package com.example.exchangeratecalculator.core.coroutine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Emits Unit on a fixed cadence so consumers can re-derive wall-clock-based
 * state (e.g. "is this cached value past its TTL?") without polling on their
 * own. Injectable so tests can swap in a single-emission or empty flow.
 */
interface StaleRecheckTicker {
    fun ticks(): Flow<Unit>
}

class DefaultStaleRecheckTicker
    @Inject
    constructor() : StaleRecheckTicker {
        override fun ticks(): Flow<Unit> =
            flow {
                while (true) {
                    emit(Unit)
                    delay(INTERVAL_MS)
                }
            }

        companion object {
            const val INTERVAL_MS = 10_000L
        }
    }

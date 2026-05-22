package com.example.exchangeratecalculator.domain.model

import java.math.BigDecimal

data class RateTicker(
    val book: String,
    val ask: BigDecimal,
    val bid: BigDecimal,
    val fetchedAtEpochMs: Long,
)

const val STALE_THRESHOLD_MS = 2 * 60 * 1000L

val RateTicker.isStale: Boolean
    get() = System.currentTimeMillis() - fetchedAtEpochMs > STALE_THRESHOLD_MS

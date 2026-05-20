package com.example.exchangeratecalculator.domain.model

import java.math.BigDecimal

data class RateTicker(
    val book: String,
    val ask: BigDecimal,
    val bid: BigDecimal,
    val fetchedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
)

val RateTicker.isStale: Boolean get() = System.currentTimeMillis() > expiresAtEpochMs

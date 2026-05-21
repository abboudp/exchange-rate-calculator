package com.example.exchangeratecalculator.data.remote

import com.example.exchangeratecalculator.data.local.RateTickerEntity
import com.example.exchangeratecalculator.domain.model.RateTicker
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class RateTickerDto(
    val ask: String,
    val bid: String,
    val book: String,
    val date: String,
)

fun RateTickerDto.toDomain(fetchedAtEpochMs: Long, ttlMs: Long): RateTicker =
    RateTicker(
        book = book,
        ask = BigDecimal(ask),
        bid = BigDecimal(bid),
        fetchedAtEpochMs = fetchedAtEpochMs,
        expiresAtEpochMs = fetchedAtEpochMs + ttlMs,
    )

fun RateTickerDto.toEntity(fetchedAtEpochMs: Long, expiresAtEpochMs: Long): RateTickerEntity =
    RateTickerEntity(
        book = book,
        ask = ask,
        bid = bid,
        fetchedAtEpochMs = fetchedAtEpochMs,
        expiresAtEpochMs = expiresAtEpochMs,
    )

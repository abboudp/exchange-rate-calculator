package com.example.exchangeratecalculator.data.remote

import com.example.exchangeratecalculator.data.local.RateTickerEntity
import kotlinx.serialization.Serializable

@Serializable
data class RateTickerDto(
    val ask: String,
    val bid: String,
    val book: String,
    val date: String,
)

fun RateTickerDto.toEntity(fetchedAtEpochMs: Long): RateTickerEntity =
    RateTickerEntity(
        book = book,
        ask = ask,
        bid = bid,
        fetchedAtEpochMs = fetchedAtEpochMs,
    )

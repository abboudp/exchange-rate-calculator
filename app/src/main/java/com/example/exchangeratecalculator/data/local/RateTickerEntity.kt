package com.example.exchangeratecalculator.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.exchangeratecalculator.domain.model.RateTicker
import java.math.BigDecimal

@Entity(tableName = "rate_tickers")
data class RateTickerEntity(
    @PrimaryKey val book: String,
    val ask: String,
    val bid: String,
    val fetchedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
)

fun RateTickerEntity.toDomain(): RateTicker = RateTicker(
    book = book,
    ask = BigDecimal(ask),
    bid = BigDecimal(bid),
    fetchedAtEpochMs = fetchedAtEpochMs,
    expiresAtEpochMs = expiresAtEpochMs,
)

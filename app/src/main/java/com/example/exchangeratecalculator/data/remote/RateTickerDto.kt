package com.example.exchangeratecalculator.data.remote

import com.example.exchangeratecalculator.data.local.RateTickerEntity
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class RateTickerDto(
    val ask: String,
    val bid: String,
    val book: String,
    val date: String,
)

fun RateTickerDto.toEntity(fetchedAtEpochMs: Long): RateTickerEntity? {
    val askDecimal = ask.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    val bidDecimal = bid.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    return RateTickerEntity(
        book = book,
        ask = askDecimal.toPlainString(),
        bid = bidDecimal.toPlainString(),
        fetchedAtEpochMs = fetchedAtEpochMs,
    )
}

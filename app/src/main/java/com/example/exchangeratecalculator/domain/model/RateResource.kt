package com.example.exchangeratecalculator.domain.model

sealed interface RateResource {
    data object Loading : RateResource

    data class Available(val ticker: RateTicker) : RateResource

    data class Unavailable(val reason: UnavailableReason) : RateResource

    enum class UnavailableReason { OFFLINE, RATE_UNAVAILABLE }
}

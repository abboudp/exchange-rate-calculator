package com.example.exchangeratecalculator.domain.model

sealed interface RateResource {
    data object Loading : RateResource
    data class Fresh(val ticker: RateTicker) : RateResource
    data class Stale(val ticker: RateTicker) : RateResource
    data class Unavailable(val reason: String) : RateResource
}

package com.example.exchangeratecalculator.domain.model

sealed interface RateResource {
    data object Loading : RateResource

    data class Fresh(val ticker: RateTicker) : RateResource

    /**
     * Cached data is still within TTL but the most recent poll attempt failed.
     * Surfaced to the UI as degraded (gray text + error icon) so the user knows
     * the rate may be stale before TTL formally expires.
     */
    data class Degraded(val ticker: RateTicker) : RateResource

    data class Stale(val ticker: RateTicker) : RateResource

    data class Unavailable(val reason: String) : RateResource
}

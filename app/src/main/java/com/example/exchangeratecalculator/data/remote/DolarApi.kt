package com.example.exchangeratecalculator.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface DolarApi {
    @GET("v1/tickers")
    suspend fun getTickers(
        @Query("currencies") currencies: String,
    ): List<RateTickerDto>

    companion object {
        const val BASE_URL = "https://api.dolarapp.dev/"
    }
}

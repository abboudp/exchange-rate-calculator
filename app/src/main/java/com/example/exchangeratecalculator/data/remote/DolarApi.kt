package com.example.exchangeratecalculator.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface DolarApi {
    @GET("v1/tickers")
    suspend fun getTickers(
        @Query("currencies") currencies: String,
    ): List<RateTickerDto>

    @GET("v1/tickers-currencies")
    suspend fun getAvailableCurrencies(): List<String>

    companion object {
        const val BASE_URL = "https://api.dolarapp.dev/"
    }
}

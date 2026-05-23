package com.example.exchangeratecalculator

import android.app.Application
import com.example.exchangeratecalculator.domain.repository.RateRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ExchangeApp : Application() {
    @Inject lateinit var rateRepository: RateRepository
}

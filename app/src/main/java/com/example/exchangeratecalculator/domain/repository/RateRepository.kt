package com.example.exchangeratecalculator.domain.repository

import com.example.exchangeratecalculator.domain.model.RateResource
import kotlinx.coroutines.flow.Flow

interface RateRepository {
    fun observeRateTicker(fiatCode: String): Flow<RateResource>
}

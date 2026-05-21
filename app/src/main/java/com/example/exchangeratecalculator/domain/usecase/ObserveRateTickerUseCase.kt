package com.example.exchangeratecalculator.domain.usecase

import com.example.exchangeratecalculator.domain.model.RateResource
import com.example.exchangeratecalculator.domain.repository.RateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRateTickerUseCase
    @Inject
    constructor(
        private val repository: RateRepository,
    ) {
        operator fun invoke(fiatCode: String): Flow<RateResource> = repository.observeRateTicker(fiatCode)
    }

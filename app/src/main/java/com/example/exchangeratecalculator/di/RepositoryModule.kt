package com.example.exchangeratecalculator.di

import com.example.exchangeratecalculator.data.repository.CurrencyRepositoryImpl
import com.example.exchangeratecalculator.data.repository.RateRepositoryImpl
import com.example.exchangeratecalculator.data.repository.SettingsRepositoryImpl
import com.example.exchangeratecalculator.domain.repository.CurrencyRepository
import com.example.exchangeratecalculator.domain.repository.RateRepository
import com.example.exchangeratecalculator.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCurrencyRepository(impl: CurrencyRepositoryImpl): CurrencyRepository

    @Binds
    @Singleton
    abstract fun bindRateRepository(impl: RateRepositoryImpl): RateRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}

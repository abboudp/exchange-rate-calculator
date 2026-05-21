package com.example.exchangeratecalculator.di

import com.example.exchangeratecalculator.core.coroutine.DefaultDispatcherProvider
import com.example.exchangeratecalculator.core.coroutine.DispatcherProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider
}

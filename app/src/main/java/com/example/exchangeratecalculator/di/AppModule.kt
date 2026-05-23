package com.example.exchangeratecalculator.di

import com.example.exchangeratecalculator.core.coroutine.DefaultDispatcherProvider
import com.example.exchangeratecalculator.core.coroutine.DefaultStaleRecheckTicker
import com.example.exchangeratecalculator.core.coroutine.DispatcherProvider
import com.example.exchangeratecalculator.core.coroutine.StaleRecheckTicker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindStaleRecheckTicker(impl: DefaultStaleRecheckTicker): StaleRecheckTicker

    companion object {
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

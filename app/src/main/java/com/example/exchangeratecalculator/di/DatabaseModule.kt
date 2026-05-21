package com.example.exchangeratecalculator.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.example.exchangeratecalculator.data.local.ExchangeDatabase
import com.example.exchangeratecalculator.data.local.RateTickerDao
import com.example.exchangeratecalculator.data.local.settingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideExchangeDatabase(
        @ApplicationContext context: Context,
    ): ExchangeDatabase =
        Room.databaseBuilder(context, ExchangeDatabase::class.java, "exchange.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideRateTickerDao(database: ExchangeDatabase): RateTickerDao = database.rateTickerDao()

    @Provides
    @Singleton
    fun provideSettingsPreferencesStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.settingsDataStore
}

package com.example.exchangeratecalculator.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RateTickerEntity::class], version = 2, exportSchema = false)
abstract class ExchangeDatabase : RoomDatabase() {
    abstract fun rateTickerDao(): RateTickerDao
}

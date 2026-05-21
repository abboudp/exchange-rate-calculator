package com.example.exchangeratecalculator.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RateTickerDao {
    @Query("SELECT * FROM rate_tickers WHERE book = :book")
    fun observeTicker(book: String): Flow<RateTickerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTicker(entity: RateTickerEntity)
}

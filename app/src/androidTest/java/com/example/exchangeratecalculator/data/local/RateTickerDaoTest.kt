package com.example.exchangeratecalculator.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RateTickerDaoTest {
    private lateinit var database: ExchangeDatabase
    private lateinit var dao: RateTickerDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, ExchangeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.rateTickerDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun observeTicker_returnsNullWhenEmpty() =
        runBlocking {
            assertNull(dao.observeTicker("usdc_mxn").first())
        }

    @Test
    fun upsertTicker_thenObserve_emitsInserted() =
        runBlocking {
            val entity = sampleEntity()

            dao.upsertTicker(entity)

            assertEquals(entity, dao.observeTicker(entity.book).first())
        }

    @Test
    fun upsertTicker_replacesOnConflict() =
        runBlocking {
            val original = sampleEntity(ask = "18.0000")
            val updated = sampleEntity(ask = "19.0000")

            dao.upsertTicker(original)
            dao.upsertTicker(updated)

            assertEquals(updated, dao.observeTicker(original.book).first())
        }

    private fun sampleEntity(
        book: String = "usdc_mxn",
        ask: String = "18.4105",
        bid: String = "18.4069",
        fetchedAtEpochMs: Long = 1_000L,
    ) = RateTickerEntity(
        book = book,
        ask = ask,
        bid = bid,
        fetchedAtEpochMs = fetchedAtEpochMs,
    )
}

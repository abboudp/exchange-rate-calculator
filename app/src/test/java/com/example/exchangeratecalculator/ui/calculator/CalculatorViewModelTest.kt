package com.example.exchangeratecalculator.ui.calculator

import com.example.exchangeratecalculator.core.coroutine.StaleRecheckTicker
import com.example.exchangeratecalculator.data.remote.FallbackCurrenciesProvider
import com.example.exchangeratecalculator.domain.model.AppSettings
import com.example.exchangeratecalculator.domain.model.Currency
import com.example.exchangeratecalculator.domain.model.RateResource
import com.example.exchangeratecalculator.domain.model.RateTicker
import com.example.exchangeratecalculator.domain.repository.CurrencyRepository
import com.example.exchangeratecalculator.domain.repository.RateRepository
import com.example.exchangeratecalculator.domain.repository.SettingsRepository
import com.example.exchangeratecalculator.domain.usecase.ConvertCurrencyUseCase
import com.example.exchangeratecalculator.domain.usecase.GetAvailableCurrenciesUseCase
import com.example.exchangeratecalculator.domain.usecase.ObserveRateTickerUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_matchesDefaults() =
        runTest {
            val viewModel = createViewModel()

            val state = viewModel.uiState.value
            assertEquals("USDC", state.topCurrencyCode)
            assertEquals("MXN", state.bottomCurrencyCode)
            assertEquals("", state.topAmountText)
            assertEquals("", state.bottomAmountText)
            assertEquals(AmountField.TOP, state.activeField)
            assertFalse(state.canBackspace)
            assertTrue(state.canInsertDecimal)
        }

    @Test
    fun onDigitPressed_updatesActiveFieldAndEnablesBackspace() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onDigitPressed('9')

            val state = viewModel.uiState.value
            assertEquals("9", state.topAmountText)
            assertTrue(state.canBackspace)
        }

    @Test
    fun onDigitPressed_thenBackspace_clearsBothFields() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onDigitPressed('9')
            viewModel.onBackspacePressed()

            val state = viewModel.uiState.value
            assertEquals("", state.topAmountText)
            assertEquals("", state.bottomAmountText)
            assertFalse(state.canBackspace)
        }

    @Test
    fun typingPastUsdcNotionalCap_clampsActiveUsdc() =
        runTest {
            // Active USDC field — keep tapping digits past the cap.
            val viewModel = createViewModel(initialRate = freshTicker(bid = "18.40", ask = "18.41"))

            // 10 nines (10,000,000,000) — over the 999,999,999.99 cap.
            repeat(10) { viewModel.onDigitPressed('9') }

            val state = viewModel.uiState.value
            assertEquals("999999999.99", state.topAmountText)
        }

    @Test
    fun typingPastUsdcNotionalCap_clampsActiveFiatViaUsdcEquivalent() =
        runTest {
            // Active fiat field (post-swap). MXN ask = 18.41.
            // Fiat input is clamped such that USDC equivalent ≤ 999,999,999.99.
            val settings = FakeSettingsRepository(AppSettings(selectedFiatCode = "MXN", isSwapped = true))
            val viewModel =
                createViewModel(
                    settings = settings,
                    initialRate = freshTicker(bid = "18.40", ask = "18.41"),
                )

            // Type "9999999999999" (13 nines) — far above the cap-equivalent
            // MXN of 999_999_999.99 × 18.41 = 18,409,999,999.61.
            repeat(13) { viewModel.onDigitPressed('9') }

            val state = viewModel.uiState.value
            // Cap-equivalent MXN: 999999999.99 * 18.41 = 18409999999.8159
            // setScale(2, DOWN) → 18409999999.81
            assertEquals("18409999999.81", state.topAmountText)
        }

    @Test
    fun onDigitPressed_withFreshRate_writesConversionToInactive() =
        runTest {
            val viewModel = createViewModel(initialRate = freshTicker(bid = "18.40", ask = "18.41"))

            viewModel.onDigitPressed('1')

            val state = viewModel.uiState.value
            assertEquals("1", state.topAmountText)
            // USDC -> MXN uses bid (18.40); 1 * 18.40 = 18.40
            assertEquals("18.40", state.bottomAmountText)
        }

    @Test
    fun onDecimalPressed_doubleTap_isBlockedAndDisablesInsertDecimal() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onDecimalPressed()
            viewModel.onDecimalPressed()

            val state = viewModel.uiState.value
            // Empty + decimal -> "0." per InputNormalizer
            assertEquals("0.", state.topAmountText)
            assertFalse(state.canInsertDecimal)
        }

    @Test
    fun onSwapPressed_flipsCurrencies_andRecomputesBottomUsingCorrectRateDirection() =
        runTest {
            val settingsRepo = FakeSettingsRepository(AppSettings(selectedFiatCode = "MXN", isSwapped = false))
            val viewModel = createViewModel(settings = settingsRepo)
            // top=USDC "1", bottom=MXN "18.40" (USDC→MXN uses bid=18.40)
            viewModel.onDigitPressed('1')
            val before = viewModel.uiState.value
            assertEquals("18.40", before.bottomAmountText)

            viewModel.onSwapPressed()

            val after = viewModel.uiState.value
            assertEquals(before.bottomCurrencyCode, after.topCurrencyCode)
            assertEquals(before.topCurrencyCode, after.bottomCurrencyCode)
            // Top inherits the old bottom value verbatim.
            assertEquals(before.bottomAmountText, after.topAmountText)
            // Bottom is recomputed from the new top using ask=18.41 (buying USDC):
            // 18.40 / 18.41 = 0.99945... → HALF_UP at 2 dp → 1.00 USDC.
            assertEquals("1.00", after.bottomAmountText)
            assertTrue(after.isSwapped)
            assertTrue(settingsRepo.lastIsSwapped == true)
        }

    @Test
    fun onFiatRowTapped_showsPicker() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onFiatRowTapped()

            assertTrue(viewModel.uiState.value.pickerState.isVisible)
        }

    @Test
    fun onCurrencySelected_dismissesPickerAndRestartsRateObservation() =
        runTest {
            val rateRepo = FakeRateRepository()
            rateRepo.emit("MXN", freshTicker(book = "usdc_mxn", bid = "18.40", ask = "18.41"))
            rateRepo.emit("ARS", freshTicker(book = "usdc_ars", bid = "1500.00", ask = "1510.00"))
            val viewModel = createViewModel(rateRepository = rateRepo)
            viewModel.onFiatRowTapped()

            viewModel.onCurrencySelected("ARS")
            viewModel.onDigitPressed('1')

            val state = viewModel.uiState.value
            assertFalse(state.pickerState.isVisible)
            assertEquals("ARS", state.bottomCurrencyCode)
            // 1 USDC * 1500 ARS bid = 1500.00; state holds raw, comma grouping is a display concern.
            assertEquals("1500.00", state.bottomAmountText)
        }

    @Test
    fun expiredTicker_mapsToAvailableNotFresh() =
        runTest {
            // A ticker fetched long ago (fetchedAtEpochMs = 0) is past the
            // stale threshold, so the VM marks it isFresh = false even though
            // the repo just emits Available.
            val rateRepo = FakeRateRepository()
            rateRepo.emit(
                "MXN",
                RateResource.Available(ticker(book = "usdc_mxn", fetchedAtEpochMs = 0L)),
            )
            val viewModel = createViewModel(rateRepository = rateRepo)

            val display = viewModel.uiState.value.rateDisplayState
            assertTrue("expected Available but got $display", display is RateDisplayState.Available)
            assertFalse((display as RateDisplayState.Available).isFresh)
        }

    @Test
    fun unavailableRateResource_mapsToUnavailable() =
        runTest {
            val rateRepo = FakeRateRepository()
            rateRepo.emit("MXN", RateResource.Unavailable("offline"))
            val viewModel = createViewModel(rateRepository = rateRepo)

            assertEquals(RateDisplayState.Unavailable, viewModel.uiState.value.rateDisplayState)
        }

    @Test
    fun clearingActiveField_clearsInactiveField() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onDigitPressed('5')
            viewModel.onBackspacePressed()

            assertEquals("", viewModel.uiState.value.topAmountText)
            assertEquals("", viewModel.uiState.value.bottomAmountText)
        }

    // --- Helpers --------------------------------------------------------------

    private fun createViewModel(
        settings: FakeSettingsRepository = FakeSettingsRepository(AppSettings()),
        rateRepository: FakeRateRepository = FakeRateRepository().also { it.emit("MXN", freshTicker()) },
        currencies: List<Currency> = FallbackCurrenciesProvider.currencies,
        initialRate: RateResource? = null,
    ): CalculatorViewModel {
        if (initialRate != null) rateRepository.emit("MXN", initialRate)
        val currencyRepository = FakeCurrencyRepository(currencies)
        return CalculatorViewModel(
            observeRateTicker = ObserveRateTickerUseCase(rateRepository),
            getAvailableCurrencies = GetAvailableCurrenciesUseCase(currencyRepository),
            settingsRepository = settings,
            convertCurrency = ConvertCurrencyUseCase(),
            staleRecheckTicker = SingleTickStaleRecheckTicker,
        )
    }

    // Single-emission ticker so combine() fires once with the latest rate and
    // then completes — keeps runTest from waiting on a forever-ticking flow.
    private object SingleTickStaleRecheckTicker : StaleRecheckTicker {
        override fun ticks(): Flow<Unit> = flowOf(Unit)
    }

    private fun ticker(
        book: String = "usdc_mxn",
        ask: String = "18.41",
        bid: String = "18.40",
        fetchedAtEpochMs: Long = System.currentTimeMillis(),
    ) = RateTicker(
        book = book,
        ask = BigDecimal(ask),
        bid = BigDecimal(bid),
        fetchedAtEpochMs = fetchedAtEpochMs,
    )

    private fun freshTicker(
        book: String = "usdc_mxn",
        ask: String = "18.41",
        bid: String = "18.40",
    ): RateResource = RateResource.Available(ticker(book, ask, bid))

    private class FakeSettingsRepository(initial: AppSettings) : SettingsRepository {
        private val state = MutableStateFlow(initial)
        var lastIsSwapped: Boolean? = null
            private set

        override fun observeSettings(): Flow<AppSettings> = state

        override suspend fun updateSelectedCurrency(code: String) {
            state.update { it.copy(selectedFiatCode = code) }
        }

        override suspend fun updateSwapState(isSwapped: Boolean) {
            lastIsSwapped = isSwapped
            state.update { it.copy(isSwapped = isSwapped) }
        }
    }

    private class FakeRateRepository : RateRepository {
        private val flowsByFiat = mutableMapOf<String, MutableStateFlow<RateResource>>()

        override fun observeRateTicker(fiatCode: String): Flow<RateResource> =
            flowsByFiat.getOrPut(fiatCode) { MutableStateFlow(RateResource.Loading) }

        fun emit(
            fiatCode: String,
            resource: RateResource,
        ) {
            flowsByFiat.getOrPut(fiatCode) { MutableStateFlow(RateResource.Loading) }.value = resource
        }
    }

    private class FakeCurrencyRepository(private val currencies: List<Currency>) : CurrencyRepository {
        override fun observeAvailableCurrencies(): Flow<List<Currency>> = flowOf(currencies)
    }
}

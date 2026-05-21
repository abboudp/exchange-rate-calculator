package com.example.exchangeratecalculator.presentation.calculator

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
    fun onSwapPressed_flipsCurrenciesAndAmountsAndPersists() =
        runTest {
            val settingsRepo = FakeSettingsRepository(AppSettings(selectedFiatCode = "MXN", isSwapped = false))
            val viewModel = createViewModel(settings = settingsRepo)
            viewModel.onDigitPressed('5')
            val before = viewModel.uiState.value

            viewModel.onSwapPressed()

            val after = viewModel.uiState.value
            assertEquals(before.bottomCurrencyCode, after.topCurrencyCode)
            assertEquals(before.topCurrencyCode, after.bottomCurrencyCode)
            assertEquals(before.bottomAmountText, after.topAmountText)
            assertEquals(before.topAmountText, after.bottomAmountText)
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
            // 1 USDC * 1500 ARS bid = 1,500.00
            assertEquals("1,500.00", state.bottomAmountText)
        }

    @Test
    fun staleRateResource_mapsToAvailableNotFresh() =
        runTest {
            val rateRepo = FakeRateRepository()
            rateRepo.emit("MXN", RateResource.Stale(ticker(book = "usdc_mxn")))
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
        )
    }

    private fun ticker(
        book: String = "usdc_mxn",
        ask: String = "18.41",
        bid: String = "18.40",
    ) = RateTicker(
        book = book,
        ask = BigDecimal(ask),
        bid = BigDecimal(bid),
        fetchedAtEpochMs = 0L,
        expiresAtEpochMs = Long.MAX_VALUE,
    )

    private fun freshTicker(
        book: String = "usdc_mxn",
        ask: String = "18.41",
        bid: String = "18.40",
    ): RateResource = RateResource.Fresh(ticker(book, ask, bid))

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
        override suspend fun getAvailableCurrencies(): List<Currency> = currencies
    }
}

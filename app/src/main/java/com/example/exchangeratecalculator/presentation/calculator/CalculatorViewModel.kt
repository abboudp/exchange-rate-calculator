package com.example.exchangeratecalculator.presentation.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeratecalculator.domain.model.AppSettings
import com.example.exchangeratecalculator.domain.model.RateResource
import com.example.exchangeratecalculator.domain.model.RateTicker
import com.example.exchangeratecalculator.domain.model.USDC_CURRENCY
import com.example.exchangeratecalculator.domain.repository.SettingsRepository
import com.example.exchangeratecalculator.domain.usecase.ConvertCurrencyUseCase
import com.example.exchangeratecalculator.domain.usecase.GetAvailableCurrenciesUseCase
import com.example.exchangeratecalculator.domain.usecase.ObserveRateTickerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

private val MAX_USDC_OUTPUT = BigDecimal(InputNormalizer.MAX_VALUE)

@HiltViewModel
class CalculatorViewModel
    @Inject
    constructor(
        private val observeRateTicker: ObserveRateTickerUseCase,
        private val getAvailableCurrencies: GetAvailableCurrenciesUseCase,
        private val settingsRepository: SettingsRepository,
        private val convertCurrency: ConvertCurrencyUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CalculatorUiState())
        val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

        private var rateObservationJob: Job? = null
        private var latestRateResource: RateResource = RateResource.Loading
        private var lastObservedFiatCode: String? = null

        init {
            viewModelScope.launch {
                val currencies = getAvailableCurrencies()
                _uiState.update { it.copy(pickerState = it.pickerState.copy(currencies = currencies)) }
            }
            viewModelScope.launch {
                settingsRepository.observeSettings().collect { settings ->
                    applySettings(settings)
                    if (settings.selectedFiatCode != lastObservedFiatCode) {
                        observeRateForCurrency(settings.selectedFiatCode)
                        lastObservedFiatCode = settings.selectedFiatCode
                    }
                }
            }
        }

        fun onDigitPressed(digit: Char) = transformActiveText { InputNormalizer.onDigit(it, digit) }

        fun onDecimalPressed() = transformActiveText { InputNormalizer.onDecimal(it) }

        fun onBackspacePressed() = transformActiveText { InputNormalizer.onBackspace(it) }

        fun onSwapPressed() {
            val newIsSwapped = !_uiState.value.isSwapped
            _uiState.update { state ->
                state.copy(
                    topCurrencyCode = state.bottomCurrencyCode,
                    bottomCurrencyCode = state.topCurrencyCode,
                    topAmountText = state.bottomAmountText,
                    bottomAmountText = state.topAmountText,
                    isSwapped = newIsSwapped,
                ).withKeypadFlags()
            }
            viewModelScope.launch {
                settingsRepository.updateSwapState(newIsSwapped)
            }
        }

        fun onFiatRowTapped() {
            _uiState.update { it.copy(pickerState = it.pickerState.copy(isVisible = true)) }
        }

        fun onPickerDismissed() {
            _uiState.update { it.copy(pickerState = it.pickerState.copy(isVisible = false)) }
        }

        fun onCurrencySelected(code: String) {
            _uiState.update { state ->
                // Carried-over top values (from a prior swap) can exceed the
                // input cap. Without normalizing, the bottom USDC would clamp
                // and the two amounts no longer agree mathematically.
                val normalizedTop = clampToInputCap(state.topAmountText)
                state.copy(
                    topAmountText = normalizedTop,
                    pickerState = state.pickerState.copy(isVisible = false),
                )
            }
            viewModelScope.launch {
                settingsRepository.updateSelectedCurrency(code)
            }
        }

        private fun clampToInputCap(text: String): String {
            val value = text.toBigDecimalOrNull() ?: return text
            return if (value > MAX_USDC_OUTPUT) InputNormalizer.MAX_VALUE else text
        }

        fun onTopFieldFocused() = focusField(AmountField.TOP)

        fun onBottomFieldFocused() = focusField(AmountField.BOTTOM)

        private fun focusField(field: AmountField) {
            if (_uiState.value.activeField == field) return
            val ticker = currentTicker()
            _uiState.update { state ->
                state.copy(activeField = field).withRecomputedInactive(ticker).withKeypadFlags()
            }
        }

        private fun transformActiveText(transform: (String) -> String) {
            val ticker = currentTicker()
            _uiState.update { state ->
                val newActive = transform(state.activeText())
                state.withActiveText(newActive).withRecomputedInactive(ticker).withKeypadFlags()
            }
        }

        private fun applySettings(settings: AppSettings) {
            _uiState.update { state ->
                val topCode = if (settings.isSwapped) settings.selectedFiatCode else USDC_CURRENCY.code
                val bottomCode = if (settings.isSwapped) USDC_CURRENCY.code else settings.selectedFiatCode
                state.copy(
                    topCurrencyCode = topCode,
                    bottomCurrencyCode = bottomCode,
                    isSwapped = settings.isSwapped,
                    pickerState = state.pickerState.copy(selectedCode = settings.selectedFiatCode),
                )
            }
        }

        private fun observeRateForCurrency(fiatCode: String) {
            rateObservationJob?.cancel()
            rateObservationJob =
                viewModelScope.launch {
                    observeRateTicker(fiatCode).collect { resource ->
                        latestRateResource = resource
                        val ticker = currentTicker()
                        _uiState.update { state ->
                            state.copy(rateDisplayState = resource.toDisplayState(state.pickerState.selectedCode))
                                .withRecomputedInactive(ticker)
                        }
                    }
                }
        }

        private fun RateResource.toDisplayState(fiatCode: String): RateDisplayState =
            when (this) {
                is RateResource.Loading -> RateDisplayState.Loading
                is RateResource.Fresh -> RateDisplayState.Available(formatRate(ticker, fiatCode), isFresh = true)
                is RateResource.Degraded -> RateDisplayState.Available(formatRate(ticker, fiatCode), isFresh = false)
                is RateResource.Stale -> RateDisplayState.Available(formatRate(ticker, fiatCode), isFresh = false)
                is RateResource.Unavailable -> RateDisplayState.Unavailable
            }

        private fun formatRate(
            ticker: RateTicker,
            fiatCode: String,
        ): String = MoneyFormatter.formatRate(ticker.bid, USDC_CURRENCY.code, fiatCode)

        private fun CalculatorUiState.withRecomputedInactive(ticker: RateTicker?): CalculatorUiState {
            val activeText = activeText()
            if (activeText.isBlank() || ticker == null) return withInactiveText("")
            val fromCode = activeFieldCurrency()
            val toCode = inactiveFieldCurrency()
            val quote = convertCurrency(activeText, fromCode, toCode, ticker)
            // Match the same cap that InputNormalizer applies to direct input:
            // USDC output cannot exceed 999,999.99 regardless of fiat input size.
            val clamped =
                if (toCode == USDC_CURRENCY.code && quote.outputAmount > MAX_USDC_OUTPUT) {
                    MAX_USDC_OUTPUT
                } else {
                    quote.outputAmount
                }
            return withInactiveText(clamped.toPlainString())
        }

        private fun CalculatorUiState.withKeypadFlags(): CalculatorUiState {
            val activeText = activeText()
            return copy(
                canBackspace = InputNormalizer.canBackspace(activeText),
                canInsertDecimal = InputNormalizer.canInsertDecimal(activeText),
            )
        }

        private fun CalculatorUiState.activeText(): String = if (activeField == AmountField.TOP) topAmountText else bottomAmountText

        private fun CalculatorUiState.withActiveText(text: String): CalculatorUiState =
            if (activeField == AmountField.TOP) copy(topAmountText = text) else copy(bottomAmountText = text)

        private fun CalculatorUiState.withInactiveText(text: String): CalculatorUiState =
            if (activeField == AmountField.TOP) copy(bottomAmountText = text) else copy(topAmountText = text)

        private fun CalculatorUiState.activeFieldCurrency(): String =
            if (activeField == AmountField.TOP) topCurrencyCode else bottomCurrencyCode

        private fun CalculatorUiState.inactiveFieldCurrency(): String =
            if (activeField == AmountField.TOP) bottomCurrencyCode else topCurrencyCode

        private fun currentTicker(): RateTicker? =
            when (val resource = latestRateResource) {
                is RateResource.Fresh -> resource.ticker
                is RateResource.Degraded -> resource.ticker
                is RateResource.Stale -> resource.ticker
                else -> null
            }
    }

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
import javax.inject.Inject

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

        fun onDigitPressed(digit: Char) {
            setActiveFieldText(InputNormalizer.onDigit(activeFieldText(), digit))
            refreshKeypadFlags()
            recomputeInactiveField()
        }

        fun onDecimalPressed() {
            setActiveFieldText(InputNormalizer.onDecimal(activeFieldText()))
            refreshKeypadFlags()
            recomputeInactiveField()
        }

        fun onBackspacePressed() {
            setActiveFieldText(InputNormalizer.onBackspace(activeFieldText()))
            refreshKeypadFlags()
            recomputeInactiveField()
        }

        fun onSwapPressed() {
            _uiState.update { state ->
                state.copy(
                    topCurrencyCode = state.bottomCurrencyCode,
                    bottomCurrencyCode = state.topCurrencyCode,
                    topAmountText = state.bottomAmountText,
                    bottomAmountText = state.topAmountText,
                    isSwapped = !state.isSwapped,
                )
            }
            refreshKeypadFlags()
            viewModelScope.launch {
                settingsRepository.updateSwapState(_uiState.value.isSwapped)
            }
        }

        fun onFiatRowTapped() {
            _uiState.update { it.copy(pickerState = it.pickerState.copy(isVisible = true)) }
        }

        fun onPickerDismissed() {
            _uiState.update { it.copy(pickerState = it.pickerState.copy(isVisible = false)) }
        }

        fun onCurrencySelected(code: String) {
            _uiState.update { it.copy(pickerState = it.pickerState.copy(isVisible = false)) }
            viewModelScope.launch {
                settingsRepository.updateSelectedCurrency(code)
            }
        }

        fun onTopFieldFocused() {
            if (_uiState.value.activeField == AmountField.TOP) return
            _uiState.update { it.copy(activeField = AmountField.TOP) }
            refreshKeypadFlags()
            recomputeInactiveField()
        }

        fun onBottomFieldFocused() {
            if (_uiState.value.activeField == AmountField.BOTTOM) return
            _uiState.update { it.copy(activeField = AmountField.BOTTOM) }
            refreshKeypadFlags()
            recomputeInactiveField()
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
                        _uiState.update { it.copy(rateDisplayState = resource.toDisplayState()) }
                        recomputeInactiveField()
                    }
                }
        }

        private fun RateResource.toDisplayState(): RateDisplayState =
            when (this) {
                is RateResource.Loading -> RateDisplayState.Loading
                is RateResource.Fresh ->
                    RateDisplayState.Available(
                        text = formatRateText(ticker),
                        isFresh = true,
                    )
                is RateResource.Stale ->
                    RateDisplayState.Available(
                        text = formatRateText(ticker),
                        isFresh = false,
                    )
                is RateResource.Unavailable -> RateDisplayState.Unavailable
            }

        private fun formatRateText(ticker: RateTicker): String {
            val fiatCode = _uiState.value.pickerState.selectedCode
            return MoneyFormatter.formatRate(ticker.bid, USDC_CURRENCY.code, fiatCode)
        }

        private fun recomputeInactiveField() {
            val activeText = activeFieldText()
            if (activeText.isBlank()) {
                setInactiveFieldText("")
                return
            }
            val ticker = currentTicker()
            if (ticker == null) {
                setInactiveFieldText("")
                return
            }
            val quote = convertCurrency(activeText, activeFieldCurrency(), inactiveFieldCurrency(), ticker)
            setInactiveFieldText(MoneyFormatter.formatAmount(quote.outputAmount))
        }

        private fun refreshKeypadFlags() {
            val text = activeFieldText()
            _uiState.update {
                it.copy(
                    canBackspace = InputNormalizer.canBackspace(text),
                    canInsertDecimal = InputNormalizer.canInsertDecimal(text),
                )
            }
        }

        private fun activeFieldText(): String {
            val state = _uiState.value
            return if (state.activeField == AmountField.TOP) state.topAmountText else state.bottomAmountText
        }

        private fun setActiveFieldText(text: String) {
            _uiState.update {
                if (it.activeField == AmountField.TOP) {
                    it.copy(topAmountText = text)
                } else {
                    it.copy(bottomAmountText = text)
                }
            }
        }

        private fun setInactiveFieldText(text: String) {
            _uiState.update {
                if (it.activeField == AmountField.TOP) {
                    it.copy(bottomAmountText = text)
                } else {
                    it.copy(topAmountText = text)
                }
            }
        }

        private fun activeFieldCurrency(): String {
            val state = _uiState.value
            return if (state.activeField == AmountField.TOP) state.topCurrencyCode else state.bottomCurrencyCode
        }

        private fun inactiveFieldCurrency(): String {
            val state = _uiState.value
            return if (state.activeField == AmountField.TOP) state.bottomCurrencyCode else state.topCurrencyCode
        }

        private fun currentTicker(): RateTicker? =
            when (val resource = latestRateResource) {
                is RateResource.Fresh -> resource.ticker
                is RateResource.Stale -> resource.ticker
                else -> null
            }
    }

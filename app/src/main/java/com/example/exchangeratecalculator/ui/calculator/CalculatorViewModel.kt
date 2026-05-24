package com.example.exchangeratecalculator.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeratecalculator.core.coroutine.StaleRecheckTicker
import com.example.exchangeratecalculator.domain.model.AppSettings
import com.example.exchangeratecalculator.domain.model.RateResource
import com.example.exchangeratecalculator.domain.model.RateTicker
import com.example.exchangeratecalculator.domain.model.USDC_CURRENCY
import com.example.exchangeratecalculator.domain.model.isStale
import com.example.exchangeratecalculator.domain.repository.SettingsRepository
import com.example.exchangeratecalculator.domain.usecase.ConvertCurrencyUseCase
import com.example.exchangeratecalculator.domain.usecase.GetAvailableCurrenciesUseCase
import com.example.exchangeratecalculator.domain.usecase.ObserveRateTickerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * The only cap that exists. Trades are measured by USDC notional; whatever
 * currency is on top, we clamp such that the trade size never exceeds this
 * value in USDC terms. This means the same trade is allowed in either
 * direction (USDC↔fiat) — per-currency caps would create asymmetric blocking.
 */
private const val USDC_NOTIONAL_CAP_STR = "999999999.99"
private val USDC_NOTIONAL_CAP = BigDecimal(USDC_NOTIONAL_CAP_STR)
private const val RATE_SCALE = 8

@HiltViewModel
class CalculatorViewModel
    @Inject
    constructor(
        private val observeRateTicker: ObserveRateTickerUseCase,
        private val getAvailableCurrencies: GetAvailableCurrenciesUseCase,
        private val settingsRepository: SettingsRepository,
        private val convertCurrency: ConvertCurrencyUseCase,
        private val staleRecheckTicker: StaleRecheckTicker,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CalculatorUiState())
        val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

        private var rateObservationJob: Job? = null
        private var latestRateResource: RateResource = RateResource.Loading
        private var lastObservedFiatCode: String? = null

        init {
            viewModelScope.launch {
                getAvailableCurrencies().collect { currencies ->
                    _uiState.update { it.copy(pickerState = it.pickerState.copy(currencies = currencies)) }
                }
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
            val ticker = currentTicker()
            _uiState.update { state ->
                state.copy(
                    topCurrencyCode = state.bottomCurrencyCode,
                    bottomCurrencyCode = state.topCurrencyCode,
                    topAmountText = state.bottomAmountText,
                    bottomAmountText = state.topAmountText,
                    isSwapped = newIsSwapped,
                    swapAnimationKey = state.swapAnimationKey + 1,
                ).withRecomputedInactive(ticker)
                    .withRecomputedRateDisplay()
                    .withKeypadFlags()
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
                state.copy(pickerState = state.pickerState.copy(isVisible = false))
            }
            viewModelScope.launch {
                settingsRepository.updateSelectedCurrency(code)
            }
            // The active value is re-evaluated against the cap when the new
            // currency's rate arrives via observeRateForCurrency.
        }

        private fun transformActiveText(transform: (String) -> String) {
            val ticker = currentTicker()
            _uiState.update { state ->
                val rawActive = transform(state.activeText())
                val cappedActive = clampActiveToUsdcNotional(rawActive, state.activeFieldCurrency(), ticker)
                state.withActiveText(cappedActive).withRecomputedInactive(ticker).withKeypadFlags()
            }
        }

        /**
         * Enforces the single USDC notional cap regardless of which currency
         * is in the active field. USDC-active: clamp value directly. Fiat-active:
         * convert to USDC equivalent (via ask, the rate used for buying USDC),
         * and if over cap, replace the fiat amount with the cap-equivalent fiat
         * (rounded DOWN so the resulting USDC stays within the cap).
         *
         * When the ticker isn't available yet, we can't safely convert, so the
         * value passes through. observeRateForCurrency re-applies the clamp once
         * a ticker emission arrives.
         */
        private fun clampActiveToUsdcNotional(
            activeText: String,
            activeCurrency: String,
            ticker: RateTicker?,
        ): String {
            val value = activeText.toBigDecimalOrNull() ?: return activeText
            if (activeCurrency == USDC_CURRENCY.code) {
                return if (value > USDC_NOTIONAL_CAP) USDC_NOTIONAL_CAP_STR else activeText
            }
            val ask = ticker?.ask ?: return activeText
            if (ask <= BigDecimal.ZERO) return activeText
            val usdcEquivalent = value.divide(ask, RATE_SCALE, RoundingMode.HALF_UP)
            if (usdcEquivalent <= USDC_NOTIONAL_CAP) return activeText
            return USDC_NOTIONAL_CAP.multiply(ask).setScale(2, RoundingMode.DOWN).toPlainString()
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
                    observeRateTicker(fiatCode)
                        .combine(staleRecheckTicker.ticks()) { resource, _ -> resource }
                        .collect { resource ->
                            latestRateResource = resource
                            val ticker = currentTicker()
                            _uiState.update { state ->
                                // Re-clamp the active against the new rate — a
                                // value that was within cap under the old rate
                                // could exceed it under a different fiat or
                                // post-rate-update bid/ask.
                                val cappedActive =
                                    clampActiveToUsdcNotional(state.activeText(), state.activeFieldCurrency(), ticker)
                                state.withActiveText(cappedActive)
                                    .withRecomputedInactive(ticker)
                                    .withRecomputedRateDisplay()
                            }
                        }
                }
        }

        // The rate display depends on `latestRateResource` plus `activeFieldCurrency`
        // (for bid vs ask) plus `pickerState.selectedCode`. Recompute it wherever
        // any of those can change.
        private fun CalculatorUiState.withRecomputedRateDisplay(): CalculatorUiState =
            copy(
                rateDisplayState =
                    latestRateResource.toDisplayState(
                        fiatCode = pickerState.selectedCode,
                        activeCurrency = activeFieldCurrency(),
                    ),
            )

        private fun RateResource.toDisplayState(
            fiatCode: String,
            activeCurrency: String,
        ): RateDisplayState =
            when (this) {
                is RateResource.Loading -> RateDisplayState.Loading
                is RateResource.Available ->
                    RateDisplayState.Available(
                        text = formatRate(ticker, fiatCode, activeCurrency),
                        isFresh = !ticker.isStale,
                    )
                is RateResource.Unavailable -> RateDisplayState.Unavailable
            }

        // The rate text reflects the direction the user is converting from:
        //   USDC → fiat uses bid (selling USDC), fiat → USDC uses ask (buying USDC).
        private fun formatRate(
            ticker: RateTicker,
            fiatCode: String,
            activeCurrency: String,
        ): String {
            val rate = if (activeCurrency == USDC_CURRENCY.code) ticker.bid else ticker.ask
            return MoneyFormatter.formatRate(rate, USDC_CURRENCY.code, fiatCode)
        }

        private fun CalculatorUiState.withRecomputedInactive(ticker: RateTicker?): CalculatorUiState {
            val activeText = activeText()
            if (activeText.isBlank() || ticker == null) return withInactiveText("")
            val fromCode = activeFieldCurrency()
            val toCode = inactiveFieldCurrency()
            val quote = convertCurrency(activeText, fromCode, toCode, ticker)
            // Active is pre-clamped to the USDC notional cap, so the computed
            // inactive is implicitly within the cap too — no extra clamp here.
            return withInactiveText(quote.outputAmount.toPlainString())
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
                is RateResource.Available -> resource.ticker
                else -> null
            }
    }

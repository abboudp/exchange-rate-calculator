package com.example.exchangeratecalculator.presentation.calculator

import com.example.exchangeratecalculator.domain.model.Currency
import com.example.exchangeratecalculator.domain.model.USDC_CURRENCY

enum class AmountField { TOP, BOTTOM }

data class CalculatorUiState(
    val topCurrencyCode: String = USDC_CURRENCY.code,
    val bottomCurrencyCode: String = "MXN",
    val topAmountText: String = "",
    val bottomAmountText: String = "",
    val activeField: AmountField = AmountField.TOP,
    val isSwapped: Boolean = false,
    val rateDisplayState: RateDisplayState = RateDisplayState.Loading,
    val pickerState: CurrencyPickerState = CurrencyPickerState(),
    val canBackspace: Boolean = false,
    val canInsertDecimal: Boolean = true,
)

sealed interface RateDisplayState {
    data object Loading : RateDisplayState

    data class Available(val text: String, val isFresh: Boolean) : RateDisplayState

    data object Unavailable : RateDisplayState
}

data class CurrencyPickerState(
    val isVisible: Boolean = false,
    val currencies: List<Currency> = emptyList(),
    val selectedCode: String = "MXN",
    val isLoading: Boolean = false,
)

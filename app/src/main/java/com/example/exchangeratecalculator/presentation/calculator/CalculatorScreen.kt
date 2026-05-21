package com.example.exchangeratecalculator.presentation.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exchangeratecalculator.R
import com.example.exchangeratecalculator.domain.model.USDC_CURRENCY
import com.example.exchangeratecalculator.presentation.theme.BrandGreen
import com.example.exchangeratecalculator.presentation.theme.PrimaryText
import com.example.exchangeratecalculator.presentation.theme.ScreenBackground

const val RATE_DISPLAY_TAG = "rate_display"
private val StaleRateGray = PrimaryText.copy(alpha = 0.5f)

@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        HeaderAndInputs(
            uiState = uiState,
            onFiatRowTapped = viewModel::onFiatRowTapped,
            onSwap = viewModel::onSwapPressed,
        )
        Spacer(modifier = Modifier.weight(1f))
        NumericKeypad(
            onDigit = viewModel::onDigitPressed,
            onDecimal = viewModel::onDecimalPressed,
            onBackspace = viewModel::onBackspacePressed,
            canInsertDecimal = uiState.canInsertDecimal,
            canBackspace = uiState.canBackspace,
        )
    }

    if (uiState.pickerState.isVisible) {
        CurrencyBottomSheet(
            currencies = uiState.pickerState.currencies,
            selectedCode = uiState.pickerState.selectedCode,
            onSelected = viewModel::onCurrencySelected,
            onDismiss = viewModel::onPickerDismissed,
        )
    }
}

@Composable
private fun HeaderAndInputs(
    uiState: CalculatorUiState,
    onFiatRowTapped: () -> Unit,
    onSwap: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.calculator_title),
            style = MaterialTheme.typography.headlineMedium,
            color = PrimaryText,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        RateDisplayText(
            state = uiState.rateDisplayState,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        AmountRowsWithSwap(
            uiState = uiState,
            onSwap = onSwap,
            onFiatRowTapped = onFiatRowTapped,
        )
    }
}

@Composable
private fun RateDisplayText(
    state: RateDisplayState,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is RateDisplayState.Loading -> {
            Spacer(modifier = modifier.height(20.dp).testTag(RATE_DISPLAY_TAG))
        }
        is RateDisplayState.Available -> {
            Row(
                modifier = modifier.testTag(RATE_DISPLAY_TAG),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = state.text,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.isFresh) BrandGreen else StaleRateGray,
                )
                if (!state.isFresh) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "Rate is degraded or stale",
                        tint = StaleRateGray,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        is RateDisplayState.Unavailable -> {
            Row(
                modifier = modifier.testTag(RATE_DISPLAY_TAG),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.rate_unavailable),
                    style = MaterialTheme.typography.labelMedium,
                    color = StaleRateGray,
                )
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = "Rate unavailable",
                    tint = StaleRateGray,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun AmountRowsWithSwap(
    uiState: CalculatorUiState,
    onSwap: () -> Unit,
    onFiatRowTapped: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column {
            CurrencyAmountRow(
                currencyCode = uiState.topCurrencyCode,
                isSelectable = uiState.topCurrencyCode != USDC_CURRENCY.code,
                amountDisplay =
                    MoneyFormatter.formatAmountDisplay(
                        rawText = uiState.topAmountText,
                        isActive = uiState.activeField == AmountField.TOP,
                    ),
                isActive = uiState.activeField == AmountField.TOP,
                onCurrencyClick = onFiatRowTapped,
                testTag = CURRENCY_ROW_TOP_TAG,
            )
            Spacer(modifier = Modifier.height(16.dp))
            CurrencyAmountRow(
                currencyCode = uiState.bottomCurrencyCode,
                isSelectable = uiState.bottomCurrencyCode != USDC_CURRENCY.code,
                amountDisplay =
                    MoneyFormatter.formatAmountDisplay(
                        rawText = uiState.bottomAmountText,
                        isActive = false,
                    ),
                isActive = false,
                onCurrencyClick = onFiatRowTapped,
                testTag = CURRENCY_ROW_BOTTOM_TAG,
            )
        }
        SwapButton(
            onClick = onSwap,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

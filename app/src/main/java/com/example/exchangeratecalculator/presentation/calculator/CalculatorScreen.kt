package com.example.exchangeratecalculator.presentation.calculator

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exchangeratecalculator.R
import com.example.exchangeratecalculator.domain.model.USDC_CURRENCY
import com.example.exchangeratecalculator.presentation.theme.BrandGreen
import com.example.exchangeratecalculator.presentation.theme.PrimaryText
import com.example.exchangeratecalculator.presentation.theme.ScreenBackground
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
                .statusBarsPadding(),
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
                    StaleRateInfoIcon()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaleRateInfoIcon() {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    val tooltipText = stringResource(R.string.rate_stale_tooltip)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltipText) } },
        state = tooltipState,
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = tooltipText,
            tint = StaleRateGray,
            modifier =
                Modifier
                    .size(14.dp)
                    .clickable { scope.launch { tooltipState.show() } },
        )
    }
}

@Composable
private fun AmountRowsWithSwap(
    uiState: CalculatorUiState,
    onSwap: () -> Unit,
    onFiatRowTapped: () -> Unit,
) {
    val density = LocalDensity.current
    val rowAndGapPx = remember(density) { with(density) { (66.dp + 16.dp).toPx() } }

    val topOffset = remember { Animatable(0f) }
    val bottomOffset = remember { Animatable(0f) }
    val animScope = rememberCoroutineScope()

    LaunchedEffect(uiState.swapAnimationKey) {
        if (uiState.swapAnimationKey == 0) return@LaunchedEffect
        topOffset.snapTo(rowAndGapPx)
        bottomOffset.snapTo(-rowAndGapPx)
        val animSpec = tween<Float>(durationMillis = 230, easing = FastOutSlowInEasing)
        animScope.launch { topOffset.animateTo(0f, animSpec) }
        animScope.launch { bottomOffset.animateTo(0f, animSpec) }
    }

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
                modifier = Modifier.offset { IntOffset(0, topOffset.value.roundToInt()) },
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
                modifier = Modifier.offset { IntOffset(0, bottomOffset.value.roundToInt()) },
                testTag = CURRENCY_ROW_BOTTOM_TAG,
            )
        }
        SwapButton(
            onClick = onSwap,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

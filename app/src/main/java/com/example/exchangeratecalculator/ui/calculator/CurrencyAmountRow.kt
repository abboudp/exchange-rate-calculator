package com.example.exchangeratecalculator.ui.calculator

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.exchangeratecalculator.ui.theme.CardBackground
import com.example.exchangeratecalculator.ui.theme.CursorBlue
import com.example.exchangeratecalculator.ui.theme.PrimaryText

@Composable
fun CurrencyAmountRow(
    currencyCode: String,
    isSelectable: Boolean,
    amountDisplay: String,
    isActive: Boolean,
    onCurrencyClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
) {
    val displayCode = if (currencyCode == "USDC") "USDc" else currencyCode
    Card(
        onClick = onCurrencyClick,
        enabled = isSelectable,
        modifier = modifier.fillMaxWidth().height(66.dp).testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = CardBackground,
                disabledContainerColor = CardBackground,
                contentColor = PrimaryText,
                disabledContentColor = PrimaryText,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp,
                pressedElevation = if (isSelectable) 4.dp else 0.dp,
            ),
    ) {
        CardContent(
            displayCode = displayCode,
            currencyCode = currencyCode,
            isSelectable = isSelectable,
            amountDisplay = amountDisplay,
            isActive = isActive,
        )
    }
}

@Composable
private fun CardContent(
    displayCode: String,
    currencyCode: String,
    isSelectable: Boolean,
    amountDisplay: String,
    isActive: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CurrencyFlag(code = currencyCode, modifier = Modifier.size(16.dp))
            Text(
                text = displayCode,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = PrimaryText,
            )
            if (isSelectable) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Choose currency",
                    tint = PrimaryText.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        AmountText(
            amountDisplay = amountDisplay,
            isActive = isActive,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
        )
    }
}

@Composable
private fun AmountText(
    amountDisplay: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = amountDisplay,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = PrimaryText,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isActive) {
            BlinkingCursor()
        }
    }
}

@Composable
private fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "amountCursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "amountCursorAlpha",
    )
    Box(
        modifier =
            Modifier
                .padding(start = 2.dp)
                .width(2.dp)
                .height(20.dp)
                .alpha(alpha)
                .background(CursorBlue, shape = RoundedCornerShape(1.dp)),
    )
}

const val CURRENCY_ROW_TOP_TAG = "currency_row_top"
const val CURRENCY_ROW_BOTTOM_TAG = "currency_row_bottom"

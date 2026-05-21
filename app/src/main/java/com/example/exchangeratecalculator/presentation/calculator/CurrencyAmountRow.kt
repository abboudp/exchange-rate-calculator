package com.example.exchangeratecalculator.presentation.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.exchangeratecalculator.presentation.theme.BrandGreen
import com.example.exchangeratecalculator.presentation.theme.CardBackground

@Composable
fun CurrencyAmountRow(
    currencyCode: String,
    isSelectable: Boolean,
    amountDisplay: String,
    isActive: Boolean,
    onCurrencyClick: () -> Unit,
    onAmountFieldClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
) {
    Card(
        modifier = modifier.fillMaxWidth().testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = if (isActive) BorderStroke(1.5.dp, BrandGreen) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier =
                    Modifier
                        .then(if (isSelectable) Modifier.clickable { onCurrencyClick() } else Modifier)
                        .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CurrencyFlagPlaceholder(currencyCode)
                Text(
                    text = currencyCode,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (isSelectable) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Choose currency",
                    )
                }
            }
            Text(
                text = amountDisplay,
                style = MaterialTheme.typography.titleLarge,
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable { onAmountFieldClick() }
                        .padding(start = 8.dp),
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CurrencyFlagPlaceholder(code: String) {
    androidx.compose.foundation.layout.Box(
        modifier =
            Modifier
                .size(28.dp)
                .background(color = Color(0xFFE5E5EA), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = code.take(1),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

const val CURRENCY_ROW_TOP_TAG = "currency_row_top"
const val CURRENCY_ROW_BOTTOM_TAG = "currency_row_bottom"

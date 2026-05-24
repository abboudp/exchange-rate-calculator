package com.example.exchangeratecalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.exchangeratecalculator.R
import com.example.exchangeratecalculator.domain.model.Currency
import com.example.exchangeratecalculator.ui.theme.BrandGreen
import com.example.exchangeratecalculator.ui.theme.CardBackground
import com.example.exchangeratecalculator.ui.theme.HandleGray
import com.example.exchangeratecalculator.ui.theme.PickerFlagBackground
import com.example.exchangeratecalculator.ui.theme.PrimaryText
import com.example.exchangeratecalculator.ui.theme.ScreenBackground
import com.example.exchangeratecalculator.ui.theme.SelectionBorderGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyBottomSheet(
    currencies: List<Currency>,
    selectedCode: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag(PICKER_SHEET_TAG),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = ScreenBackground,
        dragHandle = { BottomSheetHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.choose_currency),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 24.sp,
                    color = PrimaryText,
                    modifier = Modifier.weight(1f),
                )
                CloseButton(onClick = onDismiss)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 310.dp)) {
                    items(currencies, key = { it.code }) { currency ->
                        CurrencyPickerItem(
                            currency = currency,
                            isSelected = currency.code == selectedCode,
                            onClick = { onSelected(currency.code) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomSheetHandle() {
    Box(
        modifier = Modifier.fillMaxWidth().height(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(HandleGray),
        )
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .testTag(PICKER_CLOSE_TAG)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Close",
            tint = PrimaryText,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun CurrencyPickerItem(
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .height(62.dp)
                .padding(horizontal = 16.dp)
                .testTag("picker_row_${currency.code}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CurrencyPickerFlag(currency.code)
        Text(
            text = currency.code,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText,
            modifier = Modifier.weight(1f),
        )
        SelectionIndicator(isSelected = isSelected)
    }
}

@Composable
private fun CurrencyPickerFlag(code: String) {
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .background(color = PickerFlagBackground, shape = RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        CurrencyFlag(code = code, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun SelectionIndicator(isSelected: Boolean) {
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .then(
                    if (isSelected) {
                        Modifier.background(BrandGreen, CircleShape)
                    } else {
                        Modifier.border(2.dp, SelectionBorderGray, CircleShape)
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

const val PICKER_SHEET_TAG = "picker_sheet"
const val PICKER_CLOSE_TAG = "picker_close"

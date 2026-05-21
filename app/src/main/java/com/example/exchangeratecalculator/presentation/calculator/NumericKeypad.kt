package com.example.exchangeratecalculator.presentation.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.exchangeratecalculator.presentation.theme.CardBackground
import com.example.exchangeratecalculator.presentation.theme.PrimaryText

@Composable
fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    canInsertDecimal: Boolean,
    canBackspace: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KeypadRow {
            DigitKey('1', null, onDigit)
            DigitKey('2', "ABC", onDigit)
            DigitKey('3', "DEF", onDigit)
        }
        KeypadRow {
            DigitKey('4', "GHI", onDigit)
            DigitKey('5', "JKL", onDigit)
            DigitKey('6', "MNO", onDigit)
        }
        KeypadRow {
            DigitKey('7', "PQRS", onDigit)
            DigitKey('8', "TUV", onDigit)
            DigitKey('9', "WXYZ", onDigit)
        }
        KeypadRow {
            DecimalKey(enabled = canInsertDecimal, onDecimal = onDecimal)
            DigitKey('0', null, onDigit)
            BackspaceKey(enabled = canBackspace, onBackspace = onBackspace)
        }
    }
}

@Composable
private fun KeypadRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DigitKey(
    digit: Char,
    subLabel: String?,
    onDigit: (Char) -> Unit,
) {
    KeyButton(
        onClick = { onDigit(digit) },
        enabled = true,
        testTag = "keypad_$digit",
        modifier = Modifier.weight(1f),
    ) {
        Text(
            text = digit.toString(),
            color = PrimaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
        )
        if (subLabel != null) {
            Text(
                text = subLabel,
                color = PrimaryText.copy(alpha = 0.6f),
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DecimalKey(
    enabled: Boolean,
    onDecimal: () -> Unit,
) {
    KeyButton(
        onClick = onDecimal,
        enabled = enabled,
        testTag = "keypad_dot",
        modifier = Modifier.weight(1f),
    ) {
        Text(
            text = ".",
            color = PrimaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BackspaceKey(
    enabled: Boolean,
    onBackspace: () -> Unit,
) {
    KeyButton(
        onClick = onBackspace,
        enabled = enabled,
        testTag = "keypad_backspace",
        modifier = Modifier.weight(1f),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Backspace",
            tint = PrimaryText,
        )
    }
}

@Composable
private fun KeyButton(
    onClick: () -> Unit,
    enabled: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) CardBackground else CardBackground.copy(alpha = 0.5f),
        contentColor = if (enabled) PrimaryText else PrimaryText.copy(alpha = 0.4f),
        shadowElevation = 1.dp,
        onClick = onClick,
        enabled = enabled,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

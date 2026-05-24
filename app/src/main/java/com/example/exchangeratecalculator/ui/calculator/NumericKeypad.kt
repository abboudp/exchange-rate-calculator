package com.example.exchangeratecalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.exchangeratecalculator.ui.theme.KeyBackground
import com.example.exchangeratecalculator.ui.theme.KeyboardBackground
import com.example.exchangeratecalculator.ui.theme.PrimaryText

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
        modifier =
            modifier
                .fillMaxWidth()
                .background(KeyboardBackground)
                .navigationBarsPadding()
                .padding(top = 6.dp, bottom = 12.dp),
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
private fun KeypadRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(KEY_HEIGHT).padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun RowScope.DigitKey(
    digit: Char,
    subLabel: String?,
    onDigit: (Char) -> Unit,
) {
    KeyButton(
        onClick = { onDigit(digit) },
        enabled = true,
        testTag = "keypad_$digit",
        modifier = Modifier.weight(1f),
        hasSurface = true,
    ) {
        Text(
            text = digit.toString(),
            color = PrimaryText,
            fontWeight = FontWeight.Normal,
            fontSize = 25.sp,
        )
        Text(
            text = subLabel.orEmpty(),
            color = PrimaryText,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun RowScope.DecimalKey(
    enabled: Boolean,
    onDecimal: () -> Unit,
) {
    KeyButton(
        onClick = onDecimal,
        enabled = enabled,
        testTag = "keypad_dot",
        modifier = Modifier.weight(1f),
        hasSurface = true,
    ) {
        Text(
            text = ".",
            color = PrimaryText,
            fontWeight = FontWeight.Normal,
            fontSize = 25.sp,
        )
    }
}

@Composable
private fun RowScope.BackspaceKey(
    enabled: Boolean,
    onBackspace: () -> Unit,
) {
    KeyButton(
        onClick = onBackspace,
        enabled = enabled,
        testTag = "keypad_backspace",
        modifier = Modifier.weight(1f),
        hasSurface = false,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Backspace",
            tint = PrimaryText.copy(alpha = if (enabled) 1f else 0.35f),
        )
    }
}

@Composable
private fun KeyButton(
    onClick: () -> Unit,
    enabled: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    hasSurface: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(5.dp),
        color = if (hasSurface) KeyBackground else Color.Transparent,
        contentColor = PrimaryText,
        shadowElevation = if (hasSurface) 1.dp else 0.dp,
        onClick = onClick,
        enabled = enabled,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(KEY_HEIGHT).padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

private val KEY_HEIGHT = 46.dp

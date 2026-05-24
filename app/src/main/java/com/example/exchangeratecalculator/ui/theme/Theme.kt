package com.example.exchangeratecalculator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ExchangeColorScheme =
    lightColorScheme(
        primary = BrandGreen,
        onPrimary = Color.White,
        background = ScreenBackground,
        surface = CardBackground,
        onSurface = PrimaryText,
        onBackground = PrimaryText,
    )

@Composable
fun ExchangeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ExchangeColorScheme,
        typography = ExchangeTypography,
        content = content,
    )
}

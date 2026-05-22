package com.example.exchangeratecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.exchangeratecalculator.presentation.calculator.CalculatorScreen
import com.example.exchangeratecalculator.presentation.theme.ExchangeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExchangeTheme {
                CalculatorScreen()
            }
        }
    }
}

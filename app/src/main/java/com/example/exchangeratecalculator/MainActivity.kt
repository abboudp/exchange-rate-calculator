package com.example.exchangeratecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.exchangeratecalculator.presentation.theme.ExchangeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExchangeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlaceholderContent()
                }
            }
        }
    }
}

@Composable
private fun PlaceholderContent() {
    Text(
        text = stringResource(R.string.calculator_title),
        modifier = Modifier.padding(16.dp),
    )
}

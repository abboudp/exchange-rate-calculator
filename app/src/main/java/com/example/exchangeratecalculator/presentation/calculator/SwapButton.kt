package com.example.exchangeratecalculator.presentation.calculator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.exchangeratecalculator.presentation.theme.BrandGreen

@Composable
fun SwapButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = BrandGreen,
        contentColor = Color.White,
        modifier = modifier.testTag(SWAP_BUTTON_TAG),
    ) {
        Icon(
            imageVector = Icons.Rounded.SwapVert,
            contentDescription = "Swap currencies",
        )
    }
}

const val SWAP_BUTTON_TAG = "swap_button"

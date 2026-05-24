package com.example.exchangeratecalculator.ui.calculator

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.exchangeratecalculator.R

@DrawableRes
fun currencyFlagRes(code: String): Int? =
    when (code) {
        "USDC" -> R.drawable.flag_usdc
        "MXN" -> R.drawable.flag_mxn
        "ARS" -> R.drawable.flag_ars
        "BRL" -> R.drawable.flag_brl
        "COP" -> R.drawable.flag_cop
        else -> null
    }

@Composable
fun CurrencyFlag(
    code: String,
    modifier: Modifier = Modifier,
) {
    val res = currencyFlagRes(code) ?: return
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = modifier.clip(CircleShape),
        contentScale = ContentScale.Fit,
    )
}

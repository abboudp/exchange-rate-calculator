package com.example.exchangeratecalculator.data.remote

import androidx.annotation.DrawableRes
import com.example.exchangeratecalculator.R

enum class SupportedCurrency(val code: String, @DrawableRes val flagRes: Int) {
    MXN("MXN", R.drawable.flag_mxn),
    ARS("ARS", R.drawable.flag_ars),
    BRL("BRL", R.drawable.flag_brl),
    COP("COP", R.drawable.flag_cop),
}

val DEFAULT_FIAT_CODE: String = SupportedCurrency.entries.first().code

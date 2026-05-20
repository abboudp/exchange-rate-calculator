package com.example.exchangeratecalculator.core.result

sealed interface Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>

    data class Error(
        val exception: Throwable,
        val message: String? = null,
    ) : Resource<Nothing>

    data object Loading : Resource<Nothing>
}

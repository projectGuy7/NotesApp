package com.example.notesapp.presentation.util

sealed interface RequestResult<T> {

    data class Success<T>(val value: T): RequestResult<T>

    data class Error<T>(
        val message: String
    ): RequestResult<T>

}
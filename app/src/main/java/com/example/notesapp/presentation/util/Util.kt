package com.example.notesapp.presentation.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val noteDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy h:mm:ss a", Locale.US)

fun LocalDateTime.toNoteDateString(): String {
    return this.format(noteDateFormatter)
}
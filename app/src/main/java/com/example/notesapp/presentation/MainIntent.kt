package com.example.notesapp.presentation

sealed interface MainIntent {
    data class CreateNote(val title: String, val content: String) : MainIntent
    data class UpdateNote(val id: Long, val content: String) : MainIntent
    data class DeleteNote(val id: Long) : MainIntent
}
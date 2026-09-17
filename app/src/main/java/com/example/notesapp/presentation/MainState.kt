package com.example.notesapp.presentation

import kotlinx.serialization.Serializable

data class NotesListState(
    val notes: List<NoteUI> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@Serializable
data class NoteUI(
    val id: Long,
    val title: String,
    val date: String,
    val content: String
)
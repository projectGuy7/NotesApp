package com.example.notesapp.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.domain.DBRepository
import com.example.notesapp.domain.Note
import com.example.notesapp.presentation.util.RequestResult
import com.example.notesapp.presentation.util.toNoteDateString
import com.example.notesapp.data.room.toNoteUI
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MainViewModel(
    val repository: DBRepository,
    val analytics: FirebaseAnalytics = Firebase.analytics
): ViewModel() {

    private val _notes: Flow<List<NoteUI>> = repository.observeNotes().map {
        it.map(Note::toNoteUI)
    }

    private val _notesListState = MutableStateFlow(NotesListState())
    val notesListState = combine(_notes, _notesListState) { notes, state ->
        NotesListState(
            notes = notes,
            isLoading = state.isLoading,
            error = state.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotesListState())

    fun onEvent(intent: MainIntent) {
        viewModelScope.launch {
            when(intent) {
                is MainIntent.CreateNote -> {
                    _notesListState.value = _notesListState.value.copy(error = null, isLoading = true)
                    val result = repository.addNote(
                        intent.title, LocalDateTime.now().toNoteDateString(), intent.content
                    )
                    when(result) {
                        is RequestResult.Success -> {
                            analytics.logEvent(ADD_NOTE_ANALYTICS, null)
                            _notesListState.value = _notesListState.value.copy(isLoading = false)
                        }
                        is RequestResult.Error -> {
                            _notesListState.value = _notesListState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
                is MainIntent.DeleteNote -> {
                    _notesListState.value = _notesListState.value.copy(error = null, isLoading = true)
                    when(val result = repository.deleteNote(intent.id)) {
                        is RequestResult.Success -> {
                            analytics.logEvent(REMOVE_NOTE_ANALYTICS, null)
                            _notesListState.value = _notesListState.value.copy(isLoading = false)
                        }
                        is RequestResult.Error -> {
                            _notesListState.value = _notesListState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
                is MainIntent.UpdateNote -> {
                    _notesListState.value = _notesListState.value.copy(error = null, isLoading = true)
                    when(val result = repository.updateNote(
                        intent.id,
                        intent.content,
                        LocalDateTime.now().toNoteDateString()
                    )) {
                        is RequestResult.Success -> {
                            _notesListState.value = _notesListState.value.copy(isLoading = false)
                        }
                        is RequestResult.Error -> {
                            _notesListState.value = _notesListState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }
}

const val ADD_NOTE_ANALYTICS = "add_note"
const val REMOVE_NOTE_ANALYTICS = "remove_note"
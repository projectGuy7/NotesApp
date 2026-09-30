package com.example.notesapp.domain

import android.content.Context
import com.example.notesapp.presentation.util.RequestResult
import com.example.notesapp.data.room.DatabaseProvider
import com.example.notesapp.data.room.NoteEntity
import com.example.notesapp.data.room.toNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.format.DateTimeParseException
import kotlin.collections.map

class DBRepository(val context: Context) {

    fun observeNotes(): Flow<List<Note>> {
        return DatabaseProvider.getInstance(context).notesDao.getAllNotes()
            .map { it.map(NoteEntity::toNote) }
    }

    suspend fun addNote(title: String, date: String, content: String): RequestResult<Unit> {
        if(title.isBlank() || date.isBlank()) return RequestResult.Error("Title and date cannot be empty")
        try {
            DatabaseProvider.getInstance(context).notesDao.insertNote(
                NoteEntity(
                    title = title,
                    date = date,
                    content = content
                )
            )
            return RequestResult.Success(Unit)
        } catch (e: DateTimeParseException) {
            return RequestResult.Error("Invalid date format")
        }
    }

    suspend fun deleteNote(id: Long): RequestResult<Unit> {
        try {
            DatabaseProvider.getInstance(context).notesDao.deleteNote(id)
            return RequestResult.Success(Unit)
        } catch (e: Exception) {
            return RequestResult.Error("Failed to delete note")
        }
    }

    suspend fun updateNote(id: Long, content: String, date: String): RequestResult<Unit> {
        try {
            DatabaseProvider.getInstance(context).notesDao.updateNoteContent(
                id,
                content,
                date
            )
            return RequestResult.Success(Unit)
        } catch (e: Exception) {
            return RequestResult.Error("Failed to update note")
        }
    }

}
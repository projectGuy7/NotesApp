package com.example.notesapp.room

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {

    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Insert
    suspend fun insertNote(note: NoteEntity)

    @Query("UPDATE notes SET content = :noteContent, date = :noteDate WHERE id = :noteId")
    suspend fun updateNoteContent(noteId: Long, noteContent: String, noteDate: String)

}
package com.example.notesapp.data.room

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(
    tableName = NoteEntity.TABLE_NAME
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String,
    val content: String
) {
    companion object {
        const val TABLE_NAME = "notes"
    }
}
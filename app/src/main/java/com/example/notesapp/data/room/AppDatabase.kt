package com.example.notesapp.data.room

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [NoteEntity::class],
    version = 1
)
abstract class AppDatabase: RoomDatabase() {
    abstract val notesDao: NotesDao
}
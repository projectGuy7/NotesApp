package com.example.notesapp.data.room

import android.content.Context
import androidx.room3.Room

object DatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: buildDatabase(context).also { instance = it }
        }
    }

    private fun buildDatabase(context: Context): AppDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "notes.db"
        ).build()
}
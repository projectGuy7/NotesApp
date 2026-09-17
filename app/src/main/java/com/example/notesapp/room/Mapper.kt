package com.example.notesapp.room

import com.example.notesapp.domain.Note
import com.example.notesapp.presentation.NoteUI

fun NoteEntity.toNote(): Note {
    return Note(
        id = this.id,
        title = this.title,
        date = this.date,
        content = this.content
    )
}

fun Note.toNoteEntity(): NoteEntity {
    return NoteEntity(
        id = this.id,
        title = this.title,
        date = this.date,
        content = this.content
    )
}

fun Note.toNoteUI(): NoteUI {
    return NoteUI(
        id = this.id,
        title = this.title,
        date = this.date,
        content = this.content
    )
}

fun NoteUI.toNote(): Note {
    return Note(
        id = this.id,
        title = this.title,
        date = this.date,
        content = this.content
    )
}
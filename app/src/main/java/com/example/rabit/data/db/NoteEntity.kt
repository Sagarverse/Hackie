package com.example.rabit.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val source: String,
    val createdAtMs: Long
)

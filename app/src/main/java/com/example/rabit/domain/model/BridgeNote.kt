package com.example.rabit.domain.model

data class BridgeNote(
    val id: String,
    val text: String,
    val createdAtMs: Long,
    val source: String
)

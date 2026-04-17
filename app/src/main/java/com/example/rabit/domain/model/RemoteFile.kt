package com.example.rabit.domain.model

data class RemoteFile(
    val name: String = "",
    val path: String = "",
    val size: Long = 0L,
    val isDirectory: Boolean = false,
    val extension: String = "",
    val modifiedTime: Long = System.currentTimeMillis()
)

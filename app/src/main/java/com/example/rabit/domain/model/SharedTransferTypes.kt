package com.example.rabit.domain.model

import android.net.Uri

enum class TransferQueueStatus { Queued, Ready, Failed }
enum class TransferHapticType { COMPLETE, ERROR, PROGRESS }

data class SharedTransferItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val status: TransferQueueStatus,
    val progress: Int,
    val addedAt: Long
)

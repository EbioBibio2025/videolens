package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val mediaType: String, // "VIDEO", "AUDIO", "IMAGE", "DOCUMENT"
    val fileSizeBytes: Long,
    val durationMs: Long = 0L,
    val pageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val indexedAt: Long? = null,
    val indexingStatus: String = "IDLE", // "IDLE", "EXTRACTING", "EMBEDDING", "COMPLETED", "FAILED", "PAUSED"
    val errorMessage: String? = null,
    val topicTag: String = "عمومی",
    val thumbnailUri: String? = null
)

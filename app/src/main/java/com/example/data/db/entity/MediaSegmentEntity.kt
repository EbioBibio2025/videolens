package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_segments",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mediaId"])]
)
data class MediaSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val segmentIndex: Int,
    val timestampMs: Long? = null,
    val pageNumber: Int? = null,
    val textSnippet: String = "",
    val previewConcept: String = "",
    val embeddingDim: Int = 256,
    val modelVersion: String = "EmbeddingGemma-2b-Phase1",
    val embeddingVectorJson: String // Comma separated or JSON array of normalized floats
)

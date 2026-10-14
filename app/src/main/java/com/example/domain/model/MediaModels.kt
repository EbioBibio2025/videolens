package com.example.domain.model

import com.example.data.db.entity.MediaItemEntity
import com.example.data.db.entity.MediaSegmentEntity

enum class MediaType(val titleFa: String, val iconName: String) {
    VIDEO("ویدئو", "video"),
    AUDIO("صوت", "audio"),
    IMAGE("تصویر", "image"),
    DOCUMENT("سند", "document");

    companion object {
        fun fromMimeOrExtension(name: String, mime: String?): MediaType {
            val lowerName = name.lowercase()
            val lowerMime = mime?.lowercase().orEmpty()
            return when {
                lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".mov") || lowerName.endsWith(".webm") -> VIDEO
                lowerMime.startsWith("audio/") || lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") || lowerName.endsWith(".m4a") || lowerName.endsWith(".ogg") || lowerName.endsWith(".aac") -> AUDIO
                lowerMime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") -> IMAGE
                else -> DOCUMENT
            }
        }
    }
}

enum class IndexingStatus(val titleFa: String) {
    IDLE("در انتظار پردازش"),
    EXTRACTING("در حال استخراج فریم‌ها / قطعات"),
    EMBEDDING("در حال تولید بردار معنایی"),
    COMPLETED("تکمیل و ایندکس شده"),
    FAILED("خطا در ایندکس"),
    PAUSED("متوقف شده")
}

enum class ModalityFilter(val titleFa: String) {
    ALL("همه"),
    VIDEO("ویدئوها"),
    AUDIO("صداها"),
    IMAGE("تصاویر"),
    DOCUMENT("اسناد")
}

enum class EmbeddingTaskPrefix(val displayName: String, val prefix: String) {
    TASK_SEARCH_QUERY("EmbeddingGemma: query", "task: search_query | query: "),
    TASK_DOCUMENT("EmbeddingGemma: doc", "task: search_document | doc: "),
    RETRIEVAL_QUERY("MediaPipe Retrieval", "retrieval.query: "),
    RAW_TEXT("بدون پیشوند", "")
}

data class SearchResultItem(
    val mediaItem: MediaItemEntity,
    val segment: MediaSegmentEntity,
    val similarityScore: Float,
    val similarityPercent: Int,
    val matchSnippet: String
)

data class TopicCluster(
    val topicTag: String,
    val count: Int,
    val sampleTitle: String,
    val mediaTypes: List<String>
)

data class ModelRuntimeInfo(
    val modelName: String,
    val engineType: String,
    val isInstalled: Boolean,
    val isPhase1Mock: Boolean,
    val embeddingDimension: Int,
    val statusTitleFa: String,
    val statusDescriptionFa: String,
    val documentationNotice: String
)

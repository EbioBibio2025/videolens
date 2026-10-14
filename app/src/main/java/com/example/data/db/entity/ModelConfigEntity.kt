package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_config")
data class ModelConfigEntity(
    @PrimaryKey val id: Int = 1,
    val modelName: String = "EmbeddingGemma 2 LiteRT-LM (MediaPipe Universal Embedder)",
    val modelFileName: String = "embeddinggemma-2b-it-cpu.tflite",
    val dimension: Int = 256,
    val isMockMode: Boolean = true,
    val modelPath: String = "/sdcard/Download/embeddinggemma-2b-it-cpu.tflite",
    val statusMessage: String = "فاز ۱: فعال با داده‌های آزمایشی. رابط رسمی MediaPipe آماده اتصال فایل مدل.",
    val lastVerifiedAt: Long = System.currentTimeMillis()
)

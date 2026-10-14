package com.example.domain.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.domain.search.VectorSearchCalculator
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.retrieval.universalembedder.UniversalEmbedder
import com.google.mediapipe.tasks.retrieval.universalembedder.UniversalEmbedderOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sqrt

/**
 * Real on-device EmbeddingGemma 2 Multimodal Embedding Engine.
 *
 * Implements the official Google MediaPipe Universal Embedder runtime (`com.google.mediapipe:tasks-retrieval`).
 * Powered by on-device LiteRT-LM using the verified model artifact `embeddinggemma-2-text-vision-440m.litertlm`.
 *
 * Technical Specifications:
 * - Model Architecture: EmbeddingGemma 2 (270M text + 170M vision backbone = 440M parameters)
 * - Vector Space: 768-dimensional native multimodal embeddings
 * - Dimensionality: Supports Matryoshka Representation Learning (MRL) truncation to 256/512 dimensions with L2 re-normalization
 * - Text Task Prefixes:
 *     Search Queries: "task: search_query | query: <text>"
 *     Document / Item indexing: "task: search_document | doc: <text>"
 * - Image Processing: Bitmap decoding and MPImage construction via BitmapImageBuilder
 */
class RealEmbeddingGemmaEngine(
    private val context: Context,
    val modelFilePath: String = getDefaultModelPath(context),
    override val dimension: Int = 256
) : MultimodalEmbeddingEngine, AutoCloseable {

    override val engineName: String = "Google MediaPipe Universal Embedder (EmbeddingGemma 2 LiteRT-LM)"
    override val isPhase1Mock: Boolean = false

    private var embedder: UniversalEmbedder? = null
    private var isInitialized: Boolean = false
    private var lastInitError: String? = null

    companion object {
        const val OFFICIAL_MODEL_FILENAME = "embeddinggemma-2-text-vision-440m.litertlm"
        const val NATIVE_EMBEDDING_DIMENSION = 768

        fun getDefaultModelPath(context: Context): String {
            val internalFile = File(context.filesDir, "models/$OFFICIAL_MODEL_FILENAME")
            if (internalFile.exists()) return internalFile.absolutePath

            val downloadFile = File("/sdcard/Download/$OFFICIAL_MODEL_FILENAME")
            if (downloadFile.exists()) return downloadFile.absolutePath

            return internalFile.absolutePath
        }

        /**
         * Formats Persian/multilingual text queries and documents with the official task prefix.
         */
        fun formatWithTaskPrefix(text: String, isQuery: Boolean): String {
            val prefix = if (isQuery) "task: search_query | query: " else "task: search_document | doc: "
            return if (text.startsWith("task:")) text else "$prefix$text"
        }
    }

    override suspend fun initialize(): EngineInitResult = withContext(Dispatchers.IO) {
        val file = File(modelFilePath)
        if (!file.exists() || !file.canRead() || file.length() == 0L) {
            isInitialized = false
            lastInitError = "فایل مدل $OFFICIAL_MODEL_FILENAME در مسیر $modelFilePath یافت نشد."
            return@withContext EngineInitResult.MissingModelFile(
                "فایل مدل باینری $OFFICIAL_MODEL_FILENAME در مسیر $modelFilePath موجود نیست (${file.length()} بایت)."
            )
        }

        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(file.absolutePath)
                .build()

            val options = UniversalEmbedderOptions.builder()
                .setBaseOptions(baseOptions)
                .setL2Normalize(true)
                .build()

            embedder = UniversalEmbedder.createFromOptions(context, options)
            isInitialized = true
            lastInitError = null
            EngineInitResult.Success("موتور MediaPipe Universal Embedder با موفقیت بارگذاری شد (${file.length() / (1024 * 1024)} MB).")
        } catch (t: Throwable) {
            isInitialized = false
            lastInitError = t.message ?: t.javaClass.simpleName
            EngineInitResult.Failed("خطا در مقداردهی اولیه موتور MediaPipe LiteRT: ${t.message}")
        }
    }

    override suspend fun embedText(text: String, prefix: String): FloatArray = withContext(Dispatchers.Default) {
        val instance = embedder ?: throw IllegalStateException(
            "موتور MediaPipe Universal Embedder بارگذاری نشده است: $lastInitError"
        )

        val formattedText = if (prefix.isNotBlank() && !text.startsWith("task:")) {
            "$prefix$text"
        } else {
            formatWithTaskPrefix(text, isQuery = true)
        }

        val result = instance.embedText(formattedText)
        val rawVector = result.embeddings().firstOrNull()?.floatEmbedding()
            ?: throw RuntimeException("پاسخ بردار از UniversalEmbedder خالی است.")

        // Apply Matryoshka Representation Learning truncation if dimension < 768
        if (dimension < rawVector.size) {
            VectorSearchCalculator.truncateAndNormalize(rawVector, dimension)
        } else {
            rawVector
        }
    }

    override suspend fun embedImage(imageBytes: ByteArray): FloatArray = withContext(Dispatchers.Default) {
        val instance = embedder ?: throw IllegalStateException(
            "موتور MediaPipe Universal Embedder بارگذاری نشده است: $lastInitError"
        )

        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw IllegalArgumentException("خطا در رمزگشایی تصویر با فرمت Bitmap.")

        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = instance.embedImage(mpImage)
        val rawVector = result.embeddings().firstOrNull()?.floatEmbedding()
            ?: throw RuntimeException("پاسخ بردار تصویر از UniversalEmbedder خالی است.")

        if (dimension < rawVector.size) {
            VectorSearchCalculator.truncateAndNormalize(rawVector, dimension)
        } else {
            rawVector
        }
    }

    override suspend fun embedAudioSegment(audioSamples: FloatArray): FloatArray {
        throw UnsupportedOperationException("استخراج و بردارسازی صوت برای فازهای بعدی برنامه‌ریزی شده است.")
    }

    override fun getRuntimeStatus(): EngineStatus {
        val file = File(modelFilePath)
        return when {
            isInitialized && embedder != null -> EngineStatus.Ready
            file.exists() && file.length() > 0L -> EngineStatus.BlockedOrMissingArtifact(
                reason = "فایل مدل شناسایی شد اما هنوز مقداردهی نشده است.",
                expectedPath = file.absolutePath,
                instructions = "برای بارگذاری موتور رسمی روی گزینه بررسی وضعیت در تنظیمات لمس نمایید."
            )
            else -> EngineStatus.BlockedOrMissingArtifact(
                reason = "فایل مدل هوش مصنوعی $OFFICIAL_MODEL_FILENAME یافت نشد.",
                expectedPath = modelFilePath,
                instructions = "فایل $OFFICIAL_MODEL_FILENAME را در مسیر $modelFilePath یا پوشه Download قرار دهید."
            )
        }
    }

    override fun close() {
        try {
            embedder?.close()
        } catch (_: Exception) {}
        embedder = null
        isInitialized = false
    }
}

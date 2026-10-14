package com.example.domain.engine

import java.io.File

/**
 * MediaPipe Universal Embedder / EmbeddingGemma 2 Integration Engine.
 *
 * Implements the contract for Google MediaPipe Tasks Retrieval & LiteRT-LM.
 * In Phase 1, model inference is explicitly kept marked as not yet integrated until
 * the actual device model artifact is supplied by the user or downloaded.
 *
 * Blocker Documentation:
 * - Official EmbeddingGemma 2 weights (~2.5GB-4GB) require on-device LiteRT/MediaPipe runtime
 *   and must be downloaded or placed in the app's internal or external storage directory.
 * - Standard MediaPipe Universal Embedder model file expected: `embeddinggemma-2b-it-cpu.tflite` or `embeddinggemma.task`.
 * - If the artifact is not present, this engine strictly returns [EngineStatus.BlockedOrMissingArtifact]
 *   and will NOT substitute silent fake data.
 */
class MediaPipeEmbeddingEngine(
    val customModelPath: String? = null,
    override val dimension: Int = 256
) : MultimodalEmbeddingEngine {

    override val engineName: String = "Google MediaPipe Universal Embedder (EmbeddingGemma 2 LiteRT-LM)"
    override val isPhase1Mock: Boolean = false

    private var isModelLoaded: Boolean = false
    private var modelFile: File? = null

    override suspend fun initialize(): EngineInitResult {
        val targetPath = customModelPath ?: "/sdcard/Download/embeddinggemma-2b-it-cpu.tflite"
        val candidate = File(targetPath)

        return if (candidate.exists() && candidate.canRead()) {
            modelFile = candidate
            isModelLoaded = true
            EngineInitResult.Success("فایل مدل در مسیر ${candidate.absolutePath} شناسایی شد (${candidate.length() / (1024 * 1024)} MB).")
        } else {
            isModelLoaded = false
            EngineInitResult.MissingModelFile(
                "فایل مدل EmbeddingGemma در مسیر $targetPath یافت نشد. برای فاز ۲ لطفاً فایل tflite یا task را در پوشه Download قرار دهید."
            )
        }
    }

    override suspend fun embedText(text: String, prefix: String): FloatArray {
        if (!isModelLoaded) {
            throw IllegalStateException(
                "موتور رسمی MediaPipe هنوز بارگذاری نشده است. مدل در مسیر مورد انتظار یافت نشد. به فاز ۱ (حالت شبیه‌سازی) مراجعه کنید."
            )
        }
        // When real MediaPipe tasks-retrieval / LiteRT-LM is wired with native lib:
        // TextEmbedder or UniversalEmbedder call with prefix (e.g. "task: search_query | query: $text")
        throw UnsupportedOperationException("فاز ۲: اجرای استنتاج برخط مدل پس از تایید فایل باینری فعال خواهد شد.")
    }

    override suspend fun embedImage(imageBytes: ByteArray): FloatArray {
        if (!isModelLoaded) {
            throw IllegalStateException("موتور رسمی MediaPipe بارگذاری نشده است.")
        }
        throw UnsupportedOperationException("فاز ۲: استنتاج فریم‌های تصویری پس از بررسی سخت‌افزار دستگاه فعال خواهد شد.")
    }

    override suspend fun embedAudioSegment(audioSamples: FloatArray): FloatArray {
        if (!isModelLoaded) {
            throw IllegalStateException("موتور رسمی MediaPipe بارگذاری نشده است.")
        }
        throw UnsupportedOperationException("فاز ۲: استنتاج قطعات صوتی ۱۶ کیلوهرتز در فاز ۳ کامل می‌شود.")
    }

    override fun getRuntimeStatus(): EngineStatus {
        val path = customModelPath ?: "/sdcard/Download/embeddinggemma-2b-it-cpu.tflite"
        val file = File(path)
        return if (file.exists()) {
            EngineStatus.Ready
        } else {
            EngineStatus.BlockedOrMissingArtifact(
                reason = "فایل وزن‌های مدل EmbeddingGemma 2 روی حافظه دستگاه یافت نشد.",
                expectedPath = path,
                instructions = "برای فعال‌سازی استنتاج واقعی در فاز ۲، فایل embeddinggemma-2b-it-cpu.tflite را به پوشه Download منتقل کرده یا از منوی وضعیت مدل مسیر دلخواه را تعیین کنید."
            )
        }
    }
}

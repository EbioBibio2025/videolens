package com.example.domain.engine

import android.content.Context

/**
 * MediaPipe Universal Embedder engine adapter.
 * Wraps [RealEmbeddingGemmaEngine] to provide official on-device EmbeddingGemma 2 inference.
 */
class MediaPipeEmbeddingEngine(
    context: Context,
    customModelPath: String = RealEmbeddingGemmaEngine.getDefaultModelPath(context),
    override val dimension: Int = 256
) : MultimodalEmbeddingEngine, AutoCloseable {

    private val realEngine = RealEmbeddingGemmaEngine(context, customModelPath, dimension)

    override val engineName: String = realEngine.engineName
    override val isPhase1Mock: Boolean = false

    override suspend fun initialize(): EngineInitResult = realEngine.initialize()

    override suspend fun embedText(text: String, prefix: String): FloatArray =
        realEngine.embedText(text, prefix)

    override suspend fun embedImage(imageBytes: ByteArray): FloatArray =
        realEngine.embedImage(imageBytes)

    override suspend fun embedAudioSegment(audioSamples: FloatArray): FloatArray =
        realEngine.embedAudioSegment(audioSamples)

    override fun getRuntimeStatus(): EngineStatus = realEngine.getRuntimeStatus()

    override fun close() {
        realEngine.close()
    }
}

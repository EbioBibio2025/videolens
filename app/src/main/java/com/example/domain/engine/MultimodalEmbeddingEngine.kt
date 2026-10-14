package com.example.domain.engine

interface MultimodalEmbeddingEngine {
    val engineName: String
    val isPhase1Mock: Boolean
    val dimension: Int

    suspend fun initialize(): EngineInitResult
    suspend fun embedText(text: String, prefix: String = ""): FloatArray
    suspend fun embedImage(imageBytes: ByteArray): FloatArray
    suspend fun embedAudioSegment(audioSamples: FloatArray): FloatArray
    fun getRuntimeStatus(): EngineStatus
}

sealed class EngineStatus {
    object Ready : EngineStatus()
    data class Phase1Simulation(val notice: String) : EngineStatus()
    data class BlockedOrMissingArtifact(
        val reason: String,
        val expectedPath: String,
        val instructions: String
    ) : EngineStatus()
    data class Error(val message: String) : EngineStatus()
}

sealed class EngineInitResult {
    data class Success(val info: String) : EngineInitResult()
    data class MissingModelFile(val details: String) : EngineInitResult()
    data class Failed(val error: String) : EngineInitResult()
}

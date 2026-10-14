package com.example.domain.engine

import java.security.MessageDigest
import kotlin.math.sqrt

/**
 * Phase 1 Mock Multimodal Embedding Engine.
 * Clearly labeled for Phase 1 testing and prototyping.
 * Generates deterministic, normalized float vectors for consistent vector similarity testing.
 */
class MockMultimodalEmbeddingEngine(
    override val dimension: Int = 256
) : MultimodalEmbeddingEngine {

    override val engineName: String = "موتور آزمایشی فاز ۱ (Mock Simulation Engine)"
    override val isPhase1Mock: Boolean = true

    override suspend fun initialize(): EngineInitResult {
        return EngineInitResult.Success("موتور شبیه‌ساز فاز ۱ آماده به کار است. [داده آزمایشی]")
    }

    override suspend fun embedText(text: String, prefix: String): FloatArray {
        val fullContent = "$prefix $text".trim()
        return generateDeterministicNormalizedVector(fullContent, dimension)
    }

    override suspend fun embedImage(imageBytes: ByteArray): FloatArray {
        val digest = MessageDigest.getInstance("SHA-256").digest(imageBytes)
        val seedString = "image_embedding_" + digest.joinToString("") { "%02x".format(it) }
        return generateDeterministicNormalizedVector(seedString, dimension)
    }

    override suspend fun embedAudioSegment(audioSamples: FloatArray): FloatArray {
        var energy = 0f
        audioSamples.forEach { energy += it * it }
        val seedString = "audio_segment_len_${audioSamples.size}_energy_${energy}"
        return generateDeterministicNormalizedVector(seedString, dimension)
    }

    override fun getRuntimeStatus(): EngineStatus {
        return EngineStatus.Phase1Simulation(
            "فاز ۱: داده‌های شبیه‌سازی‌شده فعال هستند. تمام بردارها محلی و نرمالایز شده تولید می‌شوند."
        )
    }

    companion object {
        /**
         * Generates a deterministic L2-normalized vector based on string hash and semantic keywords.
         */
        fun generateDeterministicNormalizedVector(seed: String, dim: Int): FloatArray {
            val vector = FloatArray(dim)
            val lowerSeed = seed.lowercase()

            // Pseudo-semantic semantic clusters for realistic search demonstrations
            val isMeeting = lowerSeed.contains("جلسه") || lowerSeed.contains("تیم") || lowerSeed.contains("meeting") || lowerSeed.contains("پروژه")
            val isNature = lowerSeed.contains("طبیعت") || lowerSeed.contains("کوه") || lowerSeed.contains("سفر") || lowerSeed.contains("دریا") || lowerSeed.contains("nature")
            val isCode = lowerSeed.contains("کد") || lowerSeed.contains("برنامه‌نویسی") || lowerSeed.contains("android") || lowerSeed.contains("کاتلین") || lowerSeed.contains("ai")
            val isAudioSpeech = lowerSeed.contains("پادکست") || lowerSeed.contains("صوت") || lowerSeed.contains("سخنرانی") || lowerSeed.contains("صحبت")

            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(seed.toByteArray(Charsets.UTF_8))

            for (i in 0 until dim) {
                val byteVal = hash[i % hash.size].toInt()
                var value = ((byteVal * (i + 13)) % 100).toFloat() / 50f - 1f

                if (isMeeting && i in 0..30) value += 2.0f
                if (isNature && i in 31..60) value += 2.0f
                if (isCode && i in 61..90) value += 2.0f
                if (isAudioSpeech && i in 91..120) value += 2.0f

                vector[i] = value
            }

            // L2 Normalization
            var sumSquares = 0f
            for (v in vector) {
                sumSquares += v * v
            }
            val norm = sqrt(sumSquares.coerceAtLeast(1e-12f))
            for (i in 0 until dim) {
                vector[i] /= norm
            }

            return vector
        }
    }
}

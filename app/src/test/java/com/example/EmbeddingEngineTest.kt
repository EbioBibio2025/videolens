package com.example

import com.example.domain.engine.RealEmbeddingGemmaEngine
import com.example.domain.search.VectorSearchCalculator
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.retrieval.universalembedder.UniversalEmbedderOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class EmbeddingEngineTest {

    @Test
    fun testTaskPrefixFormatting() {
        val rawQuery = "جلسه برنامه نویسی اندروید"
        val formattedQuery = RealEmbeddingGemmaEngine.formatWithTaskPrefix(rawQuery, isQuery = true)
        assertEquals("task: search_query | query: جلسه برنامه نویسی اندروید", formattedQuery)

        val rawDoc = "سند معماری نرم افزار"
        val formattedDoc = RealEmbeddingGemmaEngine.formatWithTaskPrefix(rawDoc, isQuery = false)
        assertEquals("task: search_document | doc: سند معماری نرم افزار", formattedDoc)

        // Ensure idempotence if prefix already present
        val alreadyPrefixed = "task: search_query | query: تست"
        assertEquals(alreadyPrefixed, RealEmbeddingGemmaEngine.formatWithTaskPrefix(alreadyPrefixed, isQuery = true))
    }

    @Test
    fun testMatryoshkaReductionAndNormalization() {
        // Create synthetic 768-dimensional native embedding vector
        val nativeVector768 = FloatArray(768) { (it % 17).toFloat() / 10f }

        // Truncate to 256 dimensions using Matryoshka reduction
        val truncated256 = VectorSearchCalculator.truncateAndNormalize(nativeVector768, 256)
        assertEquals(256, truncated256.size)

        // Verify L2 norm equals 1.0 (re-normalized)
        var sumSquares = 0f
        for (v in truncated256) {
            sumSquares += v * v
        }
        val norm = sqrt(sumSquares)
        assertTrue("L2 norm must be approximately 1.0, was $norm", abs(norm - 1.0f) < 1e-4f)
    }

    @Test
    fun testCosineSimilarityProperties() {
        val v1 = floatArrayOf(0.6f, 0.8f)
        val v2 = floatArrayOf(0.6f, 0.8f)
        val v3 = floatArrayOf(-0.8f, 0.6f)

        // Identity
        val simIdentity = VectorSearchCalculator.cosineSimilarity(v1, v2)
        assertEquals(1.0f, simIdentity, 1e-4f)

        // Orthogonality
        val simOrthogonal = VectorSearchCalculator.cosineSimilarity(v1, v3)
        assertEquals(0.0f, simOrthogonal, 1e-4f)

        // Symmetry
        val sim1 = VectorSearchCalculator.cosineSimilarity(v1, v3)
        val sim2 = VectorSearchCalculator.cosineSimilarity(v3, v1)
        assertEquals(sim1, sim2, 1e-6f)
    }

    @Test
    fun testMediaPipeUniversalEmbedderOptions() {
        val modelPath = "models/embeddinggemma-2-text-vision-440m.litertlm"
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(modelPath)
            .build()

        val options = UniversalEmbedderOptions.builder()
            .setBaseOptions(baseOptions)
            .setL2Normalize(true)
            .build()

        assertNotNull(options)
        assertNotNull(options.baseOptions())
        assertTrue(options.l2Normalize())
    }
}

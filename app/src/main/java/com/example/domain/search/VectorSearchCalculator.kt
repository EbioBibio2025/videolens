package com.example.domain.search

import com.example.data.db.entity.MediaItemEntity
import com.example.data.db.entity.MediaSegmentEntity
import com.example.domain.model.ModalityFilter
import com.example.domain.model.SearchResultItem
import kotlin.math.roundToInt
import kotlin.math.sqrt

object VectorSearchCalculator {

    /**
     * Calculates cosine similarity between two normalized float vectors.
     * If vectors are already L2 normalized, cosine similarity equals their dot product.
     */
    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        val len = minOf(v1.size, v2.size)
        if (len == 0) return 0f

        var dot = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in 0 until len) {
            val a = v1[i]
            val b = v2[i]
            dot += a * b
            norm1 += a * a
            norm2 += b * b
        }

        val denominator = sqrt(norm1) * sqrt(norm2)
        return if (denominator < 1e-12f) 0f else (dot / denominator).coerceIn(-1f, 1f)
    }

    /**
     * Truncates a vector (e.g., Matryoshka dimension reduction) and re-normalizes it.
     */
    fun truncateAndNormalize(vector: FloatArray, targetDim: Int): FloatArray {
        val result = FloatArray(targetDim)
        val copyLen = minOf(vector.size, targetDim)
        var sumSquares = 0f
        for (i in 0 until copyLen) {
            result[i] = vector[i]
            sumSquares += result[i] * result[i]
        }
        val norm = sqrt(sumSquares.coerceAtLeast(1e-12f))
        for (i in 0 until targetDim) {
            result[i] /= norm
        }
        return result
    }

    /**
     * Converts a FloatArray to a compact comma-separated string for SQLite storage.
     */
    fun vectorToString(vector: FloatArray): String {
        val sb = StringBuilder(vector.size * 8)
        for (i in vector.indices) {
            sb.append("%.5f".format(java.util.Locale.US, vector[i]))
            if (i < vector.lastIndex) sb.append(",")
        }
        return sb.toString()
    }

    /**
     * Parses a comma-separated or JSON float list string into a FloatArray.
     */
    fun stringToVector(str: String): FloatArray {
        val cleaned = str.trim().removePrefix("[").removeSuffix("]")
        if (cleaned.isEmpty()) return FloatArray(0)
        val tokens = cleaned.split(",")
        val result = FloatArray(tokens.size)
        for (i in tokens.indices) {
            result[i] = tokens[i].trim().toFloatOrNull() ?: 0f
        }
        return result
    }

    /**
     * Performs top-K search on all media segments against the query vector.
     */
    fun search(
        queryVector: FloatArray,
        segments: List<MediaSegmentEntity>,
        mediaItemsMap: Map<Long, MediaItemEntity>,
        modalityFilter: ModalityFilter = ModalityFilter.ALL,
        minSimilarity: Float = 0.40f,
        topK: Int = 50
    ): List<SearchResultItem> {
        val results = mutableListOf<SearchResultItem>()

        for (segment in segments) {
            val media = mediaItemsMap[segment.mediaId] ?: continue

            // Filter by modality
            val matchesModality = when (modalityFilter) {
                ModalityFilter.ALL -> true
                ModalityFilter.VIDEO -> media.mediaType == "VIDEO"
                ModalityFilter.AUDIO -> media.mediaType == "AUDIO"
                ModalityFilter.IMAGE -> media.mediaType == "IMAGE"
                ModalityFilter.DOCUMENT -> media.mediaType == "DOCUMENT"
            }
            if (!matchesModality) continue

            val segmentVector = stringToVector(segment.embeddingVectorJson)
            if (segmentVector.isEmpty()) continue

            val similarity = cosineSimilarity(queryVector, segmentVector)
            if (similarity >= minSimilarity) {
                val percent = ((similarity * 100f).coerceIn(0f, 100f)).roundToInt()
                val snippet = buildSnippet(segment, media)
                results.add(
                    SearchResultItem(
                        mediaItem = media,
                        segment = segment,
                        similarityScore = similarity,
                        similarityPercent = percent,
                        matchSnippet = snippet
                    )
                )
            }
        }

        results.sortByDescending { it.similarityScore }
        return results.take(topK)
    }

    private fun buildSnippet(segment: MediaSegmentEntity, media: MediaItemEntity): String {
        return when (media.mediaType) {
            "VIDEO" -> {
                val sec = (segment.timestampMs ?: 0L) / 1000
                val min = sec / 60
                val remSec = sec % 60
                val timeLabel = "%02d:%02d".format(min, remSec)
                "لحظه ویدیو در ثانیه $timeLabel: ${segment.previewConcept.ifEmpty { "تشخیص مفهوم بصری فریم" }}"
            }
            "AUDIO" -> {
                val sec = (segment.timestampMs ?: 0L) / 1000
                val min = sec / 60
                val remSec = sec % 60
                val timeLabel = "%02d:%02d".format(min, remSec)
                "قطعه صوتی در $timeLabel: ${segment.textSnippet.ifEmpty { "گفتار یا محتوای صوتی" }}"
            }
            "DOCUMENT" -> {
                "صفحه ${segment.pageNumber ?: 1}: ${segment.textSnippet.take(80)}..."
            }
            else -> {
                segment.previewConcept.ifEmpty { "تطابق تصویر با درخواست" }
            }
        }
    }

    /**
     * Benchmark tool for evaluating cosine similarity throughput locally on the device CPU.
     */
    fun runBenchmark(vectorCount: Int = 1000, dimension: Int = 256): Pair<Long, Float> {
        val query = FloatArray(dimension) { 0.05f }
        val testPool = List(vectorCount) {
            FloatArray(dimension) { (it % 10).toFloat() / 10f }
        }

        val startTime = System.nanoTime()
        var dummySum = 0f
        for (v in testPool) {
            dummySum += cosineSimilarity(query, v)
        }
        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
        val throughputPerSec = if (elapsedMs > 0) (vectorCount.toFloat() / (elapsedMs.toFloat() / 1000f)) else 0f
        return Pair(elapsedMs, throughputPerSec)
    }
}

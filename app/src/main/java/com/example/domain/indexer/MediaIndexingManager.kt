package com.example.domain.indexer

import com.example.data.db.entity.MediaItemEntity
import com.example.data.db.entity.MediaSegmentEntity
import com.example.data.repository.MediaRepository
import com.example.domain.engine.MockMultimodalEmbeddingEngine
import com.example.domain.engine.MultimodalEmbeddingEngine
import com.example.domain.search.VectorSearchCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IndexingJobState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val currentItemName: String = "",
    val currentStep: String = "",
    val progressPercent: Int = 0,
    val processedCount: Int = 0,
    val totalCount: Int = 0
)

class MediaIndexingManager(
    private val repository: MediaRepository,
    private val embeddingEngine: MultimodalEmbeddingEngine,
    private val scope: CoroutineScope
) {
    private val _jobState = MutableStateFlow(IndexingJobState())
    val jobState: StateFlow<IndexingJobState> = _jobState.asStateFlow()

    private var activeJob: Job? = null
    private var isPauseRequested = false

    /**
     * Indexes a newly picked media item from Storage Access Framework or user import.
     */
    fun enqueueMediaForIndexing(
        name: String,
        uri: String,
        mimeType: String,
        mediaType: String,
        sizeBytes: Long,
        topicTag: String = "عمومی"
    ) {
        scope.launch(Dispatchers.IO) {
            val item = MediaItemEntity(
                uri = uri,
                fileName = name,
                mimeType = mimeType,
                mediaType = mediaType,
                fileSizeBytes = sizeBytes,
                indexingStatus = "IDLE",
                topicTag = topicTag
            )
            val mediaId = repository.insertMedia(item)
            startIndexingQueue(mediaId)
        }
    }

    fun startIndexingQueue(targetMediaId: Long? = null) {
        if (activeJob?.isActive == true && !_jobState.value.isPaused) return

        isPauseRequested = false
        activeJob = scope.launch(Dispatchers.IO) {
            _jobState.value = _jobState.value.copy(
                isRunning = true,
                isPaused = false,
                currentStep = "آماده‌سازی صف پردازش آفلاین..."
            )

            val mediaToProcess = if (targetMediaId != null) {
                listOfNotNull(repository.getMediaById(targetMediaId))
            } else {
                emptyList()
            }

            // Perform simulated/real extraction pipeline for targetMediaId if given
            if (targetMediaId != null) {
                processSingleMedia(targetMediaId)
            }

            _jobState.value = _jobState.value.copy(
                isRunning = false,
                isPaused = false,
                currentStep = "همه فایل‌ها با موفقیت ایندکس شدند.",
                progressPercent = 100
            )
        }
    }

    private suspend fun processSingleMedia(mediaId: Long) {
        if (isPauseRequested) {
            repository.updateIndexingStatus(mediaId, "PAUSED")
            _jobState.value = _jobState.value.copy(isPaused = true, currentStep = "ایندکس متوقف شد.")
            return
        }

        // Step 1: EXTRACTING
        repository.updateIndexingStatus(mediaId, "EXTRACTING")
        _jobState.value = _jobState.value.copy(
            currentStep = "در حال استخراج فریم‌های ویدئو و قطعات صوتی...",
            progressPercent = 35
        )
        delay(900) // Simulated frame/chunk extraction delay

        if (isPauseRequested) {
            repository.updateIndexingStatus(mediaId, "PAUSED")
            return
        }

        // Step 2: EMBEDDING
        repository.updateIndexingStatus(mediaId, "EMBEDDING")
        _jobState.value = _jobState.value.copy(
            currentStep = "در حال محاسبه بردارهای فاز ۱ توسط موتور برداری...",
            progressPercent = 75
        )
        delay(800)

        // Generate segments
        val segments = listOf(
            MediaSegmentEntity(
                mediaId = mediaId,
                segmentIndex = 0,
                timestampMs = 10_000L,
                previewConcept = "فریم استخراج شده اولیه",
                textSnippet = "محتوای استخراج‌شده از رسانه کاربر",
                embeddingDim = 256,
                embeddingVectorJson = VectorSearchCalculator.vectorToString(
                    MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("محتوا فریم فایل کاربر $mediaId", 256)
                )
            ),
            MediaSegmentEntity(
                mediaId = mediaId,
                segmentIndex = 1,
                timestampMs = 30_000L,
                previewConcept = "فریم میانی استخراج شده",
                textSnippet = "نمایش بخش ثانویه رسانه برای تطابق سریع",
                embeddingDim = 256,
                embeddingVectorJson = VectorSearchCalculator.vectorToString(
                    MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("فریم میانی رسانه کاربر $mediaId", 256)
                )
            )
        )
        repository.insertSegments(segments)

        // Step 3: COMPLETED
        repository.updateIndexingStatus(mediaId, "COMPLETED", indexedAt = System.currentTimeMillis())
        _jobState.value = _jobState.value.copy(
            currentStep = "ایندکس با موفقیت پایان یافت.",
            progressPercent = 100
        )
    }

    fun pauseIndexing() {
        isPauseRequested = true
        _jobState.value = _jobState.value.copy(
            isPaused = true,
            currentStep = "درخواست توقف موقت ثبت شد."
        )
    }

    fun resumeIndexing() {
        isPauseRequested = false
        _jobState.value = _jobState.value.copy(isPaused = false)
        startIndexingQueue()
    }

    fun cancelIndexing() {
        isPauseRequested = true
        activeJob?.cancel()
        _jobState.value = IndexingJobState(
            isRunning = false,
            isPaused = false,
            currentStep = "فرایند ایندکس لغو شد."
        )
    }

    fun retryFailedItem(mediaId: Long) {
        scope.launch(Dispatchers.IO) {
            repository.updateIndexingStatus(mediaId, "IDLE")
            startIndexingQueue(mediaId)
        }
    }
}

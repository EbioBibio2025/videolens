package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.entity.MediaItemEntity
import com.example.data.db.entity.MediaSegmentEntity
import com.example.data.db.entity.ModelConfigEntity
import com.example.data.repository.MediaRepository
import com.example.domain.engine.EngineStatus
import com.example.domain.engine.MediaPipeEmbeddingEngine
import com.example.domain.engine.MockMultimodalEmbeddingEngine
import com.example.domain.engine.MultimodalEmbeddingEngine
import com.example.domain.indexer.IndexingJobState
import com.example.domain.indexer.MediaIndexingManager
import com.example.domain.model.EmbeddingTaskPrefix
import com.example.domain.model.MediaType
import com.example.domain.model.ModalityFilter
import com.example.domain.model.SearchResultItem
import com.example.domain.model.TopicCluster
import com.example.domain.search.VectorSearchCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val searchQuery: String = "",
    val selectedModality: ModalityFilter = ModalityFilter.ALL,
    val selectedPrefix: EmbeddingTaskPrefix = EmbeddingTaskPrefix.TASK_SEARCH_QUERY,
    val similarityThreshold: Float = 0.40f,
    val searchResults: List<SearchResultItem> = emptyList(),
    val isSearching: Boolean = false,
    val activeTopicFilter: String? = null,
    val selectedVideo: MediaItemEntity? = null,
    val selectedVideoSegments: List<MediaSegmentEntity> = emptyList(),
    val currentMomentTimestampMs: Long? = null,
    val isPlayingMoment: Boolean = false,
    val benchmarkOutput: String? = null,
    val activeTabRoute: String = "home"
)

class VideoLensViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private var mockEngine = MockMultimodalEmbeddingEngine(dimension = 256)
    private var mediaPipeEngine = MediaPipeEmbeddingEngine(dimension = 256)
    private var currentEngine: MultimodalEmbeddingEngine = mockEngine

    val repository = MediaRepository(database, currentEngine)
    val indexingManager = MediaIndexingManager(repository, currentEngine, viewModelScope)

    val allMedia: StateFlow<List<MediaItemEntity>> = repository.allMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSegments: StateFlow<List<MediaSegmentEntity>> = repository.allSegments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMediaCount: StateFlow<Int> = repository.totalMediaCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalSegmentCount: StateFlow<Int> = repository.totalSegmentCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val distinctTopics: StateFlow<List<String>> = repository.distinctTopics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val modelConfig: StateFlow<ModelConfigEntity?> = repository.modelConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val indexingJobState: StateFlow<IndexingJobState> = indexingManager.jobState

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _engineStatus = MutableStateFlow<EngineStatus>(mockEngine.getRuntimeStatus())
    val engineStatus: StateFlow<EngineStatus> = _engineStatus.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed initial rich mock dataset for testing Phase 1
            repository.seedPhase1MockDatasetIfEmpty()
            refreshEngineStatus()
        }
    }

    fun onTabSelected(route: String) {
        _uiState.value = _uiState.value.copy(activeTabRoute = route)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onModalitySelect(filter: ModalityFilter) {
        _uiState.value = _uiState.value.copy(selectedModality = filter)
        if (_uiState.value.searchQuery.isNotBlank()) {
            performSearch()
        }
    }

    fun onPrefixSelect(prefix: EmbeddingTaskPrefix) {
        _uiState.value = _uiState.value.copy(selectedPrefix = prefix)
    }

    fun onThresholdChange(threshold: Float) {
        _uiState.value = _uiState.value.copy(similarityThreshold = threshold)
        if (_uiState.value.searchQuery.isNotBlank()) {
            performSearch()
        }
    }

    fun onTopicFilterSelect(topic: String?) {
        _uiState.value = _uiState.value.copy(activeTopicFilter = topic)
    }

    fun performSearch(overrideQuery: String? = null) {
        val query = overrideQuery ?: _uiState.value.searchQuery
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)

            withContext(Dispatchers.Default) {
                // Generate query embedding vector using active engine and selected task prefix
                val prefix = _uiState.value.selectedPrefix.prefix
                val queryVector = currentEngine.embedText(query, prefix)

                val segments = repository.getAllSegmentsSync()
                val mediaList = allMedia.value
                val mediaMap = mediaList.associateBy { it.id }

                val results = VectorSearchCalculator.search(
                    queryVector = queryVector,
                    segments = segments,
                    mediaItemsMap = mediaMap,
                    modalityFilter = _uiState.value.selectedModality,
                    minSimilarity = _uiState.value.similarityThreshold,
                    topK = 50
                )

                _uiState.value = _uiState.value.copy(
                    searchResults = results,
                    isSearching = false
                )

                repository.saveSearchLog(query, _uiState.value.selectedModality.name, results.size)
            }
        }
    }

    fun selectVideoForMoments(video: MediaItemEntity) {
        viewModelScope.launch {
            val segments = withContext(Dispatchers.IO) {
                database.segmentDao().getSegmentsForMediaSync(video.id)
            }
            val initialMoment = segments.firstOrNull()?.timestampMs ?: 0L
            _uiState.value = _uiState.value.copy(
                selectedVideo = video,
                selectedVideoSegments = segments,
                currentMomentTimestampMs = initialMoment,
                isPlayingMoment = false,
                activeTabRoute = "video_moments"
            )
        }
    }

    fun seekToMoment(timestampMs: Long) {
        _uiState.value = _uiState.value.copy(
            currentMomentTimestampMs = timestampMs,
            isPlayingMoment = true
        )
    }

    fun togglePlayMoment() {
        _uiState.value = _uiState.value.copy(
            isPlayingMoment = !_uiState.value.isPlayingMoment
        )
    }

    fun enqueueMediaFromSaf(name: String, uri: String, mime: String, size: Long, topic: String = "عمومی") {
        val mediaType = MediaType.fromMimeOrExtension(name, mime).name
        indexingManager.enqueueMediaForIndexing(
            name = name,
            uri = uri,
            mimeType = mime,
            mediaType = mediaType,
            sizeBytes = size,
            topicTag = topic
        )
    }

    fun pauseIndexing() = indexingManager.pauseIndexing()
    fun resumeIndexing() = indexingManager.resumeIndexing()
    fun cancelIndexing() = indexingManager.cancelIndexing()
    fun retryFailed(mediaId: Long) = indexingManager.retryFailedItem(mediaId)

    fun loadPhase1Samples() {
        viewModelScope.launch {
            repository.seedPhase1MockDatasetIfEmpty()
        }
    }

    fun clearLibrary() {
        viewModelScope.launch {
            repository.clearDatabase()
            _uiState.value = _uiState.value.copy(
                searchResults = emptyList(),
                selectedVideo = null,
                selectedVideoSegments = emptyList()
            )
        }
    }

    fun runCpuBenchmark() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val (ms, throughput) = VectorSearchCalculator.runBenchmark(vectorCount = 1000, dimension = 256)
                _uiState.value = _uiState.value.copy(
                    benchmarkOutput = "محاسبه ۱,۰۰۰ بردار در $ms میلی‌ثانیه | نرخ: ${throughput.toInt()} مقایسه در ثانیه روی CPU"
                )
            }
        }
    }

    fun switchEngineMode(useMock: Boolean) {
        viewModelScope.launch {
            currentEngine = if (useMock) mockEngine else mediaPipeEngine
            refreshEngineStatus()
            repository.upsertModelConfig(
                ModelConfigEntity(
                    isMockMode = useMock,
                    statusMessage = if (useMock) {
                        "فاز ۱: موتور آزمایشی شبیه‌سازی فعال است."
                    } else {
                        "موتور رسمی MediaPipe در انتظار فایل مدل روی حافظه است."
                    }
                )
            )
        }
    }

    fun refreshEngineStatus() {
        _engineStatus.value = currentEngine.getRuntimeStatus()
    }
}

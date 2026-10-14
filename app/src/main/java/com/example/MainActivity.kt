package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.VideoLensBottomBar
import com.example.ui.components.VideoLensTopBar
import com.example.ui.screens.AudioSearchScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.DocumentSearchScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IndexingQueueScreen
import com.example.ui.screens.ModelStatusScreen
import com.example.ui.screens.SearchResultsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.TopicClusteringScreen
import com.example.ui.screens.VideoMomentsScreen
import com.example.ui.theme.VideoLensTheme
import com.example.ui.viewmodel.VideoLensViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VideoLensViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoLensTheme {
                // Ensure strictly RTL (Right-to-Left) layout for Persian interface
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    VideoLensApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun VideoLensApp(viewModel: VideoLensViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaList by viewModel.allMedia.collectAsStateWithLifecycle()
    val segments by viewModel.allSegments.collectAsStateWithLifecycle()
    val totalVectors by viewModel.totalSegmentCount.collectAsStateWithLifecycle()
    val totalMedia by viewModel.totalMediaCount.collectAsStateWithLifecycle()
    val modelConfig by viewModel.modelConfig.collectAsStateWithLifecycle()
    val engineStatus by viewModel.engineStatus.collectAsStateWithLifecycle()
    val jobState by viewModel.indexingJobState.collectAsStateWithLifecycle()

    val currentTab = uiState.activeTabRoute

    // Handle system Back press to return to Home from sub-screens
    if (currentTab != "home") {
        BackHandler {
            viewModel.onTabSelected("home")
        }
    }

    val screenTitle = when (currentTab) {
        "home" -> "کتابخانه چندرسانه‌ای"
        "search" -> "جستجوی معنایی"
        "search_results" -> "نتایج تطابق برداری"
        "video_moments" -> "لحظات و فریم‌های ویدئو"
        "audio_search" -> "جستجوی قطعات صوتی"
        "document_search" -> "جستجوی اسناد و متون"
        "topics" -> "دسته‌بندی موضوعی هوشمند"
        "indexing_queue" -> "صف پردازش و استخراج"
        "model_status" -> "موتور مدل EmbeddingGemma"
        "diagnostics" -> "تنظیمات و عیب‌یابی آفلاین"
        else -> "ویدئولنز"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            VideoLensTopBar(
                title = screenTitle,
                onNavigateToIndexing = { viewModel.onTabSelected("indexing_queue") },
                onNavigateToSettings = { viewModel.onTabSelected("diagnostics") },
                isIndexingRunning = jobState.isRunning
            )
        },
        bottomBar = {
            VideoLensBottomBar(
                currentRoute = currentTab,
                onTabSelected = { route -> viewModel.onTabSelected(route) }
            )
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)

        when (currentTab) {
            "home" -> HomeScreen(
                mediaList = mediaList,
                totalVectors = totalVectors,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                onPerformSearch = {
                    viewModel.performSearch(it)
                    viewModel.onTabSelected("search")
                },
                onMediaClick = { item ->
                    when (item.mediaType) {
                        "VIDEO" -> viewModel.selectVideoForMoments(item)
                        "AUDIO" -> viewModel.onTabSelected("audio_search")
                        "DOCUMENT" -> viewModel.onTabSelected("document_search")
                        else -> {
                            viewModel.onSearchQueryChange(item.fileName)
                            viewModel.performSearch()
                            viewModel.onTabSelected("search")
                        }
                    }
                },
                onLoadMockSamples = { viewModel.loadPhase1Samples() },
                onAddFilesFromSaf = { name, uri, mime, size ->
                    viewModel.enqueueMediaFromSaf(name, uri, mime, size)
                    viewModel.onTabSelected("indexing_queue")
                },
                onNavigateToSearch = { viewModel.onTabSelected("search") },
                modifier = modifier
            )

            "search" -> SearchScreen(
                searchQuery = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                selectedModality = uiState.selectedModality,
                onModalityChange = { viewModel.onModalitySelect(it) },
                selectedPrefix = uiState.selectedPrefix,
                onPrefixChange = { viewModel.onPrefixSelect(it) },
                similarityThreshold = uiState.similarityThreshold,
                onThresholdChange = { viewModel.onThresholdChange(it) },
                searchResults = uiState.searchResults,
                isSearching = uiState.isSearching,
                onExecuteSearch = { viewModel.performSearch() },
                onResultClick = { result ->
                    if (result.mediaItem.mediaType == "VIDEO") {
                        viewModel.selectVideoForMoments(result.mediaItem)
                        result.segment.timestampMs?.let { viewModel.seekToMoment(it) }
                    } else if (result.mediaItem.mediaType == "AUDIO") {
                        viewModel.onTabSelected("audio_search")
                    } else if (result.mediaItem.mediaType == "DOCUMENT") {
                        viewModel.onTabSelected("document_search")
                    }
                },
                modifier = modifier
            )

            "search_results" -> SearchResultsScreen(
                query = uiState.searchQuery,
                results = uiState.searchResults,
                onBackClick = { viewModel.onTabSelected("search") },
                onResultClick = { result ->
                    if (result.mediaItem.mediaType == "VIDEO") {
                        viewModel.selectVideoForMoments(result.mediaItem)
                        result.segment.timestampMs?.let { viewModel.seekToMoment(it) }
                    }
                },
                modifier = modifier
            )

            "video_moments" -> VideoMomentsScreen(
                videoList = mediaList,
                selectedVideo = uiState.selectedVideo,
                segments = uiState.selectedVideoSegments.ifEmpty {
                    val activeId = (uiState.selectedVideo ?: mediaList.firstOrNull { it.mediaType == "VIDEO" })?.id ?: -1L
                    segments.filter { it.mediaId == activeId }
                },
                currentTimestampMs = uiState.currentMomentTimestampMs,
                isPlaying = uiState.isPlayingMoment,
                onSelectVideo = { viewModel.selectVideoForMoments(it) },
                onSeekToTimestamp = { viewModel.seekToMoment(it) },
                onTogglePlay = { viewModel.togglePlayMoment() },
                modifier = modifier
            )

            "audio_search" -> AudioSearchScreen(
                audioList = mediaList.filter { it.mediaType == "AUDIO" },
                segments = segments,
                onPerformSearch = { viewModel.onSearchQueryChange(it); viewModel.performSearch() },
                modifier = modifier
            )

            "document_search" -> DocumentSearchScreen(
                docList = mediaList.filter { it.mediaType == "DOCUMENT" },
                segments = segments,
                onPerformSearch = { viewModel.onSearchQueryChange(it); viewModel.performSearch() },
                modifier = modifier
            )

            "topics" -> TopicClusteringScreen(
                mediaList = mediaList,
                onMediaClick = { item ->
                    if (item.mediaType == "VIDEO") {
                        viewModel.selectVideoForMoments(item)
                    } else {
                        viewModel.onSearchQueryChange(item.topicTag)
                        viewModel.performSearch()
                        viewModel.onTabSelected("search")
                    }
                },
                modifier = modifier
            )

            "indexing_queue" -> IndexingQueueScreen(
                jobState = jobState,
                mediaList = mediaList,
                onPause = { viewModel.pauseIndexing() },
                onResume = { viewModel.resumeIndexing() },
                onCancel = { viewModel.cancelIndexing() },
                onRetry = { viewModel.retryFailed(it) },
                onStartIndexingAll = { viewModel.indexingManager.startIndexingQueue() },
                modifier = modifier
            )

            "model_status" -> ModelStatusScreen(
                modelConfig = modelConfig,
                engineStatus = engineStatus,
                onToggleMockMode = { viewModel.switchEngineMode(it) },
                onRefreshStatus = { viewModel.refreshEngineStatus() },
                modifier = modifier
            )

            "diagnostics" -> DiagnosticsScreen(
                totalMediaCount = totalMedia,
                totalSegmentCount = totalVectors,
                benchmarkResult = uiState.benchmarkOutput,
                onRunBenchmark = { viewModel.runCpuBenchmark() },
                onClearDatabase = { viewModel.clearLibrary() },
                onLoadMockData = { viewModel.loadPhase1Samples() },
                modifier = modifier
            )
        }
    }
}

package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.entity.MediaItemEntity
import com.example.data.db.entity.MediaSegmentEntity
import com.example.data.db.entity.ModelConfigEntity
import com.example.data.db.entity.SearchLogEntity
import com.example.domain.engine.MockMultimodalEmbeddingEngine
import com.example.domain.engine.MultimodalEmbeddingEngine
import com.example.domain.search.VectorSearchCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MediaRepository(
    private val database: AppDatabase,
    private val embeddingEngine: MultimodalEmbeddingEngine
) {
    private val mediaDao = database.mediaDao()
    private val segmentDao = database.segmentDao()
    private val searchLogDao = database.searchLogDao()
    private val modelConfigDao = database.modelConfigDao()

    val allMedia: Flow<List<MediaItemEntity>> = mediaDao.getAllMedia()
    val allSegments: Flow<List<MediaSegmentEntity>> = segmentDao.getAllSegments()
    val distinctTopics: Flow<List<String>> = mediaDao.getDistinctTopics()
    val totalMediaCount: Flow<Int> = mediaDao.getMediaCount()
    val totalSegmentCount: Flow<Int> = segmentDao.getTotalSegmentCount()
    val recentSearchLogs: Flow<List<SearchLogEntity>> = searchLogDao.getRecentLogs()
    val modelConfig: Flow<ModelConfigEntity?> = modelConfigDao.getConfig()

    fun getMediaById(id: Long): Flow<MediaItemEntity?> = mediaDao.getMediaById(id)
    fun getSegmentsForMedia(mediaId: Long): Flow<List<MediaSegmentEntity>> = segmentDao.getSegmentsForMedia(mediaId)

    suspend fun insertMedia(item: MediaItemEntity): Long = withContext(Dispatchers.IO) {
        mediaDao.insertMedia(item)
    }

    suspend fun insertSegments(segments: List<MediaSegmentEntity>) = withContext(Dispatchers.IO) {
        segmentDao.insertSegments(segments)
    }

    suspend fun updateIndexingStatus(id: Long, status: String, error: String? = null, indexedAt: Long? = null) = withContext(Dispatchers.IO) {
        mediaDao.updateIndexingStatus(id, status, error, indexedAt)
    }

    suspend fun deleteMedia(item: MediaItemEntity) = withContext(Dispatchers.IO) {
        // Never deletes original file on disk; only deletes database index records
        mediaDao.deleteMedia(item)
    }

    suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        segmentDao.clearAllSegments()
        mediaDao.clearAllMedia()
        searchLogDao.clearLogs()
    }

    suspend fun saveSearchLog(query: String, modality: String, matchCount: Int) = withContext(Dispatchers.IO) {
        searchLogDao.insertLog(
            SearchLogEntity(
                query = query,
                modalityFilter = modality,
                matchCount = matchCount
            )
        )
    }

    suspend fun upsertModelConfig(config: ModelConfigEntity) = withContext(Dispatchers.IO) {
        modelConfigDao.upsertConfig(config)
    }

    suspend fun getAllSegmentsSync(): List<MediaSegmentEntity> = withContext(Dispatchers.IO) {
        segmentDao.getAllSegmentsSync()
    }

    /**
     * Seeds initial rich multimodal mock dataset for Phase 1 testing if library is empty.
     */
    suspend fun seedPhase1MockDatasetIfEmpty() = withContext(Dispatchers.IO) {
        val currentSegments = segmentDao.getAllSegmentsSync()
        if (currentSegments.isNotEmpty()) return@withContext

        // 1. Video 1: جلسه فنی هوش مصنوعی و معماری نرم‌افزار
        val video1Id = mediaDao.insertMedia(
            MediaItemEntity(
                uri = "content://media/external/video/media/101",
                fileName = "AI_Architecture_Team_Sync.mp4",
                mimeType = "video/mp4",
                mediaType = "VIDEO",
                fileSizeBytes = 450 * 1024 * 1024L,
                durationMs = 180_000L, // 3 minutes
                indexingStatus = "COMPLETED",
                indexedAt = System.currentTimeMillis() - 3600_000,
                topicTag = "فناوری و هوش مصنوعی",
                thumbnailUri = null
            )
        )
        val video1Segments = listOf(
            MediaSegmentEntity(
                mediaId = video1Id,
                segmentIndex = 0,
                timestampMs = 15_000L,
                previewConcept = "تخته سفید و دیاگرام معماری مدل EmbeddingGemma",
                textSnippet = "بررسی جریان پردازش بردارها و کاهش بعد در معماری موبایل",
                embeddingDim = 256,
                embeddingVectorJson = VectorSearchCalculator.vectorToString(
                    MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("جلسه تخته سفید معماری مدل هوش مصنوعی کدهای اندروید", 256)
                )
            ),
            MediaSegmentEntity(
                mediaId = video1Id,
                segmentIndex = 1,
                timestampMs = 55_000L,
                previewConcept = "ارائه اسلایدهای بنچمارک عملکرد LiteRT و MediaPipe",
                textSnippet = "مقایسه سرعت استنتاج برداری روی پردازنده اسنپ‌دراگون و تنسور",
                embeddingDim = 256,
                embeddingVectorJson = VectorSearchCalculator.vectorToString(
                    MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("بنچمارک سرعت عملکرد سخت‌افزار پردازنده هوش مصنوعی", 256)
                )
            ),
            MediaSegmentEntity(
                mediaId = video1Id,
                segmentIndex = 2,
                timestampMs = 120_000L,
                previewConcept = "دموی زنده جستجوی ویدئو با لحظات زمانی",
                textSnippet = "نمایش جهش به ثانیه‌های منطبق در پخش‌کننده ویدئولنز",
                embeddingDim = 256,
                embeddingVectorJson = VectorSearchCalculator.vectorToString(
                    MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("دموی زنده جستجوی ویدئو لحظات زمانی پخش ویدئولنز", 256)
                )
            )
        )
        segmentDao.insertSegments(video1Segments)

        // 2. Video 2: مستند سفر کوهستان دماوند و طبیعت
        val video2Id = mediaDao.insertMedia(
            MediaItemEntity(
                uri = "content://media/external/video/media/102",
                fileName = "Damavand_Trek_Documentary.mp4",
                mimeType = "video/mp4",
                mediaType = "VIDEO",
                fileSizeBytes = 720 * 1024 * 1024L,
                durationMs = 240_000L,
                indexingStatus = "COMPLETED",
                indexedAt = System.currentTimeMillis() - 7200_000,
                topicTag = "طبیعت و سفر"
            )
        )
        val video2Segments = listOf(
            MediaSegmentEntity(
                mediaId = video2Id,
                segmentIndex = 0,
                timestampMs = 25_000L,
                previewConcept = "چشم‌انداز قله برفی و ابرها در طلوع خورشید",
                textSnippet = "نمای هوایی از دامنه‌های سرسبز و قله پوشیده از برف دماوند",
                embeddingDim = 256,
                embeddingVectorJson = VectorSearchCalculator.vectorToString(
                    MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("طبیعت کوهستان قله دماوند برف طلوع سفر کوهنوردی", 256)
                )
            ),
            MediaSegmentEntity(
                mediaId = video2Id,
                segmentIndex = 1,
                timestampMs = 95_000L,
                previewConcept = "برپایی چادر کمپ و آتش شبانه در پناهگاه",
                textSnippet = "تیم کوهنوردی در حال استراحت و چای در کنار کمپ",
                embeddingDim = 256,
                embeddingVectorJson = VectorSearchCalculator.vectorToString(
                    MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("کمپ چادر آتش شبانه استراحت تیم کوهنوردی سفر طبیعت", 256)
                )
            )
        )
        segmentDao.insertSegments(video2Segments)

        // 3. Audio: پادکست رادیو فناوری — گفتار درباره حریم خصوصی و پردازش محلی
        val audio1Id = mediaDao.insertMedia(
            MediaItemEntity(
                uri = "content://media/external/audio/media/201",
                fileName = "Tech_Radio_Ep42_Privacy_AI.mp3",
                mimeType = "audio/mpeg",
                mediaType = "AUDIO",
                fileSizeBytes = 35 * 1024 * 1024L,
                durationMs = 360_000L,
                indexingStatus = "COMPLETED",
                indexedAt = System.currentTimeMillis() - 10000_000,
                topicTag = "فناوری و هوش مصنوعی"
            )
        )
        val audio1Segments = listOf(
            MediaSegmentEntity(
                mediaId = audio1Id,
                segmentIndex = 0,
                timestampMs = 40_000L,
                previewConcept = "قطعه صوتی ۱۶kHz: مفهوم هوش مصنوعی لبه (Edge AI)",
                textSnippet = "چرا پردازش بدون اینترنت در دستگاه‌های موبایل امنیت کاربر را تضمین می‌کند؟",
                embeddingDim = 256,
                embeddingVectorJson = VectorSearchCalculator.vectorToString(
                    MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("پادکست صوتی سخنرانی امنیت حریم خصوصی پردازش محلی هوش مصنوعی", 256)
                )
            ),
            MediaSegmentEntity(
                mediaId = audio1Id,
                segmentIndex = 1,
                timestampMs = 180_000L,
                previewConcept = "قطعه صوتی ۱۶kHz: نحوه کار بردارهای معنایی و دیتابیس محلی",
                textSnippet = "توضیح ریاضی ضرب داخلی بردارها و فاصله کسینوسی برای جستجوی فوری",
                embeddingDim = 256,
                embeddingVectorJson = VectorSearchCalculator.vectorToString(
                    MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("پادکست صوت توضیح ریاضی ضرب داخلی فاصله کسینوسی بردار معنایی", 256)
                )
            )
        )
        segmentDao.insertSegments(audio1Segments)

        // 4. Document: کتابچه راهنمای توسعه اندروید و Jetpack Compose
        val doc1Id = mediaDao.insertMedia(
            MediaItemEntity(
                uri = "content://media/external/file/doc/301",
                fileName = "Android_Compose_Architecture_Guide.pdf",
                mimeType = "application/pdf",
                mediaType = "DOCUMENT",
                fileSizeBytes = 12 * 1024 * 1024L,
                pageCount = 38,
                indexingStatus = "COMPLETED",
                indexedAt = System.currentTimeMillis() - 15000_000,
                topicTag = "آموزش و پژوهش"
            )
        )
        val doc1Segments = listOf(
            MediaSegmentEntity(
                mediaId = doc1Id,
                segmentIndex = 0,
                pageNumber = 4,
                previewConcept = "فصل دوم: مدیریت وضعیت با StateFlow و Coroutines",
                textSnippet = "در Jetpack Compose برای جلوگیری از بازتولیدهای غیرضروری از remember و StateFlow استفاده نمایید.",
                embeddingDim = 256,
                embeddingVectorJson = VectorSearchCalculator.vectorToString(
                    MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("آموزش کد کاتلین اندروید کامپوز معماری stateflow coroutines سند pdf", 256)
                )
            ),
            MediaSegmentEntity(
                mediaId = doc1Id,
                segmentIndex = 1,
                pageNumber = 18,
                previewConcept = "فصل پنجم: ادغام دیتابیس Room با بردارهای باینری",
                textSnippet = "ذخیره‌سازی سریع بردارهای چندبعدی در جداول SQLite و بازیابی با اندیس زمانی.",
                embeddingDim = 256,
                embeddingVectorJson = VectorSearchCalculator.vectorToString(
                    MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("آموزش کد برنامه نویسی دیتابیس room بردار چند بعدی کتابخانه سند pdf", 256)
                )
            )
        )
        segmentDao.insertSegments(doc1Segments)

        // 5. Image: دیاگرام سیستم جستجوی ویدیولنز
        val img1Id = mediaDao.insertMedia(
            MediaItemEntity(
                uri = "content://media/external/images/media/401",
                fileName = "VideoLens_System_Pipeline.png",
                mimeType = "image/png",
                mediaType = "IMAGE",
                fileSizeBytes = 4 * 1024 * 1024L,
                indexingStatus = "COMPLETED",
                indexedAt = System.currentTimeMillis() - 5000_000,
                topicTag = "فناوری و هوش مصنوعی"
            )
        )
        val img1Segments = listOf(
            MediaSegmentEntity(
                mediaId = img1Id,
                segmentIndex = 0,
                previewConcept = "اینفوگرافیک معماری خط لوله جستجوی معنایی محلی",
                textSnippet = "شمای گرافیکی استخراج فریم‌ها، تولید بردار Embedding و مقایسه شباهت کسینوسی در گوشی",
                embeddingDim = 256,
                embeddingVectorJson = VectorSearchCalculator.vectorToString(
                    MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("اینفوگرافیک تصویر معماری جستجوی معنایی هوش مصنوعی خط لوله", 256)
                )
            )
        )
        segmentDao.insertSegments(img1Segments)
    }
}

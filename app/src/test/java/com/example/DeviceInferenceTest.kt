package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.domain.engine.EngineInitResult
import com.example.domain.engine.EngineStatus
import com.example.domain.engine.MockMultimodalEmbeddingEngine
import com.example.domain.engine.RealEmbeddingGemmaEngine
import com.google.mediapipe.framework.image.BitmapImageBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceInferenceTest {

    @Test
    fun testRealEngineStatusWithoutModelDoesNotFakeResults() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = RealEmbeddingGemmaEngine(context, "/non/existent/path/model.litertlm")

        val initResult = engine.initialize()
        assertTrue("Init result must report MissingModelFile when file is absent", initResult is EngineInitResult.MissingModelFile)

        val status = engine.getRuntimeStatus()
        assertTrue("Status must be BlockedOrMissingArtifact when file is absent", status is EngineStatus.BlockedOrMissingArtifact)

        assertFalse("Real engine must never be flagged as Phase 1 mock", engine.isPhase1Mock)

        // Verifying requirement: Never silently substitute fake embeddings
        try {
            engine.embedText("تست فارسی")
            fail("embedText must throw IllegalStateException if model is not loaded, never return fake vectors silently.")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Universal Embedder") == true)
        }
    }

    @Test
    fun testMockEngineIsolation() {
        val mockEngine = MockMultimodalEmbeddingEngine(256)
        assertTrue("Mock engine must be explicitly marked as Phase 1 mock", mockEngine.isPhase1Mock)
        assertTrue("Status must indicate simulation mode", mockEngine.getRuntimeStatus() is EngineStatus.Phase1Simulation)
    }

    @Test
    fun testLocalImagePreprocessingBitmap() {
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        assertNotNull(bitmap)

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageBytes = stream.toByteArray()
        assertTrue("Compressed image bytes must be non-empty", imageBytes.isNotEmpty())

        val mpImage = BitmapImageBuilder(bitmap).build()
        assertNotNull(mpImage)
        assertEquals(128, mpImage.width)
        assertEquals(128, mpImage.height)
    }
}

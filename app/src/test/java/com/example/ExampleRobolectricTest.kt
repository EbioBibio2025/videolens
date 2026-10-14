package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.engine.MockMultimodalEmbeddingEngine
import com.example.domain.search.VectorSearchCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ویدئولنز", appName)
  }

  @Test
  fun `test vector cosine similarity`() {
    val v1 = MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("جلسه فنی هوش مصنوعی", 256)
    val v2 = MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("جلسه پروژه و تخته سفید", 256)
    val v3 = MockMultimodalEmbeddingEngine.generateDeterministicNormalizedVector("طبیعت کوهستان و برف", 256)

    val simMeeting = VectorSearchCalculator.cosineSimilarity(v1, v2)
    val simDiff = VectorSearchCalculator.cosineSimilarity(v1, v3)

    // Meeting should have higher similarity with meeting than with mountains
    assertTrue("Similarity between meetings ($simMeeting) should exceed meeting-nature ($simDiff)", simMeeting > simDiff)
  }
}

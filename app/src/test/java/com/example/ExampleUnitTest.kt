package com.example

import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.retrieval.universalembedder.UniversalEmbedderOptions
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testUniversalEmbedderOptionsBuilder() {
    val baseOptions = BaseOptions.builder()
      .setModelAssetPath("embeddinggemma-2-text-vision-440m.litertlm")
      .build()
    val options = UniversalEmbedderOptions.builder()
      .setBaseOptions(baseOptions)
      .setL2Normalize(true)
      .build()
    assertNotNull(options)
    assertNotNull(options.baseOptions())
  }
}

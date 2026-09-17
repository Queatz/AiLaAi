package com.queatz.api

import com.queatz.qwenFlashModel
import com.queatz.qwenTranscribeRequestBody
import com.queatz.qwenTranscriptFromResponse
import kotlinx.serialization.json.jsonObject
import com.queatz.plugins.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QwenTranscriptionTest {
    @Test
    fun readsTranscriptFromQwenCompatibleResponse() {
        val response = """
            {
              "output": {
                "choices": [
                  {
                    "message": {
                      "content": [
                        {
                          "text": "  Remind me to call Mum tomorrow.  "
                        }
                      ]
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        assertEquals(
            expected = "Remind me to call Mum tomorrow.",
            actual = qwenTranscriptFromResponse(response),
        )
    }

    @Test
    fun readsTranscriptFromFlashOutputText() {
        val response = """
            {
              "output": {
                "text": "  Buy milk after work.  "
              }
            }
        """.trimIndent()

        assertEquals(
            expected = "Buy milk after work.",
            actual = qwenTranscriptFromResponse(response),
        )
    }

    @Test
    fun rejectsBlankAndMalformedQwenResponses() {
        assertNull(qwenTranscriptFromResponse("""{"output":{"choices":[{"message":{"content":[{"text":" "}]}}]}}"""))
        assertNull(qwenTranscriptFromResponse("not json"))
        assertNull(qwenTranscriptFromResponse("""{"output":{"choices":[]}}"""))
    }

    @Test
    fun transcribeRequestBodyIsAJsonObjectForTheFlashModel() {
        val body = qwenTranscribeRequestBody(
            audioBase64 = "AAA",
            language = "vi",
        )
        val parsed = json.parseToJsonElement(body).jsonObject

        assertEquals(
            expected = qwenFlashModel,
            actual = parsed["model"]?.toString()?.trim('"'),
        )
        assertTrue(body.startsWith("{"))
        assertTrue("qwen-audio-3.0-asr-flash" in body)
        assertTrue("language_hints" in body)
        assertTrue("data:audio/wav;base64,AAA" in body)
    }
}

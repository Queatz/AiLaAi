package com.queatz.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun rejectsBlankAndMalformedQwenResponses() {
        assertNull(qwenTranscriptFromResponse("""{"output":{"choices":[{"message":{"content":[{"text":" "}]}}]}}"""))
        assertNull(qwenTranscriptFromResponse("not json"))
        assertNull(qwenTranscriptFromResponse("""{"output":{"choices":[]}}"""))
    }
}
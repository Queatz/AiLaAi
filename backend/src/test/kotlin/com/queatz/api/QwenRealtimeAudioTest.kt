package com.queatz.api

import com.queatz.PcmAudioBatcher
import com.queatz.QwenStreamingEvent
import com.queatz.QwenTranscriptAccumulator
import com.queatz.qwenFinishTaskJson
import com.queatz.qwenRunTaskJson
import com.queatz.qwenStreamingEvent
import com.queatz.qwenStreamingModel
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class QwenRealtimeAudioTest {
    @Test
    fun realtimeBridgeUsesTheInferenceStreamingEndpoint() {
        val routeSource = File("src/main/kotlin/com/queatz/api/AiRoutes.kt").readText()

        assertTrue(routeSource.contains("/api-ws/v1/inference"))
        assertTrue(routeSource.contains("qwenStreamingModel"))
        assertTrue(routeSource.contains("Frame.Binary(fin = true, data = audio)"))
        assertFalse(routeSource.contains("/api-ws/v1/realtime"))
        assertFalse(routeSource.contains("qwen3-asr-flash-realtime"))
        assertFalse(routeSource.contains("input_audio_buffer.append"))
    }

    @Test
    fun streamingModelAndTaskPayloadUseTheFlashStreamingApi() {
        val runTask = qwenRunTaskJson(
            taskId = "task-1",
            language = "en",
        )
        val finishTask = qwenFinishTaskJson(
            taskId = "task-1",
        )

        assertEquals(
            expected = "qwen-audio-3.0-asr-flash-streaming",
            actual = qwenStreamingModel,
        )
        assertTrue("run-task" in runTask)
        assertTrue(qwenStreamingModel in runTask)
        assertTrue("\"format\":\"pcm\"" in runTask.replace(" ", ""))
        assertTrue("language_hints" in runTask)
        assertTrue("finish-task" in finishTask)
        assertTrue("task-1" in finishTask)
    }

    @Test
    fun parsesIncrementalAndFinalStreamingTranscripts() {
        val partial = qwenStreamingEvent(
            text = """
                {
                  "header": { "event": "result-generated" },
                  "payload": {
                    "output": {
                      "sentence": {
                        "text": "call mum",
                        "sentence_end": false,
                        "heartbeat": false
                      }
                    }
                  }
                }
            """.trimIndent(),
        )
        val completed = qwenStreamingEvent(
            text = """
                {
                  "header": { "event": "result-generated" },
                  "payload": {
                    "output": {
                      "sentence": {
                        "text": "call mum tomorrow",
                        "sentence_end": true
                      }
                    }
                  }
                }
            """.trimIndent(),
        )
        val heartbeat = qwenStreamingEvent(
            text = """
                {
                  "header": { "event": "result-generated" },
                  "payload": {
                    "output": {
                      "sentence": {
                        "text": "",
                        "heartbeat": true
                      }
                    }
                  }
                }
            """.trimIndent(),
        )

        val partialEvent = assertIs<QwenStreamingEvent.Transcript>(partial)
        assertEquals("call mum", partialEvent.text)
        assertFalse(partialEvent.isEnd)

        val completedEvent = assertIs<QwenStreamingEvent.Transcript>(completed)
        assertEquals("call mum tomorrow", completedEvent.text)
        assertTrue(completedEvent.isEnd)

        assertIs<QwenStreamingEvent.Ignored>(heartbeat)
        assertIs<QwenStreamingEvent.TaskStarted>(
            qwenStreamingEvent("""{"header":{"event":"task-started"}}""")
        )
        assertIs<QwenStreamingEvent.TaskFinished>(
            qwenStreamingEvent("""{"header":{"event":"task-finished"}}""")
        )
    }

    @Test
    fun accumulatorJoinsCompletedSentencesWithTheCurrentPartial() {
        val accumulator = QwenTranscriptAccumulator()

        assertEquals(
            expected = "Buy",
            actual = accumulator.append(
                text = "Buy",
                isEnd = false,
            ),
        )
        assertEquals(
            expected = "Buy milk",
            actual = accumulator.append(
                text = "Buy milk",
                isEnd = true,
            ),
        )
        assertEquals(
            expected = "Buy milk after work",
            actual = accumulator.append(
                text = "after work",
                isEnd = false,
            ),
        )
    }

    @Test
    fun realtimeBridgeBatchesAudioBeforeSendingItToQwen() {
        val routeSource = File("src/main/kotlin/com/queatz/api/AiRoutes.kt").readText()

        assertTrue(routeSource.contains("PcmAudioBatcher"))
        assertFalse(routeSource.contains("encoder.encodeToString(frame.data)"))
    }

    @Test
    fun audioBatcherPreservesFullChunksAndFlushesTheFinalPartialChunk() {
        val batcher = PcmAudioBatcher(
            chunkSize = 4,
        )

        assertEquals(
            expected = emptyList(),
            actual = batcher.append(
                audio = byteArrayOf(1, 2),
            ),
        )
        assertEquals(
            expected = listOf(byteArrayOf(1, 2, 3, 4).toList()),
            actual = batcher.append(
                audio = byteArrayOf(3, 4, 5),
            ).map(ByteArray::toList),
        )
        assertEquals(
            expected = byteArrayOf(5).toList(),
            actual = batcher.flush()?.toList(),
        )
        assertEquals(
            expected = null,
            actual = batcher.flush(),
        )
    }
}

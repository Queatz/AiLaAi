package com.queatz.api

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QwenRealtimeAudioTest {
    @Test
    fun realtimeBridgeUsesTheDashScopeRealtimeEndpointInsteadOfTheDeploymentHost() {
        val routeSource = File("src/main/kotlin/com/queatz/api/AiRoutes.kt").readText()

        assertTrue(
            routeSource.contains(
                "private const val qwenRealtimeHost = \"dashscope.aliyuncs.com\"",
            ),
        )
        assertTrue(
            routeSource.contains(
                "host = qwenRealtimeHost",
            ),
        )
        assertFalse(
            routeSource.contains(
                "host = secrets.qwen!!.host",
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
package com.queatz.ailaai.services

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import app.ailaai.api.aiAssistantTranscribe
import app.ailaai.api.newReminder
import com.queatz.ailaai.R
import com.queatz.ailaai.appLanguage
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.toast
import com.queatz.db.Reminder
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

val assistant by lazy {
    Assistant()
}

class Assistant {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val recording = AtomicBoolean(false)
    private var job: Job? = null

    val isListening = MutableStateFlow(false)
    val speechText = MutableStateFlow("")

    fun start(
        context: Context,
    ) {
        if (job?.isActive == true) return

        val appContext = context.applicationContext
        speechText.value = appContext.getString(R.string.listening)
        isListening.value = true
        recording.set(true)

        job = scope.launch {
            try {
                runSession(
                    context = appContext,
                )
            } finally {
                recording.set(false)
                isListening.value = false
            }
        }
    }

    fun stop() {
        recording.set(false)
    }

    @SuppressLint("MissingPermission")
    private suspend fun runSession(
        context: Context,
    ) {
        val languageCode = if (appLanguage?.startsWith("vi") == true) "vi" else "en"
        val recordedAudio = ByteArrayOutputStream()
        val transcript = AtomicReference("")

        val sampleRate = 16_000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBufferSize.coerceAtLeast(3_200 * 2)
        )

        val audioChannel = Channel<ByteArray>(Channel.UNLIMITED)

        try {
            audioRecord.startRecording()

            val recordJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(3_200)
                try {
                    while (recording.get() && isActive) {
                        val read = audioRecord.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            val data = buffer.copyOf(read)
                            recordedAudio.write(data)
                            audioChannel.send(data)
                        }
                    }
                } finally {
                    audioChannel.close()
                }
            }

            val streamJob = scope.launch(Dispatchers.IO) {
                try {
                    streamRealtime(
                        language = languageCode,
                        audio = audioChannel,
                        onTranscript = { text ->
                            transcript.set(text)
                            speechText.value = text
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            recordJob.join()
            withTimeoutOrNull(15.seconds) {
                streamJob.join()
            } ?: streamJob.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                audioRecord.stop()
                audioRecord.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        var finalTranscript = transcript.get()
        if (finalTranscript.isBlank()) {
            finalTranscript = fallbackTranscribe(
                audio = recordedAudio.toByteArray(),
                language = languageCode,
            ).orEmpty()
            if (finalTranscript.isNotBlank()) {
                speechText.value = finalTranscript
            }
        }

        if (finalTranscript.isNotBlank()) {
            createReminder(
                context = context,
                title = finalTranscript,
            )
        }
    }

    private suspend fun streamRealtime(
        language: String,
        audio: ReceiveChannel<ByteArray>,
        onTranscript: (String) -> Unit,
    ) {
        val wsUrl = api.baseUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ai/assistant"
        val token = api.token()

        voiceAssistantClient.webSocket(
            urlString = "$wsUrl?language=$language",
            request = {
                if (token != null) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        ) {
            val sendJob = launch {
                for (data in audio) {
                    send(Frame.Binary(fin = true, data = data))
                }
            }
            val receiveJob = launch {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (text.isNotBlank()) {
                            onTranscript(text)
                        }
                    }
                }
            }

            sendJob.join()
            close(
                reason = CloseReason(
                    code = CloseReason.Codes.NORMAL,
                    message = "done"
                )
            )
            withTimeoutOrNull(10.seconds) {
                receiveJob.join()
            }
        }
    }

    private suspend fun fallbackTranscribe(
        audio: ByteArray,
        language: String,
    ): String? {
        if (audio.isEmpty()) return null

        var transcript: String? = null
        api.aiAssistantTranscribe(
            audio = pcm16ToWav(
                audio = audio,
            ),
            language = language,
            onError = {
                it.printStackTrace()
            }
        ) {
            transcript = it.text
        }
        return transcript?.trim()?.takeIf { it.isNotBlank() }
    }

    private suspend fun createReminder(
        context: Context,
        title: String,
    ) {
        val startInstant = Clock.System.now().plus(1.hours)
        val zone = TimeZone.currentSystemDefault()
        val reminder = Reminder(
            title = title,
            start = startInstant,
            timezone = zone.id,
            utcOffset = zone.offsetAt(Clock.System.now()).totalSeconds / (60.0 * 60.0)
        )

        api.newReminder(
            reminder = reminder,
            onError = {
                it.printStackTrace()
            }
        ) {
            withContext(Dispatchers.Main) {
                context.toast(R.string.reminder_created)
            }
        }
    }
}

private val voiceAssistantClient by lazy {
    HttpClient(OkHttp) {
        install(WebSockets)
    }
}

private fun pcm16ToWav(
    audio: ByteArray,
    sampleRate: Int = 16_000,
): ByteArray {
    val header = ByteBuffer
        .allocate(44)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put("RIFF".toByteArray())
        .putInt(36 + audio.size)
        .put("WAVE".toByteArray())
        .put("fmt ".toByteArray())
        .putInt(16)
        .putShort(1)
        .putShort(1)
        .putInt(sampleRate)
        .putInt(sampleRate * 2)
        .putShort(2)
        .putShort(16)
        .put("data".toByteArray())
        .putInt(audio.size)
        .array()

    return header + audio
}

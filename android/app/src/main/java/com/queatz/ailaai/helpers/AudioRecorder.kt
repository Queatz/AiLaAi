package com.queatz.ailaai.helpers

import android.Manifest
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.queatz.ailaai.ui.permission.permissionRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import java.io.File
import java.io.IOException

internal enum class AudioRecorderControlEvent {
    Record,
    Cancel,
    Send
}


class AudioRecorderControl internal constructor(
    private val scope: CoroutineScope,
    private val events: MutableSharedFlow<AudioRecorderControlEvent>,
) {
    fun cancelRecording() {
        scope.launch { events.emit(AudioRecorderControlEvent.Cancel) }
    }

    fun sendActiveRecording() {
        scope.launch { events.emit(AudioRecorderControlEvent.Send) }
    }

    fun recordAudio() {
        scope.launch { events.emit(AudioRecorderControlEvent.Record) }
    }
}

@Composable
fun audioRecorder(
    onIsRecordingAudio: (Boolean) -> Unit,
    onRecordingAudioDuration: (Long) -> Unit,
    onPermissionDenied: () -> Unit,
    onFailed: suspend (File) -> Unit,
    onAudio: suspend (File) -> Boolean
): AudioRecorderControl {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentOnIsRecordingAudio by rememberUpdatedState(onIsRecordingAudio)
    val currentOnRecordingAudioDuration by rememberUpdatedState(onRecordingAudioDuration)
    val currentOnPermissionDenied by rememberUpdatedState(onPermissionDenied)
    val currentOnFailed by rememberUpdatedState(onFailed)
    val currentOnAudio by rememberUpdatedState(onAudio)

    val recordAudioPermissionRequester = permissionRequester(Manifest.permission.RECORD_AUDIO)
    var audioOutputFile by remember { mutableStateOf<File?>(null) }
    var audioRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var trackDurationJob by remember { mutableStateOf<Job?>(null) }
    val events = remember {
        MutableSharedFlow<AudioRecorderControlEvent>()
    }

    fun ensureAudioRecorder(): MediaRecorder {
        if (audioRecorder == null) {
            audioRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
        }

        return audioRecorder!!
    }

    fun prepareRecorder(): MediaRecorder {
        return ensureAudioRecorder().apply {
            reset()
            audioOutputFile = File.createTempFile("audio", ".mp4", context.cacheDir).apply {
                if (exists()) {
                    delete()
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setPreferredMicrophoneDirection(MediaRecorder.MIC_DIRECTION_TOWARDS_USER)
            }
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(64000)
            setOutputFile(audioOutputFile)

            setOnErrorListener { mr, what, extra ->
                Log.w("MediaRecorder", "error: $what, extra = $extra")
            }
            try {
                prepare()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun recordTheAudio() {
        currentOnIsRecordingAudio(true)
        try {
            prepareRecorder().start()
            trackDurationJob = scope.launch {
                val start = Clock.System.now().toEpochMilliseconds()
                while (true) {
                    delay(100)
                    currentOnRecordingAudioDuration(Clock.System.now().toEpochMilliseconds() - start)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            currentOnIsRecordingAudio(false)
        }
    }

    fun recordAudio() {
        recordAudioPermissionRequester.use(
            onPermanentlyDenied = {
                currentOnPermissionDenied()
            }
        ) {
            recordTheAudio()
        }
    }

    fun stopRecording() {
        trackDurationJob?.cancel()

        try {
            audioRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            currentOnIsRecordingAudio(false)
        }
    }

    fun cancelRecording() {
        stopRecording()
        audioOutputFile?.delete()
        audioOutputFile = null
    }

    fun sendActiveRecording() {
        stopRecording()
        val file = audioOutputFile ?: return
        audioOutputFile = null
        scope.launch {
            repeat(times = 3) {
                var success = currentOnAudio(file)
                if (success) {
                    file.delete()
                    return@launch
                }
            }
            currentOnFailed(file)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioRecorder?.release()
        }
    }

    val currentRecordAudio by rememberUpdatedState(::recordAudio)
    val currentCancelRecording by rememberUpdatedState(::cancelRecording)
    val currentSendActiveRecording by rememberUpdatedState(::sendActiveRecording)

    LaunchedEffect(events) {
        events.collect {
            try {
                when (it) {
                    AudioRecorderControlEvent.Record -> currentRecordAudio()
                    AudioRecorderControlEvent.Cancel -> currentCancelRecording()
                    AudioRecorderControlEvent.Send -> currentSendActiveRecording()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    return remember(scope, events) { AudioRecorderControl(scope, events) }
}

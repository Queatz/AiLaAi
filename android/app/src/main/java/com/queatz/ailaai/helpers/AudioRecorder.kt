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
            setAudioSamplingRate(96000)
            setAudioEncodingBitRate(16 * 96000)
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
        onIsRecordingAudio(true)
        prepareRecorder().start()
        trackDurationJob = scope.launch {
            val start = Clock.System.now().toEpochMilliseconds()
            while (true) {
                delay(100)
                onRecordingAudioDuration(Clock.System.now().toEpochMilliseconds() - start)
            }
        }
    }

    fun recordAudio() {
        recordAudioPermissionRequester.use(
            onPermanentlyDenied = {
                onPermissionDenied()
            }
        ) {
            recordTheAudio()
        }
    }

    fun stopRecording() {
        trackDurationJob?.cancel()

        try {
            audioRecorder?.stop()
            onIsRecordingAudio(false)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun cancelRecording() {
        stopRecording()
        audioOutputFile?.delete()
        audioOutputFile = null
    }

    fun sendActiveRecording() {
        stopRecording()
        scope.launch {
            repeat(times = 3) {
                var success = onAudio(audioOutputFile ?: return@launch)
                if (success) {
                    audioOutputFile?.delete()
                    audioOutputFile = null
                    return@launch
                }
            }
            onFailed(audioOutputFile ?: return@launch)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioRecorder?.release()
        }
    }

    LaunchedEffect(Unit) {
        events.collect {
            when (it) {
                AudioRecorderControlEvent.Record -> recordAudio()
                AudioRecorderControlEvent.Cancel -> cancelRecording()
                AudioRecorderControlEvent.Send -> sendActiveRecording()
            }
        }
    }

    return AudioRecorderControl(scope, events)
}

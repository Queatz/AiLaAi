package com.queatz.ailaai.helpers

import android.Manifest
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun audioRecorder(
    onIsRecordingAudio: (Boolean) -> Unit,
    onRecordingAudioDuration: (Long) -> Unit,
    onAudio: suspend (File) -> Unit,
): AudioRecorderControl {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val initialRecordAudioPermissionState by remember { mutableStateOf(recordAudioPermissionState.status.isGranted) }
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
        if (recordAudioPermissionState.status.isGranted.not()) {
            recordAudioPermissionState.launchPermissionRequest()
        } else {
            recordTheAudio()
        }
    }

    fun stopRecording() {
        trackDurationJob?.cancel()
        onIsRecordingAudio(false)
        audioRecorder?.stop()
    }

    fun cancelRecording() {
        stopRecording()
        audioOutputFile?.delete()
        audioOutputFile = null
    }

    fun sendActiveRecording() {
        stopRecording()
        scope.launch {
            onAudio(audioOutputFile ?: return@launch)
            audioOutputFile?.delete()
            audioOutputFile = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioRecorder?.release()
        }
    }

    if (!initialRecordAudioPermissionState) {
        LaunchedEffect(recordAudioPermissionState.status.isGranted) {
            if (recordAudioPermissionState.status.isGranted) {
                recordTheAudio()
            }
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

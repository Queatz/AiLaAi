package com.queatz.ailaai

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import app.ailaai.api.group
import app.ailaai.api.groupCall
import app.ailaai.api.me
import com.queatz.ailaai.call.CallScreen
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberIsInPipMode
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.ui.theme.AiLaAiTheme
import com.queatz.db.GroupExtended
import com.queatz.db.Person
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.Stream
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.listeners.MeetingEventListener
import live.videosdk.rtc.android.listeners.ParticipantEventListener
import org.json.JSONObject
import org.webrtc.AudioTrack
import org.webrtc.MediaStreamTrack
import org.webrtc.VideoTrack

data class GroupCallParticipant(
    val participant: Participant,
    val stream: MediaStreamTrack,
    val kind: String
)

data class GroupCall(
    val group: GroupExtended,
    val meeting: Meeting,
    val localVideo: VideoTrack? = null,
    val localAudio: AudioTrack? = null,
    val localShare: VideoTrack? = null,
    val pinnedStream: VideoTrack? = null,
    val streams: List<GroupCallParticipant> = emptyList()
)

class CallActivity : AppCompatActivity() {

    private var meeting: Meeting? = null
    private var enabledStreams = MutableStateFlow(emptySet<String>())
    private val active = MutableStateFlow<GroupCall?>(null)
    private val group = MutableStateFlow<GroupExtended?>(null)
    private lateinit var startMediaProjection: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        VideoSDK.setActivityForLifeCycle(this)

        startMediaProjection = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                meeting?.enableScreenShare(result.data)
            } else {
                applicationContext.showDidntWork()
            }
        }

        enterPip()

        onBackPressedDispatcher.addCallback {
            enterPip()
        }

        setContent {
            AiLaAiTheme {
                var me by rememberStateOf<Person?>(null)
                val isInPipMode = rememberIsInPipMode()
                val context = LocalContext.current
                val active by active.collectAsState()
                val group by group.collectAsState()

                val groupId = remember {
                    when (intent?.action) {
                        Intent.ACTION_CALL -> {
                            intent.getStringExtra(GROUP_ID_EXTRA)
                        }

                        else -> null
                    }
                }

                if (groupId == null) {
                    context.showDidntWork()
                    finish()
                    return@AiLaAiTheme
                }

                // todo used cached me for calls
                LaunchedEffect(Unit) {
                    api.me {
                        me = it
                    }
                }

                LaunchedEffect(groupId) {
                    api.group(groupId) {
                        this@CallActivity.group.value = it
                    }
                }

                LaunchedEffect(me, group) {
                    if (me != null && group != null) {
                        join(context, me!!, groupId)
                    }
                }

                CompositionLocalProvider(LocalAppState provides AppState(me)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (isInPipMode) Modifier.clip(MaterialTheme.shapes.large) else Modifier)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        group?.let {
                            active?.let { active ->
                                CallScreen(
                                    it,
                                    active,
                                    isInPipMode = isInPipMode,
                                    cameraEnabled = enabled("video"),
                                    micEnabled = enabled("audio"),
                                    screenShareEnabled = enabled("share"),
                                    onToggleCamera = {
                                        toggleCamera()
                                    },
                                    onSwitchCamera = {
                                         switchCamera()
                                    },
                                    onToggleMic = {
                                        toggleMic()
                                    },
                                    onToggleScreenShare = {
                                        toggleScreenShare()
                                    },
                                    onEndCall = {
                                        end()
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    private suspend fun join(context: Context, me: Person, groupId: String) {
        api.groupCall(groupId) {
            VideoSDK.config(it.token)
            meeting = VideoSDK.initMeeting(
                context,
                it.call.room!!,
                me.name ?: context.getString(R.string.someone),
                true,
                true,
                null,
                null,
                true,
                emptyMap(),
                JSONObject()
            )

            val meeting = meeting ?: return@groupCall

            meeting.localParticipant.addEventListener(object : ParticipantEventListener() {
                override fun onStreamEnabled(stream: Stream?) {
                    enabledStreams.update {
                        it + (stream!!.kind as String)
                    }

                    active.value ?: return

                    if (stream!!.kind == "video") {
                        active.value = active.value!!.copy(localVideo = stream.track as VideoTrack)
                    } else if (stream.kind == "audio") {
                        active.value = active.value!!.copy(localAudio = stream.track as AudioTrack)
                    } else if (stream.kind == "share") {
                        active.value = active.value!!.copy(localShare = stream.track as VideoTrack)
                    }
                }

                override fun onStreamDisabled(stream: Stream?) {
                    enabledStreams.update {
                        it - (stream!!.kind as String)
                    }

                    active.value ?: return

                    if (stream!!.kind == "video") {
                        active.value = active.value!!.copy(localVideo = null)
                    } else if (stream.kind == "audio") {
                        active.value = active.value!!.copy(localAudio = null)
                    } else if (stream.kind == "share") {
                        active.value = active.value!!.copy(localShare = null)
                    }
                }
            })

            meeting.addEventListener(object : MeetingEventListener() {
                override fun onMeetingJoined() {

                }

                override fun onMeetingLeft() {

                }

                override fun onParticipantJoined(participant: Participant) {
                    participant.addEventListener(object : ParticipantEventListener() {
                        override fun onStreamEnabled(stream: Stream?) {
                            active.value ?: return

                            active.value = active.value!!.copy(
                                streams = active.value!!.streams + GroupCallParticipant(
                                    participant,
                                    stream!!.track,
                                    stream!!.kind
                                )
                            )
                        }

                        override fun onStreamDisabled(stream: Stream?) {
                            active.value ?: return

                            active.value = active.value!!.copy(
                                streams = active.value!!.streams.filter {
                                    it.stream != stream!!.track
                                }
                            )
                        }
                    })
                }

                override fun onParticipantLeft(participant: Participant) {
                    active.value ?: return

                    active.value = active.value!!.copy(
                        streams = active.value!!.streams.filter {
                            it.participant != participant
                        }
                    )
                }

                override fun onError(error: JSONObject?) {

                }
            })

            meeting.join()

            active.value = GroupCall(
                group.value!!,
                meeting
            )
        }
    }

    fun switchCamera() {
        meeting?.changeWebcam()
    }

    fun toggleCamera() {
        val meeting = meeting ?: return
        if (enabled("video")) {
            meeting.disableWebcam()
        } else {
            meeting.enableWebcam()
        }
    }

    fun toggleMic() {
        val meeting = meeting ?: return
        if (enabled("audio")) {
            meeting.muteMic()
        } else {
            meeting.unmuteMic()
        }
    }

    fun toggleScreenShare() {
        val meeting = meeting ?: return
        if (enabled("share")) {
            meeting.disableScreenShare()
        } else {
            startMediaProjection.launch(
                getSystemService(MediaProjectionManager::class.java)
                    .createScreenCaptureIntent()
            )
        }
    }

    fun togglePin(stream: VideoTrack?) {
        active.value ?: return

        active.value = active.value!!.copy(
            pinnedStream = stream.takeIf { active.value!!.pinnedStream != stream }
        )
    }

    fun end() {
        meeting?.leave()
        meeting = null
        active.value = null
        finish()
    }


    private fun enabled(stream: String): Boolean {
        val meeting = meeting ?: return false
        return meeting.localParticipant.streams.values.any {
            it.kind == stream
        } || enabledStreams.value.contains(stream)
    }

    private fun enterPip() {
        val ratio = Rational(3, 2)
        enterPictureInPictureMode(
            PictureInPictureParams.Builder().apply {
                setAspectRatio(ratio)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(true)
                }
            }.build()
        )
    }

    companion object {
        const val GROUP_ID_EXTRA = "groupId"
    }
}

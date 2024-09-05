package com.queatz.ailaai.services

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import app.ailaai.api.calls
import app.ailaai.api.group
import app.ailaai.api.groupCall
import app.ailaai.api.me
import com.queatz.ailaai.CallService
import com.queatz.ailaai.CallService.Companion.GROUP_ID_EXTRA
import com.queatz.ailaai.CallService.Companion.GROUP_NAME_EXTRA
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.db.Call
import com.queatz.db.GroupExtended
import com.queatz.db.Person
import com.queatz.push.CallStatusPushData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import live.videosdk.rtc.android.CustomStreamTrack
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.Stream
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.listeners.MeetingEventListener
import live.videosdk.rtc.android.listeners.ParticipantEventListener
import live.videosdk.rtc.android.mediaDevice.VideoDeviceInfo
import org.json.JSONObject
import org.webrtc.AudioTrack
import org.webrtc.MediaStreamTrack
import org.webrtc.VideoTrack
import java.util.HashMap

val calls by lazy {
    Calls()
}

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

class Calls {

    private lateinit var context: Context

    val calls = MutableStateFlow<List<Call>>(emptyList())

    val onEndCall = MutableSharedFlow<String>()
    val onStartScreenShare = MutableSharedFlow<Unit>()

    var meeting: Meeting? = null
    var enabledStreams = MutableStateFlow(emptySet<String>())
    val active = MutableStateFlow<GroupCall?>(null)
    val group = MutableStateFlow<GroupExtended?>(null)

    val scope = CoroutineScope(Dispatchers.Default)

    var job: Job? = null

    fun inCallCount(groupId: String) = calls.map {
        it.firstOrNull { it.group == groupId }?.participants ?: 0
    }

    fun init(context: Context) {
        this.context = context
    }

    suspend fun setMe(id: String) {
        job?.cancel()
        job = scope.launch {
            reload()
            push.events.filter {
                it is CallStatusPushData
            }.collectLatest {
                reload()
            }
        }
    }

    suspend fun reload() {
        api.calls { calls ->
            this.calls.update {
                calls
            }
        }
    }

    fun start(
        groupId: String,
        micEnabled: Boolean,
        cameraEnabled: Boolean,
        onReady: () -> Unit = {}
    ) {
        scope.launch {
            api.group(groupId) {
                group.value = it

                api.me { me ->
                    withContext(Dispatchers.Main) {
                        join(
                            context,
                            me,
                            groupId,
                            micEnabled = micEnabled,
                            cameraEnabled = cameraEnabled
                        )
                        onReady()
                    }
                }
            }
        }
    }

    fun start(
        groupId: String,
        groupName: String
    ) {
        context.startForegroundService(
            Intent(
                context,
                CallService::class.java
            ).putExtras(
                bundleOf(
                    GROUP_ID_EXTRA to groupId,
                    GROUP_NAME_EXTRA to groupName
                )
            )
        )
    }

    private suspend fun join(
        context: Context,
        me: Person,
        groupId: String,
        micEnabled: Boolean,
        cameraEnabled: Boolean
    ) {
        if (active.value?.group?.group?.id == groupId) return

        end()

        val customTracks: MutableMap<String, CustomStreamTrack> = HashMap()

        val videoCustomTrack = VideoSDK.createCameraVideoTrack(
            "h720p_w960p",
            "front",
            CustomStreamTrack.VideoMode.DETAIL,
            true,
            context,
            VideoSDK.getSelectedVideoDevice()
        )
        customTracks["video"] = videoCustomTrack

        val audioCustomTrack = VideoSDK.createAudioTrack("high_quality", context)
        customTracks["mic"] = audioCustomTrack

        api.groupCall(groupId) {
            VideoSDK.config(it.token)
            meeting = VideoSDK.initMeeting(
                context,
                it.call.room!!,
                me.name ?: context.getString(R.string.someone),
                micEnabled,
                cameraEnabled,
                null,
                null,
                true,
                customTracks,
                JSONObject()
            )

            val meeting = meeting ?: return@groupCall

            meeting.localParticipant.addEventListener(object : ParticipantEventListener() {
                override fun onStreamEnabled(stream: Stream?) {
                    enabledStreams.update {
                        it + (stream!!.kind as String)
                    }

                    active.value ?: return

                    when {
                        stream!!.kind == "video" -> {
                            active.value = active.value!!.copy(localVideo = stream.track as VideoTrack)
                        }
                        stream.kind == "audio" -> {
                            active.value = active.value!!.copy(localAudio = stream.track as AudioTrack)
                        }
                        stream.kind == "share" -> {
                            active.value = active.value!!.copy(localShare = stream.track as VideoTrack)
                        }
                    }
                }

                override fun onStreamDisabled(stream: Stream?) {
                    enabledStreams.update {
                        it - (stream!!.kind as String)
                    }

                    active.value ?: return

                    when {
                        stream!!.kind == "video" -> {
                            active.value = active.value!!.copy(localVideo = null)
                        }
                        stream.kind == "audio" -> {
                            active.value = active.value!!.copy(localAudio = null)
                        }
                        stream.kind == "share" -> {
                            active.value = active.value!!.copy(localShare = null)
                        }
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
            val videoCustomTrack = VideoSDK.createCameraVideoTrack(
                "h720p_w960p",
                "front",
                CustomStreamTrack.VideoMode.DETAIL,
                true,
                context,
                VideoSDK.getSelectedVideoDevice()
            )
            meeting.enableWebcam(videoCustomTrack)
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
            scope.launch {
                onStartScreenShare.emit(Unit)
            }
        }
    }

    fun togglePin(stream: VideoTrack?) {
        active.value ?: return

        active.value = active.value!!.copy(
            pinnedStream = stream.takeIf { active.value!!.pinnedStream != stream }
        )
    }

    fun end(groupId: String? = null) {
        groupId?.let {
            scope.launch {
                onEndCall.emit(it)
            }
        }

        if (groupId != null && active.value?.group?.group?.id != groupId) {
            return
        }

        meeting?.leave()
        meeting = null
        active.value = null
    }

    fun enabled(stream: String): Boolean {
        val meeting = meeting ?: return false
        return meeting.localParticipant.streams.values.any {
            it.kind == stream
        } || enabledStreams.value.contains(stream)
    }
}

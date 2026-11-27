package com.queatz.ailaai.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import app.ailaai.api.calls
import app.ailaai.api.group
import app.ailaai.api.groupCall
import app.ailaai.api.me
import com.queatz.ailaai.CallActivity
import com.queatz.ailaai.CallService
import com.queatz.ailaai.CallService.Companion.GROUP_ID_EXTRA
import com.queatz.ailaai.CallService.Companion.GROUP_NAME_EXTRA
import com.queatz.ailaai.CallService.Companion.LAUNCH_CALL_ACTIVITY_EXTRA
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.db.Call
import com.queatz.db.GroupExtended
import com.queatz.db.Person
import com.queatz.push.CallStatusPushData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import live.videosdk.rtc.android.CustomCameraTrackConfig
import live.videosdk.rtc.android.CustomMicrophoneTrackConfig
import live.videosdk.rtc.android.MediaType
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.RTCStream
import live.videosdk.rtc.android.Room
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.lib.LeaveReason
import live.videosdk.rtc.android.lib.VideoSDKError
import live.videosdk.rtc.android.listeners.ParticipantEventListener
import live.videosdk.rtc.android.listeners.RoomEventListener
import live.videosdk.rtc.android.mediaDevice.FacingMode
import org.json.JSONObject
import videosdk.org.webrtc.AudioTrack
import videosdk.org.webrtc.MediaStreamTrack
import videosdk.org.webrtc.VideoTrack

val calls by lazy {
    Calls()
}

data class GroupCallParticipant(
    val participant: Participant,
    val stream: MediaStreamTrack,
    val kind: MediaType
)

data class GroupCall(
    val group: GroupExtended,
    val room: Room,
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

    var room: Room? = null
    var enabledStreams = MutableStateFlow(emptySet<MediaType>())
    val active = MutableStateFlow<GroupCall?>(null)
    val group = MutableStateFlow<GroupExtended?>(null)
    val me = MutableStateFlow<Person?>(null)

    val scope = CoroutineScope(Dispatchers.Default)

    var job: Job? = null
    private var startJob: Job? = null
    private var connectingGroupId: String? = null
    private var pendingMicEnabled: Boolean? = null
    private var pendingCameraEnabled: Boolean? = null

    fun inCallCount(groupId: String) = calls.map {
        it.firstOrNull { it.group == groupId }?.participants ?: 0
    }

    fun init(context: Context) {
        this.context = context
    }

    suspend fun setMe(id: String) {
        if (me.value?.id != id) {
            me.value = null
        }
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

    fun clear() {
        job?.cancel()
        job = null
        me.value = null
        calls.value = emptyList()
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
        if (active.value?.group?.group?.id == groupId) {
            onReady()
            return
        }

        if (connectingGroupId == groupId && startJob?.isActive == true) {
            return
        }

        startJob?.cancel()
        connectingGroupId = groupId
        pendingMicEnabled = null
        pendingCameraEnabled = null

        startJob = scope.launch {
            try {
                val loadedGroup = group.value?.takeIf { it.group?.id == groupId }
                    ?: run {
                        var fetched: GroupExtended? = null
                        api.group(
                            groupId,
                            onError = { error ->
                                if (error !is CancellationException && error !is InterruptedException) {
                                    withContext(Dispatchers.Main) {
                                        context.showDidntWork(R.string.error_start_call)
                                    }
                                    end(groupId)
                                }
                            }
                        ) {
                            fetched = it
                            group.value = it
                        }
                        fetched
                    } ?: return@launch

                ensureActive()

                val currentMe = me.value
                    ?: run {
                        var fetched: Person? = null
                        api.me(
                            onError = { error ->
                                if (error !is CancellationException && error !is InterruptedException) {
                                    withContext(Dispatchers.Main) {
                                        context.showDidntWork(R.string.error_start_call)
                                    }
                                    end(groupId)
                                }
                            }
                        ) {
                            fetched = it
                            me.value = it
                        }
                        fetched
                    } ?: return@launch

                ensureActive()

                withContext(Dispatchers.Main) {
                    ensureActive()
                    join(
                        context = context,
                        me = currentMe,
                        groupExtended = loadedGroup,
                        groupId = groupId,
                        micEnabled = micEnabled,
                        cameraEnabled = cameraEnabled
                    )
                    if (active.value?.group?.group?.id == groupId) {
                        onReady()
                    }
                }
            } finally {
                if (connectingGroupId == groupId) {
                    connectingGroupId = null
                }
            }
        }
    }

    fun start(
        groupId: String,
        groupName: String,
        groupExtended: GroupExtended? = null,
        me: Person? = null,
        activityContext: Context? = null
    ) {
        if (groupExtended != null) {
            this.group.value = groupExtended
        }
        if (me != null) {
            this.me.value = me
        }

        val launchContext = activityContext ?: context
        launchContext.startActivity(
            Intent(
                launchContext,
                CallActivity::class.java
            ).apply {
                action = Intent.ACTION_CALL
                if (launchContext !is Activity) {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                putExtra(GROUP_ID_EXTRA, groupId)
                putExtra(GROUP_NAME_EXTRA, groupName)
            }
        )

        context.startForegroundService(
            Intent(
                context,
                CallService::class.java
            ).putExtras(
                Bundle().apply {
                    putString(GROUP_ID_EXTRA, groupId)
                    putString(GROUP_NAME_EXTRA, groupName)
                    putBoolean(LAUNCH_CALL_ACTIVITY_EXTRA, false)
                }
            )
        )
    }

    private suspend fun join(
        context: Context,
        me: Person,
        groupExtended: GroupExtended,
        groupId: String,
        micEnabled: Boolean,
        cameraEnabled: Boolean
    ) {
        if (active.value?.group?.group?.id == groupId) return

        leaveCurrentRoom()

        api.groupCall(
            groupId,
            onError = { error ->
                if (error !is CancellationException && error !is InterruptedException) {
                    withContext(Dispatchers.Main) {
                        context.showDidntWork(R.string.error_start_call)
                    }
                    end(groupId)
                }
            }
        ) {
            currentCoroutineContext().ensureActive()

            val effectiveMicEnabled = pendingMicEnabled ?: micEnabled
            val effectiveCameraEnabled = pendingCameraEnabled ?: cameraEnabled

            val videoCustomTrack = if (effectiveCameraEnabled) {
                runCatching {
                    VideoSDK.createCameraVideoTrack(
                        encoderConfig = CustomCameraTrackConfig.H720P_W960P,
                        facingMode = FacingMode.front,
                        multiStream = true,
                        cameraId = VideoSDK.getSelectedVideoDevice()?.deviceId
                    )
                }.getOrNull()
            } else {
                null
            }

            val audioCustomTrack = if (effectiveMicEnabled) {
                runCatching {
                    VideoSDK.createMicrophoneAudioTrack(
                        encoderConfig = CustomMicrophoneTrackConfig.HIGH_QUALITY
                    )
                }.getOrNull()
            } else {
                null
            }

            val newRoom = VideoSDK.createRoom(
                roomId = it.call.room!!,
                token = it.token,
                participantName = me.name ?: context.getString(R.string.someone),
                micEnabled = effectiveMicEnabled,
                camEnabled = effectiveCameraEnabled,
                multiStream = true,
                customCameraTrack = videoCustomTrack,
                customMicrophoneTrack = audioCustomTrack,
                metaData = JSONObject()
            )
            room = newRoom

            newRoom.localParticipant.addEventListener(object : ParticipantEventListener() {
                override fun onStreamEnabled(stream: RTCStream) {
                    enabledStreams.update {
                        it + stream.kind
                    }

                    active.value ?: return

                    when (stream.kind) {
                        MediaType.VIDEO -> {
                            active.value = active.value!!.copy(localVideo = stream.track as? VideoTrack)
                        }
                        MediaType.AUDIO -> {
                            active.value = active.value!!.copy(localAudio = stream.track as? AudioTrack)
                        }
                        MediaType.SHARE -> {
                            active.value = active.value!!.copy(localShare = stream.track as? VideoTrack)
                        }
                        else -> Unit
                    }
                }

                override fun onStreamDisabled(stream: RTCStream) {
                    enabledStreams.update {
                        it - stream.kind
                    }

                    active.value ?: return

                    when (stream.kind) {
                        MediaType.VIDEO -> {
                            active.value = active.value!!.copy(localVideo = null)
                        }
                        MediaType.AUDIO -> {
                            active.value = active.value!!.copy(localAudio = null)
                        }
                        MediaType.SHARE -> {
                            active.value = active.value!!.copy(localShare = null)
                        }
                        else -> Unit
                    }
                }
            })

            newRoom.addEventListener(object : RoomEventListener() {
                override fun onRoomJoined(switchRoomId: String?) {

                }

                override fun onRoomLeft(reason: LeaveReason) {

                }

                override fun onParticipantJoined(participant: Participant) {
                    participant.addEventListener(object : ParticipantEventListener() {
                        override fun onStreamEnabled(stream: RTCStream) {
                            active.value ?: return

                            active.value = active.value!!.copy(
                                streams = active.value!!.streams + GroupCallParticipant(
                                    participant = participant,
                                    stream = stream.track,
                                    kind = stream.kind
                                )
                            )
                        }

                        override fun onStreamDisabled(stream: RTCStream) {
                            active.value ?: return

                            active.value = active.value!!.copy(
                                streams = active.value!!.streams.filter {
                                    it.stream != stream.track
                                }
                            )
                        }
                    })
                }

                override fun onParticipantLeft(participant: Participant, reason: LeaveReason) {
                    active.value ?: return

                    active.value = active.value!!.copy(
                        streams = active.value!!.streams.filter {
                            it.participant != participant
                        }
                    )
                }

                override fun onError(error: VideoSDKError) {

                }
            })

            active.value = GroupCall(
                groupExtended,
                newRoom
            )

            runCatching {
                newRoom.join()
            }.onFailure { error ->
                error.printStackTrace()
                if (room == newRoom) {
                    end(groupId)
                }
            }
        }
    }

    private suspend fun leaveCurrentRoom() {
        val currentRoom = room ?: return
        room = null
        active.value = null
        enabledStreams.value = emptySet()
        currentRoom.removeAllEventListeners()
        currentRoom.localParticipant.removeAllEventListeners()
        runCatching {
            currentRoom.leave()
        }.onFailure { it.printStackTrace() }
    }

    fun switchCamera() {
        val room = room ?: return
        scope.launch {
            runCatching {
                room.changeCam()
            }.onFailure { it.printStackTrace() }
        }
    }

    fun toggleCamera() {
        val room = room
        if (room == null) {
            pendingCameraEnabled = true
            return
        }
        scope.launch {
            runCatching {
                if (enabled(MediaType.VIDEO)) {
                    room.disableCam()
                } else {
                    val videoCustomTrack = VideoSDK.createCameraVideoTrack(
                        encoderConfig = CustomCameraTrackConfig.H720P_W960P,
                        facingMode = FacingMode.front,
                        multiStream = true,
                        cameraId = VideoSDK.getSelectedVideoDevice()?.deviceId
                    )
                    room.enableCam(videoCustomTrack)
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    fun toggleMic() {
        val room = room
        if (room == null) {
            pendingMicEnabled = true
            return
        }
        scope.launch {
            runCatching {
                if (enabled(MediaType.AUDIO)) {
                    room.muteMic()
                } else {
                    room.unmuteMic()
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    fun toggleScreenShare() {
        val room = room ?: return
        if (enabled(MediaType.SHARE)) {
            scope.launch {
                runCatching {
                    room.disableScreenShare()
                }.onFailure { it.printStackTrace() }
            }
        } else {
            scope.launch {
                onStartScreenShare.emit(Unit)
            }
        }
    }

    fun enableScreenShare(data: Intent?) {
        val room = room ?: return
        data ?: return
        scope.launch {
            runCatching {
                room.enableScreenShare(data)
            }.onFailure { it.printStackTrace() }
        }
    }

    fun togglePin(stream: VideoTrack?) {
        active.value ?: return

        active.value = active.value!!.copy(
            pinnedStream = stream.takeIf { active.value!!.pinnedStream != stream }
        )
    }

    fun end(groupId: String? = null) {
        val targetGroupId = groupId ?: active.value?.group?.group?.id ?: connectingGroupId
        targetGroupId?.let {
            scope.launch {
                onEndCall.emit(it)
            }
        }

        if (groupId == null || connectingGroupId == groupId) {
            startJob?.cancel()
            startJob = null
            connectingGroupId = null
            pendingMicEnabled = null
            pendingCameraEnabled = null
        }

        if (groupId != null && active.value?.group?.group?.id != groupId) {
            return
        }

        val currentRoom = room
        room = null
        active.value = null
        enabledStreams.value = emptySet()

        if (currentRoom != null) {
            currentRoom.removeAllEventListeners()
            currentRoom.localParticipant.removeAllEventListeners()
            scope.launch {
                runCatching {
                    currentRoom.leave()
                }.onFailure { it.printStackTrace() }
            }
        }
    }

    fun enabled(
        stream: MediaType,
        currentEnabledStreams: Set<MediaType> = enabledStreams.value
    ): Boolean {
        val room = room ?: return false
        return room.localParticipant.streams.values.any {
            it.kind == stream
        } || currentEnabledStreams.contains(stream)
    }
}

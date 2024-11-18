@file:OptIn(ExperimentalSerializationApi::class)

import androidx.compose.runtime.*
import app.ailaai.api.calls
import app.ailaai.api.groupCall
import com.queatz.db.GroupExtended
import com.queatz.db.Person
import com.queatz.push.PushAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToDynamic
import lib.VideoSDK
import org.w3c.dom.mediacapture.MediaStream

val call = Call()

data class GroupCallParticipant(
    val participant: dynamic,
    val stream: MediaStream,
    val kind: String
)

data class GroupCall(
    val group: GroupExtended,
    val meeting: dynamic,
    val localVideo: MediaStream? = null,
    val localAudio: MediaStream? = null,
    val localShare: MediaStream? = null,
    val pinnedStream: MediaStream? = null,
    val streams: List<GroupCallParticipant> = emptyList()
)

@Serializable
data class VideoSdkInitMeetingBody(
    val meetingId: String,
    val name: String,
    val micEnabled: Boolean = true,
    val webcamEnabled: Boolean = true
)

class Call {

    private lateinit var scope: CoroutineScope

    val active = MutableStateFlow<GroupCall?>(null)
    val calls = MutableStateFlow<List<com.queatz.db.Call>>(emptyList())

    private var enabledStreams = MutableStateFlow(emptySet<String>())

    fun init(scope: CoroutineScope) {
        this.scope = scope

        scope.launch {
            reload()
            push.events.filter {
                it.action == PushAction.CallStatus
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

    // todo remove @NoLiveLiterals annotation
    @NoLiveLiterals
    fun join(me: Person, group: GroupExtended) {
        scope.launch {
            end()
            api.groupCall(group.group!!.id!!) {
                VideoSDK.config(it.token)
                val meeting = VideoSDK.initMeeting(
                    json.encodeToDynamic(
                        VideoSdkInitMeetingBody(
                            meetingId = it.call.room!!,
                            name = me.name ?: application.appString { someone }
                        )
                    )
                )

                meeting.localParticipant.on("stream-enabled") { stream: dynamic ->
                    enabledStreams.update {
                        it + (stream.kind as String)
                    }
                    when (stream.kind) {
                        "video" -> {
                            val mediaStream = MediaStream()
                            mediaStream.addTrack(stream.track)
                            // todo: set stream kind?
                            active.value = active.value!!.copy(localVideo = mediaStream)
                        }
                        "audio" -> {
                            val mediaStream = MediaStream()
                            mediaStream.addTrack(stream.track)
                            // todo: set stream kind?
                            active.value = active.value!!.copy(localAudio = mediaStream)
                        }
                        "share" -> {
                            val mediaStream = MediaStream()
                            mediaStream.addTrack(stream.track)
                            // todo: set stream kind?
                            active.value = active.value!!.copy(localShare = mediaStream)
                        }
                    }
                    Unit
                }

                meeting.localParticipant.on("stream-disabled") { stream: dynamic ->
                    enabledStreams.update {
                        it - (stream.kind as String)
                    }
                    when (stream.kind) {
                        "video" -> {
                            active.value = active.value!!.copy(localVideo = null)
                        }
                        "audio" -> {
                            active.value = active.value!!.copy(localAudio = null)
                        }
                        "share" -> {
                            active.value = active.value!!.copy(localShare = null)
                        }
                    }
                    Unit
                }

                meeting.on("participant-joined") { participant: dynamic ->
                    participant.on("stream-enabled") { stream: dynamic ->
                        val mediaStream = MediaStream()
                        mediaStream.addTrack(stream.track)
                        active.value = active.value!!.copy(
                            streams = active.value!!.streams + GroupCallParticipant(
                                participant = participant,
                                stream = mediaStream,
                                kind = stream.kind
                            )
                        )
                        Unit
                    }

                    participant.on("stream-disabled") { stream: dynamic ->
                        active.value = active.value!!.copy(
                            streams = active.value!!.streams.filter {
                                it.stream.getTracks().firstOrNull() != stream.track || it.kind != stream.kind
                            }
                        )
                        Unit
                    }
                }

                meeting.on("participant-left") { participant: dynamic ->
                    active.value = active.value!!.copy(
                        streams = active.value!!.streams.filter {
                            it.participant != participant
                        }
                    )
                    Unit
                }

                meeting.join()

                active.value = GroupCall(
                    group = group,
                    meeting = meeting
                )
            }
        }
    }

    fun end() {
        // todo: remove listeners with .off()
        active.value?.meeting?.leave()
        active.value = null
    }

    fun enabled(stream: String): Boolean {
        // used by js() below
        val meeting = active.value?.meeting ?: return false
        val streams = js("Array.from(meeting.localParticipant.streams.values())") as Array<dynamic>
        return streams.any {
            it.kind == stream
        } || enabledStreams.value.contains(stream)
    }

    fun toggleCamera() {
        val meeting = active.value?.meeting ?: return
        if (enabled("video")) {
            meeting.disableWebcam()
        } else {
            meeting.enableWebcam()
        }
    }

    fun toggleMic() {
        val meeting = active.value?.meeting ?: return
        if (enabled("audio")) {
            meeting.muteMic()
        } else {
            meeting.unmuteMic()
        }
    }

    fun toggleScreenShare() {
        val meeting = active.value?.meeting ?: return
        if (enabled("share")) {
            meeting.disableScreenShare()
        } else {
            meeting.enableScreenShare()
        }
    }

    fun togglePin(stream: MediaStream?) {
        active.value = active.value!!.copy(
            pinnedStream = stream.takeIf { active.value!!.pinnedStream != stream }
        )
    }
}

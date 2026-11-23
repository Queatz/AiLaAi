package com.queatz.ailaai.call

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.inDp
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.services.GroupCall
import com.queatz.ailaai.services.GroupCallParticipant
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.GroupExtended
import live.videosdk.rtc.android.MediaType
import videosdk.org.webrtc.RendererCommon
import videosdk.org.webrtc.VideoTrack

@Composable
fun CallScreen(
    group: GroupExtended?,
    groupName: String,
    active: GroupCall?,
    isInPipMode: Boolean,
    cameraEnabled: Boolean,
    micEnabled: Boolean,
    screenShareEnabled: Boolean,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onTogglePictureInPicture: () -> Unit,
    onTogglePin: (GroupCallParticipant) -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val me = me
    val title = if (group != null && me?.id != null) {
        group.name(
            stringResource(R.string.someone),
            stringResource(R.string.empty_group_name),
            omit = me.id!!.inList()
        )
    } else {
        groupName
    }

    val activeVideoStreams = active?.streams?.count {
        it.kind == MediaType.VIDEO || it.kind == MediaType.SHARE
    } ?: 0

    Box(modifier) {
        if (active == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator()
            }
        } else {
            // todo lazy grid
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (activeVideoStreams > 0) {
                    active.streams.filter {
                        it.kind == MediaType.VIDEO || it.kind == MediaType.SHARE
                    }.filter {
                        active.pinnedStream == null || active.pinnedStream == it.stream
                    }.forEach {
                        VideoSdkView(
                            track = it.stream as VideoTrack,
                            scaleType = if (it.kind == MediaType.VIDEO) RendererCommon.ScalingType.SCALE_ASPECT_FILL else RendererCommon.ScalingType.SCALE_ASPECT_FIT,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onTogglePin(it)
                                }
                        )
                    }
                } else if (active.localShare != null || active.localVideo != null) {
                    VideoSdkView(
                        track = active.localShare ?: active.localVideo as VideoTrack,
                        scaleType = if (active.localShare == null) RendererCommon.ScalingType.SCALE_ASPECT_FILL else RendererCommon.ScalingType.SCALE_ASPECT_FIT,
                        modifier = Modifier
                            .weight(1f)
                    )
                }
            }
        }

        var size by rememberStateOf(IntSize(0, 0))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isInPipMode) {
                        Modifier
                    } else {
                        Modifier
                            .windowInsetsPadding(
                                WindowInsets.safeContent.only(WindowInsetsSides.Vertical)
                            )
                    }
                )
                .onPlaced {
                    size = it.size
                }
        ) {
            if (active != null && activeVideoStreams > 0) {
                (active.localShare ?: active.localVideo)?.let { track ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.pad)
                            .size(size.width.inDp() * .15f, size.height.inDp() * .15f)
                    ) {
                        VideoSdkView(
                            track = track,
                            scaleType = RendererCommon.ScalingType.SCALE_ASPECT_FILL,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.elevation), MaterialTheme.shapes.medium)
                                .shadow(1.elevation, MaterialTheme.shapes.medium)
                                .clip(MaterialTheme.shapes.medium)
                        )
                    }
                }
            }

            if (isInPipMode) {
                Text(
                    title,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(1.pad)
                        .align(Alignment.TopCenter)
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(2.pad)
                ) {
                    FilledIconButton(
                        {
                            onSwitchCamera()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        enabled = active != null && cameraEnabled
                    ) {
                        Icon(Icons.Outlined.Sync, null)
                    }
                    Text(
                        title,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(1.pad)
                    )
                    FilledIconButton(
                        {
                            onTogglePictureInPicture()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Icon(Icons.Outlined.FullscreenExit, null)
                    }
                }

                Row(
                    horizontalArrangement = spacedBy(1.pad),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.pad)
                ) {
                    FilledIconButton(
                        {
                            onToggleMic()
                        },
                        enabled = active != null,
                        colors = if (micEnabled) {
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        } else {
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        }
                    ) {
                        Icon(if (micEnabled) Icons.Outlined.Mic else Icons.Outlined.MicOff, null)
                    }
                    FilledIconButton(
                        {
                            onToggleCamera()
                        },
                        enabled = active != null,
                        colors = if (cameraEnabled) {
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        } else {
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        }
                    ) {
                        Icon(if (cameraEnabled) Icons.Outlined.Videocam else Icons.Outlined.VideocamOff, null)
                    }
                    FilledIconButton(
                        {
                            onToggleScreenShare()
                        },
                        enabled = active != null,
                        colors = if (screenShareEnabled) {
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        } else {
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        }
                    ) {
                        Icon(
                            if (screenShareEnabled) Icons.Outlined.CancelPresentation else Icons.Outlined.PresentToAll,
                            null
                        )
                    }
                    FilledIconButton(
                        {
                            onEndCall()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(Icons.Outlined.CallEnd, null)
                    }
                }
            }
        }
    }
}

package com.queatz.ailaai.call

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.IntSize
import com.queatz.ailaai.GroupCall
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.inDp
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.GroupExtended
import org.webrtc.VideoTrack

@Composable
fun CallScreen(
    group: GroupExtended,
    active: GroupCall,
    isInPipMode: Boolean,
    cameraEnabled: Boolean,
    micEnabled: Boolean,
    screenShareEnabled: Boolean,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val me = me ?: return

    val activeVideoStreams = active.streams.count {
        it.kind == "video" || it.kind == "share"
    }

    Box(modifier) {
        // todo lazy grid
        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (activeVideoStreams > 0) {
                active.streams.filter {
                    it.kind == "video" || it.kind == "share"
                }.forEach {
                    VideoSdkView(
                        track = it.stream as VideoTrack,
                        modifier = Modifier
                            .weight(1f)
                    )
                }
            } else if (active.localShare != null || active.localVideo != null) {
                VideoSdkView(
                    track = active.localShare ?: active.localVideo as VideoTrack,
                    modifier = Modifier
                        .weight(1f)
                )
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
            if (activeVideoStreams > 0) {
                (active.localShare ?: active.localVideo)?.let { track ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.pad)
                            .size(size.width.inDp() * .15f, size.height.inDp() * .15f)
                    ) {
                        VideoSdkView(
                            track = track,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.elevation), MaterialTheme.shapes.medium)
                                .shadow(1.elevation, MaterialTheme.shapes.medium)
                                .clip(MaterialTheme.shapes.medium)
                        )
                    }
                }
            }

            Text(
                group.name(
                    stringResource(R.string.someone),
                    stringResource(R.string.empty_group_name),
                    omit = me.id!!.inList()
                ),
                modifier = Modifier
                    .padding(1.pad)
                    .align(Alignment.TopCenter)
            )

            if (!isInPipMode) {
                FilledIconButton(
                    {
                        onSwitchCamera()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.pad)
                ) {
                    Icon(Icons.Outlined.Sync, null)
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

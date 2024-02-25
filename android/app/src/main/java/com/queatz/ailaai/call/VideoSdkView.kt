package com.queatz.ailaai.call

import android.view.ViewGroup.LayoutParams
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import live.videosdk.rtc.android.VideoView
import org.webrtc.RendererCommon
import org.webrtc.RendererCommon.ScalingType
import org.webrtc.VideoTrack

@Composable
fun VideoSdkView(track: VideoTrack, scaleType: ScalingType, modifier: Modifier = Modifier) {
    // todo just do removeTrack/addTrack in update = {} ?
    key(track) {
        AndroidView(
            factory = {
                VideoView(it).apply {
                    addTrack(track)
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                }
            },
            modifier = modifier,
            update = { view ->
                view.setScalingType(scaleType)
            },
            onRelease = { view ->
                view.releaseSurfaceViewRenderer()
            }
        )
    }
}

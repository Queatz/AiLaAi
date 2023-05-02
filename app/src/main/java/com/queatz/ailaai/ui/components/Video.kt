package com.queatz.ailaai.ui.components

import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.queatz.ailaai.databinding.LayoutVideoBinding
import com.queatz.ailaai.ui.theme.ElevationDefault


@Composable
fun Video(
    url: String,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
//            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            prepare()
        }
    }

    LaunchedEffect(url) {
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycle by remember { mutableStateOf(Lifecycle.State.CREATED) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycle = event.targetState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isPlaying, lifecycle) {
        val isPlayable = isPlaying && lifecycle == Lifecycle.State.RESUMED
        exoPlayer.playWhenReady = isPlayable
        if (isPlayable) {
            if (!exoPlayer.isPlaying) {
                exoPlayer.play()
            }
        } else {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    val color = Color.Transparent.toArgb() //MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp /* Elevated card container color */).toArgb()
    val colorDrawable = ColorDrawable(color)
    AndroidViewBinding(
        modifier = modifier,
        factory = LayoutVideoBinding::inflate
    ) {
        root.apply {
            player = exoPlayer
            useController = false
            useArtwork = true
            setShutterBackgroundColor(color)
            defaultArtwork = colorDrawable
            background = colorDrawable
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            clipToPadding = true
        }
    }
}

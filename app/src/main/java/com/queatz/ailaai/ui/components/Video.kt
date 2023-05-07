package com.queatz.ailaai.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.queatz.ailaai.databinding.LayoutVideoBinding


@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
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
    AndroidViewBinding(
        modifier = modifier,
        factory = LayoutVideoBinding::inflate
    ) {
        root.apply {
            player = exoPlayer
            useController = false
            useArtwork = true
            setShutterBackgroundColor(color)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
}

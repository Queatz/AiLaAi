package com.queatz.ailaai.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.queatz.ailaai.extensions.formatTime
import com.queatz.ailaai.ui.theme.pad
import kotlinx.coroutines.android.awaitFrame

enum class PlaybackSpeed(val factor: Float) {
    Slow(0.75f),
    Normal(1.0f),
    Fast(1.5f),
    Faster(2f),
}

private sealed class AudioSource {
    data class Url(val url: String) : AudioSource()
    data class Data(val data: ByteArray, val contentType: String?) : AudioSource()
}

@Composable
fun Audio(
    url: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false
) {
    Audio(AudioSource.Url(url), modifier, autoPlay)
}

@Composable
fun Audio(
    data: ByteArray,
    contentType: String?,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false,
) {
    Audio(AudioSource.Data(data, contentType), modifier, autoPlay)
}

@Composable
private fun Audio(
    source: AudioSource,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = autoPlay
            setWakeMode(C.WAKE_MODE_LOCAL)
            prepare()
        }
    }

    var isPlaying by remember {
        mutableStateOf(exoPlayer.isPlaying)
    }

    var playbackSpeed by remember {
        mutableStateOf(PlaybackSpeed.Normal)
    }

    var seekAmount by remember {
        mutableStateOf(0f)
    }

    var timeRemaining by remember {
        mutableStateOf(0L)
    }

    var duration by remember {
        mutableStateOf(0L)
    }

    var viewport by remember {
        mutableStateOf(Size(0f, 0f))
    }

    var dragFactor by remember {
        mutableStateOf<Float?>(null)
    }

    exoPlayer.addListener(object : Player.Listener {
        override fun onIsPlayingChanged(_isPlaying: Boolean) {
            isPlaying = _isPlaying
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(EVENT_TIMELINE_CHANGED)) {
                duration = if (exoPlayer.duration == C.TIME_UNSET) 0L else exoPlayer.duration
                timeRemaining = exoPlayer.duration - exoPlayer.contentPosition
            }
        }
    })

    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed.factor)
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            awaitFrame()
            seekAmount =
                if (exoPlayer.duration == C.TIME_UNSET) 0f else exoPlayer.contentPosition.toFloat() / exoPlayer.duration.toFloat()
            timeRemaining = exoPlayer.duration - exoPlayer.contentPosition
            if (dragFactor != null) {
                dragFactor = null
            }
        }
    }

    LaunchedEffect(source) {
        when (source) {
            is AudioSource.Url -> {
                exoPlayer.setMediaItem(MediaItem.fromUri(source.url))
            }
            is AudioSource.Data -> {
                val mediaSource = ProgressiveMediaSource.Factory({ ByteArrayDataSource(source.data) })
                    .createMediaSource(MediaItem.Builder().let {
                        if (source.contentType == null) {
                            it
                        } else {
                            it.setMimeType(source.contentType)
                        }
                    }.build())
                exoPlayer.setMediaSource(mediaSource)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = modifier
        .onPlaced { viewport = it.boundsInParent().size }
        .pointerInput(Unit) {
            detectTapGestures {
                exoPlayer.seekTo(((it.x / viewport.width) * exoPlayer.duration).toLong())
                if (!isPlaying) {
                    exoPlayer.play()
                }
            }
        }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = {
                    exoPlayer.pause()
                },
                onDragEnd = {
                    exoPlayer.seekTo(((dragFactor ?: 0f) * exoPlayer.duration).toLong())
                    exoPlayer.play()
                }
            ) { change, dragAmount ->
                dragFactor = change.position.x / viewport.width
                timeRemaining = (exoPlayer.duration * (1f - dragFactor!!)).toLong()
            }
        }
    ) {
        Box(
            modifier = Modifier
                .height(6.dp)
                .fillMaxWidth(fraction = dragFactor ?: seekAmount)
                .align(Alignment.BottomStart)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.333f))
        ) {}
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(
                    horizontal = 1.pad
                )
                .fillMaxSize()
        ) {
            IconButton(
                onClick = {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    } else {
                        if (exoPlayer.playbackState == Player.STATE_ENDED) {
                            exoPlayer.seekTo(0L)
                        }
                        exoPlayer.play()
                    }
                }
            ) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
            }
            IconButton(
                onClick = {
                    playbackSpeed = when (playbackSpeed) {
                        PlaybackSpeed.Slow -> PlaybackSpeed.Normal
                        PlaybackSpeed.Normal -> PlaybackSpeed.Fast
                        PlaybackSpeed.Fast -> PlaybackSpeed.Faster
                        PlaybackSpeed.Faster -> PlaybackSpeed.Slow
                    }
                }
            ) {
                Text(
                    when (playbackSpeed) {
                        PlaybackSpeed.Slow -> ".75x"
                        PlaybackSpeed.Normal -> "1x"
                        PlaybackSpeed.Fast -> "1.5x"
                        PlaybackSpeed.Faster -> "2x"
                    }
                )
            }
            Crossfade(
                duration > 0,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 1.pad),
                label = ""
            ) { show ->
                if (show) {
                    Text(
                        timeRemaining.formatTime(),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

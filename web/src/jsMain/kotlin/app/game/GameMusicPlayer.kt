package app.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.gameMusics
import baseUrl
import com.queatz.db.GameMusic
import game.AnimationMarker
import game.Game
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.dom.Audio
import org.jetbrains.compose.web.dom.Source
import org.w3c.dom.HTMLAudioElement
import kotlin.math.abs

@Composable
fun GameMusicPlayer(game: Game?) {
    val scope = rememberCoroutineScope()
    var musicList by remember { mutableStateOf<List<GameMusic>>(emptyList()) }
    var currentMusic by remember { mutableStateOf<GameMusic?>(null) }
    var audioElement by remember { mutableStateOf<HTMLAudioElement?>(null) }
    var currentAudioSrc by remember { mutableStateOf<String?>(null) }

    // Load music list on component mount
    LaunchedEffect(Unit) {
        api.gameMusics(
            onError = {
                console.error("Failed to load music", it)
            }
        ) { musics ->
            musicList = musics
        }
    }

    // DisposableEffect to stop audio when component is unmounted or game changes
    DisposableEffect(game) {
        onDispose {
            // Stop any playing audio when component is unmounted or game changes
            audioElement?.pause()
            audioElement?.src = ""
            currentMusic = null
            currentAudioSrc = null
        }
    }

    // Audio component for playback (hidden in the UI)
    key(currentAudioSrc) {
        currentAudioSrc?.let { src ->
            Audio({
                style {
                    display(DisplayStyle.None)
                }

                // Set loop attribute to make music loop during animation
                attr("loop", "true")

                ref {
                    audioElement = it
                    onDispose {
                        audioElement = null
                    }
                }
            }) {
                // Only add Source when we have a valid audio URL
                Source({
                    attr("src", src)
                    attr("type", "audio/mp4") // Assuming MP4 format, adjust if needed
                })
            }
        }
    }
    // Update audio element when source changes
    LaunchedEffect(currentAudioSrc) {
        currentAudioSrc?.let { src ->
            audioElement?.apply {
                // Set up event handler for when the audio can play
                oncanplay = {
                    play() // Play the audio when it's ready
                    Unit
                }

                try {
                    load() // Reload with the new source
                } catch (e: Throwable) {
                    console.error("Error loading audio: $src", e)
                }
            }
        }
    }

    // Function to play music at a marker
    fun playMusicAtMarker(marker: AnimationMarker) {
        // If we have a current music and it matches the marker name, play it
        val musicToPlay = musicList.find { it.name == marker.name }
        if (musicToPlay?.audio != null) {
            console.log("Playing music: ${musicToPlay.name} ($baseUrl${musicToPlay.audio}) at marker time ${marker.time}")

            // Update the current audio source
            currentAudioSrc = "$baseUrl${musicToPlay.audio}"
            currentMusic = musicToPlay
        } else {
            console.log("No matching music found for marker: ${marker.name} at time ${marker.time}")
            console.log("Available music: ${musicList.map { it.name }}")
        }
    }

    // Register a callback with the game to check for markers during animation
    LaunchedEffect(game, musicList) {
        game?.let { g ->
            // Add a callback to check for markers during animation
            g.animationData.onTimeUpdate = { time ->
                // Find markers at or very close to the current time
                val markersAtTime = g.animationData.markers.filter { marker ->
                    abs(marker.time - time) < 1.0 // Within 1 second for more reliable detection
                }

                // Play music for any markers at this time
                markersAtTime.forEach { marker ->
                    playMusicAtMarker(marker)
                }
            }
        }
    }

    // Set up flow collection to handle play state changes
    LaunchedEffect(game) {
        game?.playStateFlow?.collectLatest { isPlaying ->
            if (!isPlaying) {
                // Pause the audio when animation is paused
                audioElement?.pause()
            } else {
                // When animation starts playing, check for markers at the current time
                game.animationData.currentTime.let { time ->
                    console.log("Animation started playing at time: $time")

                    // Find markers at or very close to the current time
                    val markersAtTime = game.animationData.markers.filter { marker ->
                        abs(marker.time - time) < 1.0 // Within 1 second for more reliable detection
                    }

                    // Play music for any markers at this time
                    if (markersAtTime.isNotEmpty()) {
                        console.log("Found ${markersAtTime.size} markers at start time: $time")
                        markersAtTime.forEach { marker ->
                            playMusicAtMarker(marker)
                        }
                    } else {
                        console.log("No markers found at start time: $time")
                    }
                }

                // Resume the audio if we have a current music and animation is playing
                if (currentMusic != null) {
                    audioElement?.play()
                }
            }
        }
    }
}

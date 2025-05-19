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
import com.queatz.db.PlayMusicEvent
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
    // Create an instance of GameMusicPlayerUtil
    val musicPlayerUtil = remember { GameMusicPlayerUtil() }
    var audioElement by remember { mutableStateOf<HTMLAudioElement?>(null) }
    var currentAudioSrc by remember { mutableStateOf<String?>(null) }

    // Track current music for UI updates
    var currentMusic by remember { mutableStateOf<GameMusic?>(null) }

    // Update currentMusic and currentAudioSrc when they change in the util
    LaunchedEffect(Unit) {
        // We don't need to load all music at once anymore
        // We'll load music on demand when markers are encountered
        currentMusic = musicPlayerUtil.getCurrentMusic()
        currentAudioSrc = musicPlayerUtil.getCurrentAudioSrc()
    }

    // DisposableEffect to stop audio when component is unmounted or game changes
    DisposableEffect(game) {
        onDispose {
            // Stop any playing audio when component is unmounted or game changes
            musicPlayerUtil.stopMusic()
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
                    // Store the audio element in our local state
                    audioElement = it
                    // Also pass it to the utility
                    musicPlayerUtil.setAudioElement(it)

                    onDispose {
                        audioElement = null
                        musicPlayerUtil.setAudioElement(null)
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
        // Use the utility to play music for this marker
        musicPlayerUtil.playMusicForMarker(marker)

        // Update our local state to reflect changes in the utility
        currentMusic = musicPlayerUtil.getCurrentMusic()
        currentAudioSrc = musicPlayerUtil.getCurrentAudioSrc()
    }

    // Register a callback with the game to check for markers during animation
    LaunchedEffect(game) {
        game?.let { g ->
            // Add a callback to check for markers during animation
            g.animationData.onTimeUpdate = { time ->
                // Use the utility to process markers at the current time
                musicPlayerUtil.processMarkersAtTime(g, time)

                // Update our local state to reflect changes in the utility
                currentMusic = musicPlayerUtil.getCurrentMusic()
                currentAudioSrc = musicPlayerUtil.getCurrentAudioSrc()
            }
        }
    }

    // Set up flow collection to handle play state changes
    LaunchedEffect(game) {
        game?.playStateFlow?.collectLatest { isPlaying ->
            if (!isPlaying) {
                // Pause the audio when animation is paused
                musicPlayerUtil.pauseMusic()
            } else {
                // When animation starts playing, check for markers at the current time
                game.animationData.currentTime.let { time ->
                    console.log("Animation started playing at time: $time")

                    // Use the utility to process markers at the current time
                    musicPlayerUtil.processMarkersAtTime(game, time)

                    // Update our local state to reflect changes in the utility
                    currentMusic = musicPlayerUtil.getCurrentMusic()
                    currentAudioSrc = musicPlayerUtil.getCurrentAudioSrc()
                }

                // Resume the audio if we have a current music and animation is playing
                if (musicPlayerUtil.getCurrentMusic() != null) {
                    musicPlayerUtil.resumeMusic()
                }
            }
        }
    }
}

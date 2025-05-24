package app.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.queatz.db.GameMusic
import game.Game
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Audio
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Source
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLAudioElement
import r
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun GameMusicPlayer(game: Game) {
    val scope = rememberCoroutineScope()
    val musicPlayerUtil = remember { GameMusicPlayerUtil() }
    var audioElement by remember { mutableStateOf<HTMLAudioElement?>(null) }
    var currentAudioSrc by remember { mutableStateOf<String?>(null) }

    // Track current music for UI updates
    var currentMusic by remember { mutableStateOf<GameMusic?>(null) }

    // State for music notification display
    var showMusicNotification by remember { mutableStateOf(false) }
    var musicNotificationOpacity by remember { mutableStateOf(1.0f) }
    var musicNameToShow by remember { mutableStateOf("") }

    // Set the musicPlayerUtil in the Game object so it can be accessed by MusicSection
    LaunchedEffect(musicPlayerUtil, game) {
        game.musicPlayerUtil = musicPlayerUtil
    }

    // Update currentMusic and currentAudioSrc when they change in the util
    LaunchedEffect(Unit) {
        // We don't need to load all music at once anymore
        // We'll load music on demand when markers are encountered
        currentMusic = musicPlayerUtil.getCurrentMusic()
        currentAudioSrc = musicPlayerUtil.getCurrentAudioSrc()
    }

    // Handle music notification display and fade-out
    LaunchedEffect(currentMusic) {
        currentMusic?.let { music ->
            // Show notification with the music name
            musicNameToShow = music.name ?: "Unknown Music"
            showMusicNotification = true
            musicNotificationOpacity = 1.0f

            // Wait for 5 seconds
            delay(5000)

            // Fade out over 1 second
            val fadeSteps = 10
            val fadeDelay = 1000 / fadeSteps

            for (i in 1..fadeSteps) {
                musicNotificationOpacity = 1.0f - (i.toFloat() / fadeSteps)
                delay(fadeDelay.milliseconds)
            }

            // Hide notification
            showMusicNotification = false
        }
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
                // Preload audio for prompt playback
                attr("preload", "auto")

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
                    attr("type", "audio/mpeg")
                })
            }
        }
    }
    // Update audio element when source changes
    LaunchedEffect(audioElement, currentAudioSrc) {
        currentAudioSrc?.let { src ->
            audioElement?.apply {
                console.log("Audio source changed, src =", src)
                oncanplay = {
                    console.log("Audio canplay event for src =", src)
                    try {
                        play()
                    } catch (e: Throwable) {
                        console.error("Error playing audio:", e)
                    }
                    Unit
                }
                onerror = { e, _, _, _, _ ->
                    console.error("Audio error event for src =", src, e)
                }
                onplay = {
                    console.log("Audio playback started for src =", src, "at time", this.currentTime)
                    Unit
                }
                try {
                    load()
                } catch (e: Throwable) {
                    console.error("Error loading audio:", e)
                }
            }
        }
    }

    val time = game.animationData.collectCurrentTime()

    // Process markers during animation based on currentTime changes
    LaunchedEffect(time) {
        scope.launch {
            musicPlayerUtil.processMarkersAtTime(game, time)
        }
        currentMusic = musicPlayerUtil.getCurrentMusic()
        currentAudioSrc = musicPlayerUtil.getCurrentAudioSrc()
    }

    // Set up state collection to handle play state changes
    val isPlaying by game.playStateFlow.collectAsState(false)

    LaunchedEffect(isPlaying) {
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

    // Music notification overlay
    if (showMusicNotification && musicNameToShow.isNotEmpty()) {
        Div({
            style {
                position(Position.Absolute)
                bottom(4.r)
                right(2.r)
                backgroundColor(rgba(0, 0, 0, 0.7f * musicNotificationOpacity))
                color(rgba(255, 255, 255, musicNotificationOpacity))
                padding(1.r, 2.r)
                borderRadius(1.r)
                fontSize(18.px)
                fontWeight("bold")
                property("z-index", "100")
                property("pointer-events", "none")
                property("transition", "opacity 0.1s ease-out")
            }
        }) {
            Text("🎵 $musicNameToShow")
        }
    }
}

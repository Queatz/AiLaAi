package app.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.gameMusic
import app.ailaai.api.gameMusics
import baseUrl
import com.queatz.db.GameMusic
import com.queatz.db.MarkerEvent
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

/**
 * Utility class for playing music in the game
 */
class GameMusicPlayerUtil {
    private var musicList: List<GameMusic> = emptyList()
    private var currentMusic: GameMusic? = null
    // Currently playing marker with duration, for stopping music at its end
    private var currentMarker: AnimationMarker? = null
    private var audioElement: HTMLAudioElement? = null
    private var currentAudioSrc: String? = null
    // Track last processed time to catch markers in between frames
    private var lastTime: Double = 0.0

    /**
     * Load music list from API
     * Note: This loads all music at once, which may not be efficient.
     * Consider using loadMusicById for on-demand loading.
     */
    suspend fun loadMusicList(onError: (Throwable) -> Unit = {}, onSuccess: (List<GameMusic>) -> Unit = {}) {
        api.gameMusics(
            onError = { error ->
                console.error("Failed to load music", error)
                onError(error)
            }
        ) { musics ->
            musicList = musics
            onSuccess(musics)
        }
    }

    /**
     * Load a single music by ID from the API
     */
    suspend fun loadMusicById(musicId: String, onError: (Throwable) -> Unit = {}, onSuccess: (GameMusic?) -> Unit = {}) {
        // First check if we already have this music in our list
        val existingMusic = musicList.find { it.id == musicId }
        if (existingMusic != null) {
            onSuccess(existingMusic)
            return
        }

        // If not, fetch it from the API
        api.gameMusic(
            id = musicId,
            onError = { error ->
                console.error("Failed to load music with ID: $musicId", error)
                onError(error)
            },
            onSuccess = { music ->
                // Add to our list if not already there
                if (musicList.none { it.id == music.id }) {
                    musicList = musicList + listOf(music)
                }
                onSuccess(music)
            }
        )
    }

    /**
     * Get the current music list
     */
    fun getMusicList(): List<GameMusic> = musicList

    /**
     * Set the music list (useful for testing or when list is loaded elsewhere)
     */
    fun setMusicList(list: List<GameMusic>) {
        musicList = list
    }

    /**
     * Play music by ID, loading from API if not already present
     */
    suspend fun playMusicById(musicId: String) {
        // Try to find in current list
        val musicToPlay = musicList.find { it.id == musicId }
        if (musicToPlay?.audio != null) {
            val path = musicToPlay.audio!!
            val url = when {
                path.startsWith("http") -> path
                path.startsWith("/") -> "$baseUrl$path"
                else -> "$baseUrl/$path"
            }
            console.log("Playing music by ID:", musicToPlay.name, "url=", url)
            currentAudioSrc = url
            currentMusic = musicToPlay
        } else {
            console.log("No matching music found for ID: $musicId; attempting to load from API")
            api.gameMusic(
                id = musicId,
                onError = { error ->
                    console.error("Failed to load music with ID: $musicId", error)
                },
                onSuccess = { music ->
                    if (musicList.none { it.id == music.id }) {
                        musicList = musicList + listOf(music)
                    }
                    val path = music.audio!!
                    val url = "$baseUrl/$path"
                    console.log("Playing loaded music:", music.name, "url=", url)
                    currentAudioSrc = url
                    currentMusic = music
                }
            )
        }
    }

    /**
     * Play music for a marker, restarting same track if triggered again
     */
    suspend fun playMusicForMarker(marker: AnimationMarker) {
        console.log("playMusicForMarker called for marker:", marker.id, "time:", marker.time, "duration:", marker.duration, "event:", marker.event)
        currentMarker = marker
        val event = marker.event
        if (event is PlayMusicEvent) {
            // If same music is already playing, restart from beginning
            if (currentMusic?.id == event.musicId) {
                audioElement?.let {
                    it.currentTime = 0.0
                    try {
                        it.play()
                    } catch (_: Throwable) {}
                }
            } else {
                playMusicById(event.musicId)
            }
        }
    }

    /**
     * Stop current music
     */
    fun stopMusic() {
        audioElement?.pause()
        audioElement?.src = ""
        currentMusic = null
        currentAudioSrc = null
    }

    /**
     * Pause current music
     */
    fun pauseMusic() {
        audioElement?.pause()
    }

    /**
     * Resume current music
     */
    fun resumeMusic() {
        audioElement?.play()
    }

    /**
     * Get current music
     */
    fun getCurrentMusic(): GameMusic? = currentMusic

    /**
     * Get current audio source
     */
    fun getCurrentAudioSrc(): String? = currentAudioSrc

    /**
     * Set audio element
     */
    fun setAudioElement(element: HTMLAudioElement?) {
        audioElement = element
    }

    /**
     * Process markers at current time, catching those between lastTime and time
     */
    suspend fun processMarkersAtTime(game: Game, time: Double) {
        // Stop music if current marker exceeded its duration
        currentMarker?.let { m ->
            if (m.duration > 0.0 && time >= m.time + m.duration) {
                stopMusic()
                currentMarker = null
            }
        }
        // Reset tracking if timeline rewound
        if (time < lastTime) {
            lastTime = 0.0
        }
        // Find markers between lastTime (exclusive) and current time (inclusive)
        val markersToPlay = game.animationData._markers.filter { marker ->
            marker.time > lastTime && marker.time <= time
        }
        if (markersToPlay.isNotEmpty()) {
            console.log("Found ${markersToPlay.size} markers between $lastTime and $time")
            markersToPlay.forEach { marker ->
                playMusicForMarker(marker)
            }
        }
        lastTime = time
    }
}

// Note: We're not implementing the audio element handling here.
// Instead, we'll modify GameMusicPlayer to use this utility class.

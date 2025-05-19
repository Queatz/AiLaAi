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
    private var audioElement: HTMLAudioElement? = null
    private var currentAudioSrc: String? = null

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
     * Play music by ID
     */
    fun playMusicById(musicId: String) {
        val musicToPlay = musicList.find { it.id == musicId }
        if (musicToPlay?.audio != null) {
            console.log("Playing music by ID: ${musicToPlay.name} ($baseUrl${musicToPlay.audio})")

            // Update the current audio source
            currentAudioSrc = "$baseUrl${musicToPlay.audio}"
            currentMusic = musicToPlay

            // The audio element will be updated by the composable
        } else {
            console.log("No matching music found for ID: $musicId")
            console.log("Available music: ${musicList.map { "${it.id}: ${it.name}" }}")
        }
    }

    /**
     * Play music for a marker
     */
    fun playMusicForMarker(marker: AnimationMarker) {
        // Check if the marker has a PlayMusicEvent
        val event = marker.event
        if (event is PlayMusicEvent) {
            // Play music by ID from the event
            playMusicById(event.musicId)
        } else {
            // Legacy fallback: try to find music by name
            val musicToPlay = musicList.find { it.name == marker.name }
            if (musicToPlay?.audio != null) {
                console.log("Playing music by name (legacy): ${musicToPlay.name} ($baseUrl${musicToPlay.audio}) at marker time ${marker.time}")

                // Update the current audio source
                currentAudioSrc = "$baseUrl${musicToPlay.audio}"
                currentMusic = musicToPlay
            } else {
                console.log("No matching music found for marker: ${marker.name} at time ${marker.time}")
                console.log("Available music: ${musicList.map { it.name }}")
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
     * Process markers at current time
     */
    fun processMarkersAtTime(game: Game, time: Double) {
        // Find markers at or very close to the current time
        val markersAtTime = game.animationData.markers.filter { marker ->
            abs(marker.time - time) < 1.0 // Within 1 second for more reliable detection
        }

        // Play music for any markers at this time
        if (markersAtTime.isNotEmpty()) {
            console.log("Found ${markersAtTime.size} markers at time: $time")
            markersAtTime.forEach { marker ->
                playMusicForMarker(marker)
            }
        }
    }
}

// Note: We're not implementing the audio element handling here.
// Instead, we'll modify GameMusicPlayer to use this utility class.

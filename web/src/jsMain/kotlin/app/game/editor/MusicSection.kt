package app.game.editor

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.createGameMusic
import app.ailaai.api.gameMusics
import app.ailaai.api.uploadAudio
import app.components.HorizontalSpacer
import app.nav.NavSearchInput
import baseUrl
import com.queatz.db.GameMusic
import components.IconButton
import game.AnimationMarker
import game.Game
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Audio
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Source
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLAudioElement
import pickAudio
import r
import toBytes
import kotlin.math.abs

@Composable
fun MusicSection(game: Game?) {
    val scope = rememberCoroutineScope()
    var musicList by remember { mutableStateOf<List<GameMusic>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var currentMusic by remember { mutableStateOf<GameMusic?>(null) }
    var audioElement by remember { mutableStateOf<HTMLAudioElement?>(null) }
    var currentAudioSrc by remember { mutableStateOf<String?>(null) }

    // Search functionality
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Load music list on component mount
    LaunchedEffect(Unit) {
        isLoading = true
        api.gameMusics(
            onError = {
                console.error("Failed to load music", it)
                isLoading = false
            }
        ) { musics ->
            musicList = musics
            isLoading = false
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
    Audio({
        style {
            display(DisplayStyle.None)
        }

        // Set loop attribute to make music loop during animation
        attr("loop", "true")

        ref {
            audioElement = it

            onDispose { }
        }
    }) {
        // Only add Source when we have a valid audio URL
        currentAudioSrc?.let { src ->
            Source({
                attr("src", src)
                attr("type", "audio/mp4") // Assuming MP4 format, adjust if needed
            })
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
            console.log("Playing music: ${musicToPlay.name} at marker time ${marker.time}")

            // Update the current audio source
            currentAudioSrc = "$baseUrl/${musicToPlay.audio}"
            currentMusic = musicToPlay

            // The LaunchedEffect will handle loading the audio
            // and we'll play it after it's loaded
        } else {
            console.log("No matching music found for marker: ${marker.name} at time ${marker.time}")
            // List available music for debugging
            console.log("Available music: ${musicList.map { it.name }}")
        }
    }

    // Register a callback with the game to check for markers during animation
    LaunchedEffect(game, musicList) {
        game?.let { g ->
            // Add a callback to check for markers during animation
            g.animationData.onTimeUpdate = { time ->
                // Find markers at or very close to the current time
                // Use a wider window for more reliable detection
                val markersAtTime = g.animationData.markers.filter { marker -> 
                    abs(marker.time - time) < 1.0 // Within 1 second for more reliable detection
                }

                // Play music for any markers at this time
                markersAtTime.forEach { marker ->
                    playMusicAtMarker(marker)
                }
            }

            // For backward compatibility
            g.onPlayStateChanged = { isPlaying ->
                if (!isPlaying) {
                    // Pause the audio when animation is paused
                    audioElement?.pause()
                } else if (currentMusic != null) {
                    // Resume the audio if we have a current music and animation is playing
                    audioElement?.play()
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

    // Upload music function
    fun uploadMusic() {
        isUploading = true
        pickAudio { file ->
            scope.launch {
                try {
                    val audioBytes = file.toBytes()

                    api.uploadAudio(
                        audio = audioBytes,
                        contentType = file.type,
                        filename = file.name,
                        onError = {
                            console.error("Failed to upload audio", it)
                            isUploading = false
                        }
                    ) { response ->
                        // Create GameMusic object
                        val gameMusic = GameMusic(
                            name = file.name.substringBeforeLast("."),
                            audio = response.urls.firstOrNull(),
                            published = false
                        )

                        // Save to database
                        api.createGameMusic(
                            gameMusic = gameMusic,
                            onError = {
                                console.error("Failed to create game music", it)
                                isUploading = false
                            }
                        ) { savedMusic ->
                            // Add to list
                            musicList = musicList + savedMusic
                            isUploading = false
                        }
                    }
                } catch (e: Throwable) {
                    console.error("Error processing audio file", e)
                    isUploading = false
                }
            }
        }
    }

    // Add music to current frame
    fun addMusicToCurrentFrame(music: GameMusic) {
        game?.let { g ->
            // Create a marker with the music name at the current time
            val marker = g.animationData.addMarker(music.name ?: "Unnamed Music")
            // Default to 0 (play until end)
            marker.duration = 0.0
        }
    }

    PanelSection(
        title = "Music",
        icon = "music_note",
        enabled = true,
        initiallyExpanded = false
    ) {
        // Search icon and input
        Div({
            style {
                property("display", "flex")
                property("align-items", "center")
                marginBottom(1.r)
            }
        }) {
            if (showSearch) {
                NavSearchInput(
                    value = searchQuery,
                    onChange = { searchQuery = it },
                    onDismissRequest = { 
                        showSearch = false
                        searchQuery = ""
                    }
                )
            } else {
                IconButton(
                    name = "search",
                    title = "Search music"
                ) {
                    showSearch = true
                }
            }
        }

        // Upload and Add Music buttons
        Div({
            style {
                display(DisplayStyle.Flex)
                gap(1.r)
                marginBottom(1.r)
            }
        }) {
            Button({
                classes(Styles.button)
                onClick { uploadMusic() }
                if (isUploading) {
                    attr("disabled", "true")
                }
            }) {
                Text(if (isUploading) "Uploading..." else "Upload Music")
            }
        }

        // Music library
        if (isLoading) {
            Text("Loading music...")
        } else if (musicList.isEmpty()) {
            Text("No music available. Upload some music to get started.")
        } else {
            Toolbox(
                styles = {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(0.5.r)
                    marginTop(1.r)
                }
            ) {
                // Filter music based on search query
                val filteredMusic = if (searchQuery.isNotEmpty()) {
                    musicList.filter { music ->
                        // Filter by name, ID, or creation date
                        val name = music.name ?: ""
                        val id = music.id ?: ""
                        val createdAtStr = music.createdAt?.toString() ?: ""

                        name.contains(searchQuery, ignoreCase = true) || 
                        id.contains(searchQuery, ignoreCase = true) ||
                        createdAtStr.contains(searchQuery, ignoreCase = true)
                    }
                } else {
                    musicList
                }

                filteredMusic.forEach { music ->
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            padding(0.5.r)
                            gap(0.5.r)
                        }
                    }) {
                        // Play/pause button
                        IconButton(
                            name = if (currentMusic?.id == music.id) "pause" else "play_arrow",
                            title = if (currentMusic?.id == music.id) "Pause" else "Play",
                        ) {
                            if (currentMusic?.id == music.id) {
                                audioElement?.pause()
                                currentMusic = null
                            } else {
                                if (music.audio != null) {
                                    // Update the current audio source
                                    currentAudioSrc = "$baseUrl/${music.audio}"
                                    currentMusic = music
                                    // The LaunchedEffect will handle loading and playing the audio
                                }
                            }
                        }

                        // Music name
                        Text(music.name ?: "Unnamed Music")

                        HorizontalSpacer(fill = true)

                        // Add to current frame button
                        IconButton(
                            name = "add",
                            title = "Add to current frame",
                        ) {
                            addMusicToCurrentFrame(music)
                        }
                    }
                }
            }
        }
    }
}

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
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.boxSizing
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Audio
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Source
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLAudioElement
import pickAudio
import r
import toBytes
import kotlin.math.abs

@Composable
fun MusicSection(game: Game?, mapParam: game.Map) {
    val scope = rememberCoroutineScope()
    var musicList by remember { mutableStateOf<List<GameMusic>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var currentMusic by remember { mutableStateOf<GameMusic?>(null) }
    var audioElement by remember { mutableStateOf<HTMLAudioElement?>(null) }
    var currentAudioSrc by remember { mutableStateOf<String?>(null) }

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

    // Convert GameMusic to GameMusicAsset
    val musicAssets = musicList.map { GameMusicAsset(it) }

    // Track selected music ID
    var selectedMusicId by remember { mutableStateOf<String?>(null) }

    // Custom tool renderer for music assets
    val renderMusicTool = @Composable { music: GameMusic ->
        Div({
            style {
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                padding(0.5.r)
                gap(0.5.r)
                width(100.percent)
                boxSizing("border-box")
                // Add a background color if this music is selected
                if (selectedMusicId == music.id) {
                    backgroundColor(Styles.colors.primary)
                    borderRadius(.5.r)
                }
                // Make the div clickable
                cursor("pointer")
            }
            // Add click handler to select/deselect this music
            onClick {
                val newSelectedId = if (selectedMusicId == music.id) null else music.id
                selectedMusicId = newSelectedId

                // Update the current music in the map
                if (mapParam != null) {
                    if (newSelectedId != null) {
                        mapParam.setCurrentGameMusic(music)
                    } else {
                        mapParam.setCurrentGameMusic(null)
                    }
                }
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

    // Use the generic AssetSection component with custom content
    AssetSection(
        title = "Music",
        icon = "music_note",
        assets = musicAssets,
        isLoading = isLoading,
        isCreating = isUploading,
        selectedAssetId = selectedMusicId,
        onAssetSelected = { musicAsset ->
            // Toggle selection
            val newSelectedId = if (musicAsset?.id == selectedMusicId) null else musicAsset?.id
            selectedMusicId = newSelectedId

            // Find the selected music in the list
            if (newSelectedId != null) {
                val selectedMusic = musicList.find { it.id == newSelectedId }
                // Update the current music
                currentMusic = selectedMusic

                // Update the current music in the map
                if (mapParam != null && selectedMusic != null) {
                    mapParam.setCurrentGameMusic(selectedMusic)
                }
            } else {
                // If deselected, set to null
                currentMusic = null

                // Clear the current music in the map
                if (mapParam != null) {
                    mapParam.setCurrentGameMusic(null)
                }
            }
        },
        onCreateAsset = {
            uploadMusic()
        },
        assetToTool = { musicAsset ->
            Tool(
                id = musicAsset.id,
                name = musicAsset.name,
                photoUrl = "$baseUrl/assets/icons/music_note.svg", // Default music icon
                description = musicAsset.description
            )
        },
        searchFilter = { musicAsset, query ->
            musicAsset.name.contains(query, ignoreCase = true) ||
            musicAsset.id.contains(query, ignoreCase = true) ||
            musicAsset.createdAt?.toString()?.contains(query, ignoreCase = true) ?: false
        },
        createButtonText = "Upload Music",
        emptyText = "No music available. Upload some music to get started.",
        loadingText = "Loading music...",
        processingText = "Uploading music...",
        customContent = { filteredAssets: List<GameMusicAsset> ->
            // Custom content for the music section
            Toolbox(
                styles = {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(0.5.r)
                }
            ) {
                // For each filtered music asset, find the original GameMusic and render it
                filteredAssets.forEach { musicAsset ->
                    val originalMusic = musicList.find { it.id == musicAsset.id }
                    if (originalMusic != null) {
                        renderMusicTool(originalMusic)
                    }
                }
            }
        }
    )
}

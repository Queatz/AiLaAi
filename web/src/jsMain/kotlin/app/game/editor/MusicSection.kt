package app.game.editor

import Styles
import androidx.compose.runtime.Composable
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
import com.queatz.db.PlayMusicEvent
import components.IconButton
import game.Game
import kotlinx.coroutines.launch
import app.game.editor.assetManager
import app.game.editor.rememberMusic
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
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import pickAudio
import r
import toBytes

@Composable
fun MusicSection(
    game: Game?,
    mapParam: game.Map,
    clearSelection: Boolean = false
) {
    val scope = rememberCoroutineScope()
    // Use the AssetManager to get music
    val musicList = rememberMusic()
    var isLoading by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    // Track currently playing music
    var currentlyPlayingMusicId by remember { mutableStateOf<String?>(null) }

    // Update currentlyPlayingMusicId when the game's musicPlayerUtil changes
    LaunchedEffect(game?.musicPlayerUtil) {
        game?.musicPlayerUtil?.getCurrentMusic()?.id?.let { musicId ->
            currentlyPlayingMusicId = musicId
        }
    }

    // Load music list on component mount
    LaunchedEffect(Unit) {
        isLoading = true
        api.gameMusics(
            onError = {
                console.error("Failed to load music", it)
                isLoading = false
            }
        ) { musics ->
            // Update the AssetManager with the loaded music
            assetManager.setMusic(musics)
            isLoading = false
        }
    }

    // Upload music function
    fun uploadMusic() {
        pickAudio { file ->
            isUploading = true
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
                            // Add to the AssetManager
                            assetManager.addMusic(savedMusic)
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
            // Create a marker with a descriptive name at the current time
            val markerName = "Play Music: ${music.name ?: "Unnamed Music"}"
            val marker = g.animationData.addMarker(markerName)

            // Default to 0 (play until end)
            marker.duration = 0.0

            // Set music markers to not be visible by default
            marker.visible = false

            // Set the PlayMusicEvent with the music ID
            marker.event = PlayMusicEvent(musicId = music.id ?: "")

            // Force update of markers list to trigger UI recomposition
            g.animationData.updateMarkers()

            console.log("Added music marker with event: ${marker.event}")
        }
    }

    // Convert GameMusic to GameMusicAsset
    val musicAssets = musicList.map { GameMusicAsset(it) }

    // Track selected music ID
    var selectedMusicId by remember { mutableStateOf<String?>(null) }

    // Clear selection when requested externally
    LaunchedEffect(clearSelection) {
        if (clearSelection) {
            selectedMusicId = null
            mapParam.setCurrentGameMusic(null)
        }
    }

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
                if (newSelectedId != null) {
                    mapParam.setCurrentGameMusic(music)
                } else {
                    mapParam.setCurrentGameMusic(null)
                }
            }
        }) {
            // Play/pause button
            IconButton(
                name = if (currentlyPlayingMusicId == music.id) "pause" else "play_arrow",
                title = if (currentlyPlayingMusicId == music.id) "Pause" else "Play",
            ) {
                // Get the musicPlayerUtil from the game object
                val musicPlayerUtil = game?.musicPlayerUtil
                if (musicPlayerUtil != null) {
                    if (currentlyPlayingMusicId == music.id) {
                        // Stop the currently playing music
                        musicPlayerUtil.stopMusic()
                        currentlyPlayingMusicId = null
                    } else {
                        if (music.audio != null && music.id != null) {
                            // Make sure the music is in the utility's list
                            if (musicPlayerUtil.getMusicList().none { it.id == music.id }) {
                                musicPlayerUtil.setMusicList(musicPlayerUtil.getMusicList() + listOf(music))
                            }

                            // Play the selected music
                            scope.launch {
                                val musicId = music.id ?: return@launch
                                musicPlayerUtil.playMusicById(musicId)
                                currentlyPlayingMusicId = musicId
                                // Start the game animation
                                game?.play()
                                // Force UI update
                                game.setTime(game.animationData.currentTime)
                            }
                        }
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

                // Update the current music in the map
                if (selectedMusic != null) {
                    mapParam.setCurrentGameMusic(selectedMusic)
                }
            } else {
                // Clear the current music in the map
                mapParam.setCurrentGameMusic(null)
            }
        },
        onCreateAsset = {
            uploadMusic()
        },
        assetToTool = { musicAsset ->
            AssetTool(
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

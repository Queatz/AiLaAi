package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.createGameTile
import app.ailaai.api.gameTiles
import app.dialog.rememberChoosePhotoDialog
import app.nav.NavSearchInput
import baseUrl
import com.queatz.db.GameTile
import components.IconButton
import components.Loading
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun TilesSection(
    map: game.Map? = null,
    onTileSelected: (() -> Unit)? = null,
    clearSelection: Boolean = false
) {
    val scope = rememberCoroutineScope()
    var tiles by remember { mutableStateOf<List<GameTile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    // Track tile creation state separately from photo generation
    var isTileCreationInProgress by remember { mutableStateOf(false) }
    val choosePhotoDialog = rememberChoosePhotoDialog(showUpload = true, aspectRatio = 1.0)

    // Search functionality
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Get the photo generation state
    val isGenerating by choosePhotoDialog.isGenerating.collectAsState()

    // Combine both states to determine if we're creating a tile
    var isCreatingTile by remember { mutableStateOf(false) }
    LaunchedEffect(isGenerating, isTileCreationInProgress) {
        isCreatingTile = isGenerating || isTileCreationInProgress
    }

    // Load tiles when the component is first rendered
    LaunchedEffect(Unit) {
        isLoading = true
        api.gameTiles(
            onSuccess = { tilesList ->
                tiles = tilesList
                isLoading = false
            }
        )
    }

    PanelSection(
        title = "Tiles",
        icon = "map",
        initiallyExpanded = true
    ) {
        // Search and button container
        Div({
            style {
                marginBottom(1.r)
            }
        }) {
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
                        title = "Search tiles"
                    ) {
                        showSearch = true
                    }
                }
            }

            // Button to create a new tile
            Button({
                classes(Styles.button)

                style {
                    width(100.percent)
                    marginBottom(1.r)
                }
                onClick {
                    // Use ChoosePhotoDialog to select, process, and upload a photo
                    scope.launch {
                        choosePhotoDialog.launch { photoUrl ->
                            // Process the photo URL
                            scope.launch {
                                isTileCreationInProgress = true
                                // Create a new tile with the photo URL
                                api.createGameTile(
                                    gameTile = GameTile(
                                        photo = photoUrl
                                    ),
                                    onSuccess = { newTile ->
                                        // Add the new tile to the list
                                        tiles = listOf(newTile) + tiles
                                        isTileCreationInProgress = false
                                    },
                                    onError = {
                                        isTileCreationInProgress = false
                                    }
                                )
                            }
                        }
                    }
                }
            }) {
                Text("Create New Tile")
            }

            // Show loading indicator when creating a tile
            if (isCreatingTile) {
                Div({
                    style {
                        padding(.5.r)
                        marginBottom(1.r)
                        property("text-align", "center")
                    }
                }) {
                    Loading()
                    Div({
                        style {
                            marginTop(.5.r)
                        }
                    }) {
                        Text("Processing tile...")
                    }
                }
            }

            // Display tiles in a grid
            if (isLoading) {
                Div({
                    style {
                        padding(1.r)
                        textAlign("center")
                    }
                }) {
                    Text("Loading tiles...")
                }
            } else if (tiles.isEmpty()) {
                Div({
                    style {
                        padding(1.r)
                        textAlign("center")
                    }
                }) {
                    Text("No tiles yet. Create your first tile!")
                }
            } else {
                // Convert GameTile objects to Tool objects for the ToolGrid
                val filteredTiles = if (searchQuery.isNotEmpty()) {
                    tiles.filter { tile ->
                        // Filter by name, ID, or creation date
                        val name = tile.name ?: ""
                        val id = tile.id ?: ""
                        val createdAtStr = tile.createdAt?.toString() ?: ""

                        name.contains(searchQuery, ignoreCase = true) ||
                                id.contains(searchQuery, ignoreCase = true) ||
                                createdAtStr.contains(searchQuery, ignoreCase = true)
                    }
                } else {
                    tiles
                }

                val tileTools = filteredTiles.map { tile ->
                    Tool(
                        id = tile.id ?: "",
                        name = "Tile",
                        photoUrl = "$baseUrl${tile.photo}",
                        description = "Created: ${tile.createdAt}"
                    )
                }

                var selectedTileId by remember { mutableStateOf<String?>(null) }

                // Clear selection when clearSelection is true
                LaunchedEffect(clearSelection) {
                    if (clearSelection) {
                        selectedTileId = null
                        map?.setCurrentGameTile(null)
                    }
                }

                Toolbox {
                    ToolGrid(
                        tools = tileTools,
                        selectedToolId = selectedTileId,
                        onToolSelected = { tool ->
                            // Toggle selection
                            val newSelectedId = if (tool.id == selectedTileId) null else tool.id
                            selectedTileId = newSelectedId

                            // Find the selected tile and pass it to the map
                            if (newSelectedId != null && map != null) {
                                val selectedTile = tiles.find { it.id == newSelectedId }
                                if (selectedTile != null) {
                                    map.setCurrentGameTile(selectedTile)
                                    // Notify that a tile was selected
                                    onTileSelected?.invoke()
                                }
                            } else if (map != null) {
                                // If deselected, set to null
                                map.setCurrentGameTile(null)
                            }
                        }
                    )
                }
            }
        }
    }
}

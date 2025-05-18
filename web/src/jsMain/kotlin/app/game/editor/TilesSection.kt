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
import baseUrl
import com.queatz.db.GameTile
import kotlinx.coroutines.launch

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

    // Convert GameTiles to GameTileAssets
    val tileAssets = tiles.map { GameTileAsset(it) }

    // Track selected tile ID
    var selectedTileId by remember { mutableStateOf<String?>(null) }

    // Clear selection when clearSelection is true
    LaunchedEffect(clearSelection) {
        if (clearSelection) {
            selectedTileId = null
            map?.setCurrentGameTile(null)
        }
    }

    // Use the generic AssetSection component
    AssetSection(
        title = "Tiles",
        icon = "map",
        assets = tileAssets,
        isLoading = isLoading,
        isCreating = isCreatingTile,
        selectedAssetId = selectedTileId,
        onAssetSelected = { tileAsset ->
            // Toggle selection
            val newSelectedId = if (tileAsset?.id == selectedTileId) null else tileAsset?.id
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
        },
        onCreateAsset = {
            // Use ChoosePhotoDialog to select, process, and upload a photo
            scope.launch {
                choosePhotoDialog.launch { photoUrl, _, _ ->
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
        },
        assetToTool = { tileAsset ->
            Tool(
                id = tileAsset.id,
                name = tileAsset.name,
                photoUrl = "$baseUrl${tileAsset.content}",
                description = tileAsset.description
            )
        },
        searchFilter = { tileAsset, query ->
            tileAsset.name.contains(query, ignoreCase = true) ||
            tileAsset.id.contains(query, ignoreCase = true) ||
            tileAsset.createdAt?.toString()?.contains(query, ignoreCase = true) ?: false
        },
        createButtonText = "Create New Tile",
        emptyText = "No tiles yet. Create your first tile!",
        loadingText = "Loading tiles...",
        processingText = "Processing tile..."
    )
}

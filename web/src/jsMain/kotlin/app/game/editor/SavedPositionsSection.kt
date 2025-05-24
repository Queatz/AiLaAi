package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.dialog.inputDialog
import com.queatz.db.SavedPositionData
import com.queatz.db.Vector3Data
import components.IconButton
import ellipsize
import game.Map
import game.SavedPositions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import lib.Color3
import lib.CreateSphereOptions
import lib.KeyboardEventTypes
import lib.KeyboardInfo
import lib.Matrix
import lib.Mesh
import lib.MeshBuilder
import lib.PointerEventTypes
import lib.PointerInfo
import lib.StandardMaterial
import lib.Vector3
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.rgba
import org.jetbrains.compose.web.css.width
import kotlin.js.Date
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun SavedPositionsSection(map: Map) {
    val scope = rememberCoroutineScope()
    var isPlacingPosition by remember { mutableStateOf(false) }
    var isRepositioningPosition by remember { mutableStateOf<String?>(null) } // ID of position being repositioned
    var previewMarker by remember { mutableStateOf<Mesh?>(null) }
    var savedPositions by remember { mutableStateOf<List<SavedPositionData>>(emptyList()) }
    var selectedPositionId by remember { mutableStateOf<String?>(null) }
    var isDialogOpen by remember { mutableStateOf(false) } // Flag to prevent multiple dialogs

    // Create a saved positions manager
    val savedPositionsManager = remember { 
        SavedPositions(map.game!!.scene).apply {
            // Set up the click handler for markers
            onMarkerClicked = { clickedPositionId ->
                // Select this position
                selectedPositionId = clickedPositionId

                // Focus the camera on this position
                focusOnMarker(clickedPositionId, map.camera)
            }
        }
    }

    // Load saved positions from the game's animation data
    LaunchedEffect(map.game) {
        map.game?.animationDataChanged?.onStart { emit(Unit) }?.collectLatest {
            // Get saved positions from the game
            val positions = map.game?.animationData?.savedPositions ?: emptyList()
            savedPositions = positions

            // Update markers in the 3D scene
            savedPositionsManager.updateMarkers(positions)
        }
    }

    // Clean up resources when the component is unmounted
    DisposableEffect(Unit) {
        onDispose {
            // Dispose of the saved positions manager
            savedPositionsManager.dispose()
        }
    }

    // Observe animation playback state and hide/show markers accordingly
    LaunchedEffect(map.game) {
        map.game?.playStateFlow?.collectLatest { isPlaying ->
            // Hide markers when animation is playing, show them when it's not
            savedPositionsManager.setVisible(!isPlaying)
        }
    }

    // Set up event listeners for placing or repositioning saved positions
    DisposableEffect(isPlacingPosition, isRepositioningPosition) {
        if (isPlacingPosition || isRepositioningPosition != null) {
            val scene = map.game?.scene ?: return@DisposableEffect onDispose {}

            // Create a preview marker (semi-transparent white sphere)
            val marker = MeshBuilder.CreateSphere("preview-marker", object : CreateSphereOptions {
                override val diameter = 0.5f
                override val segments = 16
            }, scene)

            // Create a white material with 50% alpha
            val material = StandardMaterial("preview-material", scene)
            material.diffuseColor = Color3(1f, 1f, 1f) // White color
            material.specularColor = Color3(0.1f, 0.1f, 0.1f)
            material.alpha = 0.5f // 50% transparency
            marker.material = material

            previewMarker = marker

            // Function to update the preview marker position
            val updatePreviewPosition = fun() {
                val camera = scene.activeCamera ?: return

                // Create a picking ray from the current pointer position
                val ray = scene.createPickingRay(
                    scene.pointerX,
                    scene.pointerY,
                    Matrix.Identity(),
                    camera
                )

                // Intersect with the tilemap mesh
                val pickInfo = ray.intersectsMesh(map.tilemapEditor.tilemap.mesh)
                val position = pickInfo.pickedPoint

                // Update the marker position if we have a valid pick point
                if (position != null) {
                    marker.position = position
                }
            }

            // Register pointer move observer to update preview position
            val pointerMoveObserver = scene.onPointerObservable.add(fun(info: PointerInfo) {
                if (info.type == PointerEventTypes.POINTERMOVE) {
                    updatePreviewPosition()
                }
            })

            // Register pointer down observer to place or reposition the saved position
            val pointerDownObserver = scene.onPointerObservable.add(fun(info: PointerInfo) {
                // Only process clicks when in placement or repositioning mode
                if (info.type == PointerEventTypes.POINTERDOWN && !info.event.shiftKey) {
                    // Get the current position of the preview marker
                    val position = marker.position.clone()

                    if (isPlacingPosition && !isDialogOpen) {
                        // Adding a new position
                        // Show input dialog to get position name
                        isDialogOpen = true // Set flag to prevent multiple dialogs
                        scope.launch {
                            try {
                                val name = inputDialog(
                                    title = "New Saved Position",
                                    placeholder = "Enter a name for this position",
                                    singleLine = true
                                )

                                if (!name.isNullOrBlank()) {
                                    // Create a new saved position
                                    val id = "position_${Date().getTime().toLong()}"
                                    val newPosition = SavedPositionData(
                                        id = id,
                                        name = name,
                                        position = Vector3Data(position.x, position.y, position.z)
                                    )

                                    // Add to the list
                                    val updatedPositions = savedPositions + newPosition
                                    savedPositions = updatedPositions

                                    // Update the game's saved positions
                                    map.game?.animationData?.savedPositions = updatedPositions

                                    // Update markers in the 3D scene
                                    savedPositionsManager.updateMarkers(updatedPositions)
                                }
                            } finally {
                                // Reset state regardless of success or failure
                                isPlacingPosition = false
                                isDialogOpen = false // Reset dialog flag
                                // Explicitly remove the preview marker
                                if (previewMarker != null) {
                                    map.game?.scene?.removeMesh(previewMarker!!)
                                    previewMarker = null
                                }
                            }
                        }
                    } else if (isRepositioningPosition != null) {
                        // Repositioning an existing position
                        val positionId = isRepositioningPosition
                        val positionToUpdate = savedPositions.find { it.id == positionId }

                        if (positionToUpdate != null) {
                            // Create updated position
                            val updatedPosition = positionToUpdate.copy(
                                position = Vector3Data(position.x, position.y, position.z)
                            )

                            // Update the list
                            val updatedPositions = savedPositions.map { 
                                if (it.id == positionId) updatedPosition else it 
                            }
                            savedPositions = updatedPositions

                            // Update the game's saved positions
                            map.game?.animationData?.savedPositions = updatedPositions

                            // Update markers in the 3D scene
                            savedPositionsManager.updateMarkers(updatedPositions)
                        }

                        // Reset state
                        isRepositioningPosition = null
                        // Explicitly remove the preview marker
                        if (previewMarker != null) {
                            map.game?.scene?.removeMesh(previewMarker!!)
                            previewMarker = null
                        }
                    }
                }
            })

            // Register keyboard observer to handle cancellation
            val keyboardObserver = scene.onKeyboardObservable.add(fun(info: KeyboardInfo) {
                if (info.type == KeyboardEventTypes.KEYDOWN && info.event.key == "Escape") {
                    // Cancel placing or repositioning
                    isPlacingPosition = false
                    isRepositioningPosition = null

                    // Explicitly remove the preview marker when Escape is pressed
                    if (previewMarker != null) {
                        map.game?.scene?.removeMesh(previewMarker!!)
                        previewMarker = null
                    }
                }
            })

            // Initial position update
            updatePreviewPosition()

            onDispose {
                // We can't remove observers in the current API, but we can clean up the preview marker
                if (previewMarker != null) {
                    scene.removeMesh(previewMarker!!)
                    previewMarker = null
                }
            }
        } else {
            // Clean up the preview marker if we're not placing or repositioning a position
            if (previewMarker != null) {
                map.game?.scene?.removeMesh(previewMarker!!)
                previewMarker = null
            }

            onDispose { }
        }
    }

    // Function to start placing a new saved position
    fun startNewPosition() {
        // Deselect any tile or object that is currently selected to prevent drawing
        map.setCurrentGameTile(null)
        map.setCurrentGameObject(null)

        // Set the flag to indicate we're in position placement mode
        isPlacingPosition = true
    }

    // Function to rename a saved position
    fun renamePosition(position: SavedPositionData) {
        // Prevent multiple dialogs
        if (isDialogOpen) return

        isDialogOpen = true
        scope.launch {
            try {
                val newName = inputDialog(
                    title = "Rename Position",
                    placeholder = "Enter a new name for this position",
                    defaultValue = position.name,
                    singleLine = true
                )

                if (!newName.isNullOrBlank() && newName != position.name) {
                    // Create updated position
                    val updatedPosition = position.copy(name = newName)

                    // Update the list
                    val updatedPositions = savedPositions.map { 
                        if (it.id == position.id) updatedPosition else it 
                    }
                    savedPositions = updatedPositions

                    // Update the game's saved positions
                    map.game?.animationData?.savedPositions = updatedPositions

                    // Update markers in the 3D scene
                    savedPositionsManager.updateMarkers(updatedPositions)
                }
            } finally {
                isDialogOpen = false
            }
        }
    }

    // Function to delete a saved position
    fun deletePosition(position: SavedPositionData) {
        // Remove from the list
        val updatedPositions = savedPositions.filter { it.id != position.id }
        savedPositions = updatedPositions

        // Update the game's saved positions
        map.game?.animationData?.savedPositions = updatedPositions

        // Update markers in the 3D scene
        savedPositionsManager.updateMarkers(updatedPositions)

        // Clear selection if this position was selected
        if (selectedPositionId == position.id) {
            selectedPositionId = null
        }
    }

    // Function to start repositioning a saved position
    fun updatePosition(position: SavedPositionData) {
        // Deselect any tile or object that is currently selected to prevent drawing
        map.setCurrentGameTile(null)
        map.setCurrentGameObject(null)

        // Set the flag to indicate we're in position repositioning mode
        isRepositioningPosition = position.id
    }

    // Only show this section in editable mode
    if (map.game?.editable == true) {
        PanelSection(
            title = "Saved Positions",
            icon = "place",
            initiallyExpanded = true,
            closeOtherPanels = true
        ) {
            // Add new position button
            Button({
                classes(Styles.button)
                style {
                    marginBottom(1.r)
                    width(100.percent)
                }
                onClick {
                    startNewPosition()
                }
                if (isPlacingPosition || isRepositioningPosition != null) {
                    attr("disabled", "true")
                }
            }) {
                when {
                    isPlacingPosition -> Text("Click on the scene to place position...")
                    isRepositioningPosition != null -> {
                        val position = savedPositions.find { it.id == isRepositioningPosition }
                        Text("Click on the scene to reposition ${position?.name ?: "position"}...")
                    }
                    else -> Text("Add Saved Position")
                }
            }

            // List of saved positions
            if (savedPositions.isEmpty()) {
                Div({
                    style {
                        padding(1.r)
                        opacity(0.7f)
                    }
                }) {
                    Text("No saved positions yet. Click 'Add Saved Position' to add one.")
                }
            } else {
                savedPositions.forEach { position ->
                    val isSelected = position.id == selectedPositionId

                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems("center")
                            padding(0.5.r)
                            marginBottom(0.5.r)
                            borderRadius(0.5.r)
                            overflow("hidden")
                            cursor("pointer")
                            if (isSelected) {
                                backgroundColor(rgba(0, 123, 255, 0.1))
                            }
                        }
                        onClick {
                            // Select this position
                            selectedPositionId = position.id

                            // Focus the camera on this position
                            savedPositionsManager.focusOnMarker(position.id, map.camera)
                        }
                    }) {
                        // Position name
                        Div({
                            style {
                                ellipsize()
                            }
                        }) {
                            Text(position.name)
                        }

                        // Action buttons
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                            }
                        }) {
                            // Jump to position button
                            IconButton(
                                name = "travel_explore",
                                title = "Jump to position",
                                styles = {
                                    marginRight(0.5.r)
                                }
                            ) {
                                // Focus the camera on this position
                                savedPositionsManager.focusOnMarker(position.id, map.camera)
                            }

                            // Update position button
                            IconButton(
                                name = "gps_fixed",
                                title = "Update to current position",
                                styles = {
                                    marginRight(0.5.r)
                                }
                            ) {
                                updatePosition(position)
                            }

                            // Rename button
                            IconButton(
                                name = "edit",
                                title = "Rename",
                                styles = {
                                    marginRight(0.5.r)
                                }
                            ) {
                                renamePosition(position)
                            }

                            // Delete button
                            IconButton(
                                name = "delete",
                                title = "Delete"
                            ) {
                                deletePosition(position)
                            }
                        }
                    }
                }
            }
        }
    }
}

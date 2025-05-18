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
import app.ailaai.api.deleteGameMusic
import app.ailaai.api.deleteGameObject
import app.ailaai.api.deleteGameTile
import app.ailaai.api.updateGameMusic
import app.ailaai.api.updateGameObject
import app.ailaai.api.updateGameTile
import app.dialog.dialog
import baseUrl
import game.Map
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.NumberInput
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput
import r

@Composable
fun CurrentSelectionSection(map: Map) {
    // Track the currently selected asset
    val currentTile by map.tilemapEditor.getCurrentGameTileState()
    val currentObject by map.tilemapEditor.getCurrentGameObjectState()
    val currentMusic by map.tilemapEditor.getCurrentGameMusicState()

    // Track editing state
    var isEditing by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var newWidth by remember { mutableStateOf(1.0) }
    var newHeight by remember { mutableStateOf(1.0) }

    val scope = rememberCoroutineScope()

    // Update when the selected assets change
    LaunchedEffect(
        map.tilemapEditor.getCurrentGameTileState(),
        map.tilemapEditor.getCurrentGameObjectState(),
        map.tilemapEditor.getCurrentGameMusicState()
    ) {

        // Reset editing state when selection changes
        isEditing = false

        // Initialize name, description, and dimensions for editing
        currentTile?.let { tile ->
            newName = tile.name ?: "Unnamed Tile"
            newDescription = tile.description ?: ""
        }

        currentObject?.let { obj ->
            newName = obj.name ?: "Unnamed Object"
            newDescription = obj.description ?: ""
            newWidth = obj.width?.toDoubleOrNull() ?: 1.0
            newHeight = obj.height?.toDoubleOrNull() ?: 1.0
        }

        currentMusic?.let { music ->
            newName = music.name ?: "Unnamed Music"
            newDescription = music.description ?: ""
        }
    }

    // Determine if any asset is selected
    val hasSelection = currentTile != null || currentObject != null || currentMusic != null

    PanelSection(
        title = "Current Asset",
        icon = "select_all",
        enabled = hasSelection,
        initiallyExpanded = hasSelection,
        closeOtherPanels = true
    ) {
        if (!hasSelection) {
            Div({
                style {
                    padding(1.r)
                }
            }) {
                Text("No asset selected. Select a tile, object, or music from the library.")
            }
        } else {
            // Display the selected asset
            Div({
                style {
                    padding(1.r)
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(1.r)
                }
            }) {
                // Asset preview and info
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        gap(1.r)
                        marginBottom(1.r)
                    }
                }) {
                    // Asset preview image
                    when {
                        currentTile != null -> {
                            currentTile?.photo?.let { photo ->
                                Img(src = "$baseUrl$photo") {
                                    style {
                                        width(100.r)
                                        property("height", "100px")
                                        property("object-fit", "contain")
                                    }
                                }
                            }
                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                    gap(0.5.r)
                                }
                            }) {
                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Type")
                                    Div({
                                        style {
                                            padding(0.5.r)
                                            property("background-color", "rgba(0, 0, 0, 0.05)")
                                            property("border-radius", "4px")
                                        }
                                    }) {
                                        Text("Tile")
                                    }
                                }

                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Name")
                                    Div({
                                        style {
                                            padding(0.5.r)
                                            property("background-color", "rgba(0, 0, 0, 0.05)")
                                            property("border-radius", "4px")
                                        }
                                    }) {
                                        Text(currentTile?.name ?: "Unnamed Tile")
                                    }
                                }

                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Description")
                                    Div({
                                        style {
                                            padding(0.5.r)
                                            property("background-color", "rgba(0, 0, 0, 0.05)")
                                            property("border-radius", "4px")
                                        }
                                    }) {
                                        Text(currentTile?.description ?: "No description")
                                    }
                                }

                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Published")
                                    Div({
                                        style {
                                            padding(0.5.r)
                                            property("background-color", "rgba(0, 0, 0, 0.05)")
                                            property("border-radius", "4px")
                                        }
                                    }) {
                                        Text(if (currentTile?.published == true) "Yes" else "No")
                                    }
                                }
                            }
                        }
                        currentObject != null -> {
                            currentObject?.photo?.let { photo ->
                                Img(src = "$baseUrl$photo") {
                                    style {
                                        width(100.r)
                                        property("height", "100px")
                                        property("object-fit", "contain")
                                    }
                                }
                            }
                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                    gap(0.5.r)
                                }
                            }) {
                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Type")
                                    Div({
                                        style {
                                            padding(0.5.r)
                                            property("background-color", "rgba(0, 0, 0, 0.05)")
                                            property("border-radius", "4px")
                                        }
                                    }) {
                                        Text("Object")
                                    }
                                }

                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Name")
                                    Div({
                                        style {
                                            padding(0.5.r)
                                            property("background-color", "rgba(0, 0, 0, 0.05)")
                                            property("border-radius", "4px")
                                        }
                                    }) {
                                        Text(currentObject?.name ?: "Unnamed Object")
                                    }
                                }

                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Description")
                                    Div({
                                        style {
                                            padding(0.5.r)
                                            property("background-color", "rgba(0, 0, 0, 0.05)")
                                            property("border-radius", "4px")
                                        }
                                    }) {
                                        Text(currentObject?.description ?: "No description")
                                    }
                                }

                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Size")
                                    Div({
                                        style {
                                            padding(0.5.r)
                                            property("background-color", "rgba(0, 0, 0, 0.05)")
                                            property("border-radius", "4px")
                                        }
                                    }) {
                                        Text("${currentObject?.width ?: "1"} x ${currentObject?.height ?: "1"}")
                                    }
                                }

                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Published")
                                    Div({
                                        style {
                                            padding(0.5.r)
                                            property("background-color", "rgba(0, 0, 0, 0.05)")
                                            property("border-radius", "4px")
                                        }
                                    }) {
                                        Text(if (currentObject?.published == true) "Yes" else "No")
                                    }
                                }
                            }
                        }
                        currentMusic != null -> {
                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                    gap(0.5.r)
                                }
                            }) {
                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Type")
                                    Div({
                                        style {
                                            padding(0.5.r)
                                            property("background-color", "rgba(0, 0, 0, 0.05)")
                                            property("border-radius", "4px")
                                        }
                                    }) {
                                        Text("Music")
                                    }
                                }

                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Name")
                                    Div({
                                        style {
                                            padding(0.5.r)
                                            property("background-color", "rgba(0, 0, 0, 0.05)")
                                            property("border-radius", "4px")
                                        }
                                    }) {
                                        Text(currentMusic?.name ?: "Unnamed Music")
                                    }
                                }

                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Description")
                                    Div({
                                        style {
                                            padding(0.5.r)
                                            property("background-color", "rgba(0, 0, 0, 0.05)")
                                            property("border-radius", "4px")
                                        }
                                    }) {
                                        Text(currentMusic?.description ?: "No description")
                                    }
                                }

                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Duration")
                                    Div({
                                        style {
                                            padding(0.5.r)
                                            property("background-color", "rgba(0, 0, 0, 0.05)")
                                            property("border-radius", "4px")
                                        }
                                    }) {
                                        Text("${currentMusic?.duration ?: "Unknown"} seconds")
                                    }
                                }

                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Published")
                                    Div({
                                        style {
                                            padding(0.5.r)
                                            property("background-color", "rgba(0, 0, 0, 0.05)")
                                            property("border-radius", "4px")
                                        }
                                    }) {
                                        Text(if (currentMusic?.published == true) "Yes" else "No")
                                    }
                                }
                            }
                        }
                    }
                }

                // Edit form
                if (isEditing) {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Column)
                            gap(0.5.r)
                            marginBottom(1.r)
                        }
                    }) {
                        // Name field
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                alignItems(AlignItems.Center)
                                gap(0.5.r)
                                marginBottom(0.5.r)
                            }
                        }) {
                            Text("Name:")
                            TextInput {
                                classes(Styles.textarea)
                                value(newName)
                                onInput { event ->
                                    newName = event.value
                                }
                            }
                        }

                        // Description field
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                alignItems(AlignItems.Center)
                                gap(0.5.r)
                                marginBottom(0.5.r)
                            }
                        }) {
                            Text("Description:")
                            TextInput {
                                classes(Styles.textarea)
                                value(newDescription)
                                onInput { event ->
                                    newDescription = event.value
                                }
                            }
                        }

                        // Size fields for objects
                        if (currentObject != null) {
                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    alignItems(AlignItems.Center)
                                    gap(0.5.r)
                                    marginBottom(0.5.r)
                                }
                            }) {
                                Text("Width:")
                                NumberInput(
                                    value = newWidth,
                                    min = 0.1,
                                    max = 100.0,
                                    attrs = {
                                        classes(Styles.textarea)
                                        style {
                                            width(100.percent)
                                        }
                                        onInput {
                                            newWidth = it.value?.toDouble() ?: 1.0
                                        }
                                    }
                                )
                            }

                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    alignItems(AlignItems.Center)
                                    gap(0.5.r)
                                    marginBottom(0.5.r)
                                }
                            }) {
                                Text("Height:")
                                NumberInput(
                                    value = newHeight,
                                    min = 0.1,
                                    max = 100.0,
                                    attrs = {
                                        classes(Styles.textarea)
                                        style {
                                            width(100.percent)
                                        }
                                        onInput {
                                            newHeight = it.value?.toDouble() ?: 1.0
                                        }
                                    }
                                )
                            }
                        }

                        // Save/Cancel buttons
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                gap(0.5.r)
                            }
                        }) {
                            Button({
                                classes(Styles.button)
                                onClick {
                                    scope.launch {
                                        when {
                                            currentTile != null -> {
                                                val updatedTile = currentTile!!.copy(
                                                    name = newName,
                                                    description = newDescription
                                                )
                                                api.updateGameTile(currentTile!!.id!!, updatedTile) {
                                                    // Update the current tile with the new values
                                                    map.setCurrentGameTile(it)
                                                    isEditing = false
                                                }
                                            }
                                            currentObject != null -> {
                                                val updatedObject = currentObject!!.copy(
                                                    name = newName,
                                                    description = newDescription,
                                                    width = newWidth.toString(),
                                                    height = newHeight.toString()
                                                )
                                                api.updateGameObject(currentObject!!.id!!, updatedObject) {
                                                    // Update the current object with the new values
                                                    map.setCurrentGameObject(it)
                                                    isEditing = false
                                                }
                                            }
                                            currentMusic != null -> {
                                                val updatedMusic = currentMusic!!.copy(
                                                    name = newName,
                                                    description = newDescription
                                                )
                                                api.updateGameMusic(currentMusic!!.id!!, updatedMusic) {
                                                    // Update the current music with the new values
                                                    isEditing = false
                                                }
                                            }
                                        }
                                    }
                                }
                            }) {
                                Text("Save")
                            }

                            Button({
                                classes(Styles.outlineButton)
                                onClick {
                                    isEditing = false
                                }
                            }) {
                                Text("Cancel")
                            }
                        }
                    }
                } else {
                    // Action buttons
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            gap(0.5.r)
                        }
                    }) {
                        // Edit button
                        Button({
                            classes(Styles.button)
                            onClick {
                                isEditing = true
                            }
                        }) {
                            Text("Edit")
                        }

                        // Check if the asset is already published
                        val isPublished = when {
                            currentTile != null -> currentTile?.published == true
                            currentObject != null -> currentObject?.published == true
                            currentMusic != null -> currentMusic?.published == true
                            else -> false
                        }

                        // Only show publish button if not already published
                        if (!isPublished) {
                            // Publish button
                            Button({
                                classes(Styles.button)
                                onClick {
                                    scope.launch {
                                        when {
                                            currentTile != null -> {
                                                val updatedTile = currentTile!!.copy(
                                                    published = true,
                                                    description = currentTile!!.description
                                                )
                                                api.updateGameTile(currentTile!!.id!!, updatedTile) {
                                                    // Update the current tile with the new values
                                                    map.setCurrentGameTile(it)
                                                }
                                            }
                                            currentObject != null -> {
                                                val updatedObject = currentObject!!.copy(
                                                    published = true,
                                                    description = currentObject!!.description
                                                )
                                                api.updateGameObject(currentObject!!.id!!, updatedObject) {
                                                    // Update the current object with the new values
                                                    map.setCurrentGameObject(it)
                                                }
                                            }
                                            currentMusic != null -> {
                                                val updatedMusic = currentMusic!!.copy(
                                                    published = true,
                                                    description = currentMusic!!.description
                                                )
                                                api.updateGameMusic(currentMusic!!.id!!, updatedMusic) {
                                                    // Update the current music with the new values
                                                    map.setCurrentGameMusic(it)
                                                }
                                            }
                                        }
                                    }
                                }
                            }) {
                                Text("Publish")
                            }
                        }

                        // Delete button (only if not published)
                        val canDelete = when {
                            currentTile != null -> currentTile?.published != true
                            currentObject != null -> currentObject?.published != true
                            currentMusic != null -> currentMusic?.published != true
                            else -> false
                        }

                        if (canDelete) {
                            Button({
                                classes(Styles.outlineButton)
                                onClick {
                                    scope.launch {
                                        val result = dialog(
                                            title = "Confirm Delete",
                                            confirmButton = "Delete",
                                            cancelButton = "Cancel"
                                        ) { _ ->
                                            Div({
                                                style {
                                                    padding(1.r)
                                                }
                                            }) {
                                                Text("Are you sure you want to delete this asset? This action cannot be undone.")
                                            }
                                        }

                                        if (result == true) {
                                            when {
                                                currentTile != null -> {
                                                    api.deleteGameTile(currentTile!!.id!!) {
                                                        // Clear the current tile
                                                        map.setCurrentGameTile(null)
                                                    }
                                                }
                                                currentObject != null -> {
                                                    api.deleteGameObject(currentObject!!.id!!) {
                                                        // Clear the current object
                                                        map.setCurrentGameObject(null)
                                                    }
                                                }
                                                currentMusic != null -> {
                                                    api.deleteGameMusic(currentMusic!!.id!!) {
                                                        // Clear the current music
                                                        map.setCurrentGameMusic(null)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

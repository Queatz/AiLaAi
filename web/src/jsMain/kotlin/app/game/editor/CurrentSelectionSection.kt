package app.game.editor

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.deleteGameMusic
import app.ailaai.api.deleteGameObject
import app.ailaai.api.deleteGameTile
import app.ailaai.api.gameObjects
import app.ailaai.api.profile
import app.ailaai.api.updateGameMusic
import app.ailaai.api.updateGameObject
import app.ailaai.api.updateGameTile
import app.dialog.dialog
import application
import baseUrl
import com.queatz.db.GameObjectOptions
import com.queatz.db.PersonProfile
import components.GroupPhoto
import components.GroupPhotoItem
import game.Map
import kotlinx.browser.document
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
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
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.NumberInput
import org.jetbrains.compose.web.dom.RangeInput
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.dom.TextInput
import org.w3c.dom.HTMLAnchorElement
import r

@Composable
fun CurrentSelectionSection(map: Map) {
    val me by application.me.collectAsState()
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
    var aspectRatio by remember { mutableStateOf(1.0) }

    // Track object options
    var scaleVariation by remember { mutableStateOf(0f) }
    var colorVariation by remember { mutableStateOf(0f) }

    // Track creator profile
    var creatorProfile by remember { mutableStateOf<PersonProfile?>(null) }

    val scope = rememberCoroutineScope()

    val isMine = me?.id == (
            currentTile?.person ?: currentObject?.person ?: currentMusic?.person
            )

    // Update when the selected assets change or their content changes
    LaunchedEffect(
        currentTile,
        currentObject,
        currentMusic
    ) {
        // Reset editing state when selection changes
        isEditing = false

        // Initialize name, description, and dimensions for editing
        currentTile?.let { tile ->
            newName = tile.name ?: "Unnamed Tile"
            newDescription = tile.description ?: ""

            // Fetch creator profile for tile
            tile.person?.let { personId ->
                scope.launch {
                    api.profile(
                        personId = personId,
                        onError = {
                            console.error("Failed to load creator profile", it)
                            creatorProfile = null
                        },
                        onSuccess = { profile ->
                            creatorProfile = profile
                        }
                    )
                }
            } ?: run {
                creatorProfile = null
            }
        }

        currentObject?.let { obj ->
            newName = obj.name ?: "Unnamed Object"
            newDescription = obj.description ?: ""
            newWidth = obj.width?.toDoubleOrNull() ?: 1.0
            newHeight = obj.height?.toDoubleOrNull() ?: 1.0
            // Calculate and store the aspect ratio (width/height)
            aspectRatio = if (newHeight > 0) newWidth / newHeight else 1.0

            // Parse options if available
            try {
                obj.options?.let { optionsJson ->
                    val options = Json.decodeFromString<GameObjectOptions>(optionsJson)
                    scaleVariation = options.scaleVariation
                    colorVariation = options.colorVariation
                } ?: run {
                    // Reset to defaults if no options
                    scaleVariation = 0f
                    colorVariation = 0f
                }
            } catch (e: Exception) {
                console.error("Failed to parse object options", e)
                // Reset to defaults on error
                scaleVariation = 0f
                colorVariation = 0f
            }

            // Fetch creator profile for object
            obj.person?.let { personId ->
                scope.launch {
                    api.profile(
                        personId = personId,
                        onError = {
                            console.error("Failed to load creator profile", it)
                            creatorProfile = null
                        },
                        onSuccess = { profile ->
                            creatorProfile = profile
                        }
                    )
                }
            } ?: run {
                creatorProfile = null
            }
        }

        currentMusic?.let { music ->
            newName = music.name ?: "Unnamed Music"
            newDescription = music.description ?: ""

            // Fetch creator profile for music
            music.person?.let { personId ->
                scope.launch {
                    api.profile(
                        personId = personId,
                        onError = {
                            console.error("Failed to load creator profile", it)
                            creatorProfile = null
                        },
                        onSuccess = { profile ->
                            creatorProfile = profile
                        }
                    )
                }
            } ?: run {
                creatorProfile = null
            }
        }

        // If no asset is selected, clear creator profile
        if (currentTile == null && currentObject == null && currentMusic == null) {
            creatorProfile = null
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
                // Asset preview and info (vertical layout)
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        alignItems(AlignItems.FlexStart)
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
                                        width(100.percent)
                                        height(12.r)

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

                                // Creator information
                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Text("Creator")
                                    // Creator profile link
                                    if (creatorProfile != null) {
                                        A("/profile/${creatorProfile!!.person.id}", {
                                            target(ATarget.Blank)
                                            style {
                                                padding(0.5.r)
                                                property("background-color", "rgba(0, 0, 0, 0.05)")
                                                property("border-radius", "4px")
                                                display(DisplayStyle.Flex)
                                                alignItems(AlignItems.Center)
                                                gap(0.5.r)
                                                property("text-decoration", "none")
                                                property("color", "inherit")
                                            }
                                        }) {
                                            GroupPhoto(
                                                items = listOf(
                                                    GroupPhotoItem(
                                                        photo = creatorProfile!!.person.photo,
                                                        name = creatorProfile!!.person.name
                                                    )
                                                ),
                                                size = 24.px
                                            )
                                            Text(creatorProfile!!.person.name ?: "Unknown")
                                        }
                                    } else {
                                        Div({
                                            style {
                                                padding(0.5.r)
                                                property("background-color", "rgba(0, 0, 0, 0.05)")
                                                property("border-radius", "4px")
                                                display(DisplayStyle.Flex)
                                                alignItems(AlignItems.Center)
                                                gap(0.5.r)
                                            }
                                        }) {
                                            Text("Unknown")
                                        }
                                    }
                                }
                            }
                        }

                        currentObject != null -> {
                            currentObject?.photo?.let { photo ->
                                Img(src = "$baseUrl$photo") {
                                    style {
                                        width(100.percent)
                                        height(12.r)
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

                                // Download button for music
                                currentMusic?.audio?.let { audioPath ->
                                    Div({
                                        style {
                                            display(DisplayStyle.Flex)
                                            flexDirection(FlexDirection.Column)
                                            gap(0.25.r)
                                            marginBottom(0.5.r)
                                        }
                                    }) {
                                        Button({
                                            classes(Styles.outlineButton)
                                            onClick {
                                                // Create an anchor element programmatically
                                                val a = document.createElement("a") as HTMLAnchorElement
                                                a.href = "$baseUrl$audioPath"
                                                a.download = currentMusic?.name ?: "Music"
                                                // Append to body, click, and remove
                                                document.body?.appendChild(a)
                                                a.click()
                                                document.body?.removeChild(a)
                                            }
                                        }) {
                                            Text("Download Music")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isMine) {
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
                                    flexDirection(FlexDirection.Column)
                                    gap(0.5.r)
                                    marginBottom(0.5.r)
                                }
                            }) {
                                Div({
                                    style { fontWeight("bold") }
                                }) { Text("Name") }
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
                                    flexDirection(FlexDirection.Column)
                                    gap(0.5.r)
                                    marginBottom(0.5.r)
                                }
                            }) {
                                Div({
                                    style { fontWeight("bold") }
                                }) { Text("Description") }
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
                                        flexDirection(FlexDirection.Column)
                                        gap(0.5.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Div({
                                        style { fontWeight("bold") }
                                    }) { Text("Width") }
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
                                        flexDirection(FlexDirection.Column)
                                        gap(0.5.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Div({
                                        style { fontWeight("bold") }
                                    }) { Text("Height") }
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
                                                val updatedHeight = it.value?.toDouble() ?: 1.0
                                                newHeight = updatedHeight
                                                // Update width to maintain aspect ratio
                                                newWidth = updatedHeight * aspectRatio
                                            }
                                        }
                                    )
                                }

                                // Scale variation slider
                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Div({
                                        style { fontWeight("bold") }
                                    }) { Text("Scale variation") }
                                    RangeInput(
                                        value = scaleVariation.toDouble(),
                                        min = 0.0,
                                        max = 1.0,
                                        step = 0.01
                                    ) {
                                        style {
                                            width(100.percent)
                                        }
                                        onInput {
                                            scaleVariation = it.value?.toFloat() ?: 0f
                                        }
                                    }
                                    Text("${(scaleVariation * 100).toInt()}%")
                                }

                                // Color variation slider
                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        flexDirection(FlexDirection.Column)
                                        gap(0.25.r)
                                        marginBottom(0.5.r)
                                    }
                                }) {
                                    Div({
                                        style { fontWeight("bold") }
                                    }) { Text("Color variation") }
                                    RangeInput(
                                        value = colorVariation.toDouble(),
                                        min = 0.0,
                                        max = 1.0,
                                        step = 0.01
                                    ) {
                                        style {
                                            width(100.percent)
                                        }
                                        onInput {
                                            colorVariation = it.value?.toFloat() ?: 0f
                                        }
                                    }
                                    Text("${(colorVariation * 100).toInt()}%")
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
                                                        // Update the AssetManager
                                                        assetManager.updateTile(it)
                                                        // Update UI inputs with the latest values
                                                        newName = it.name ?: "Unnamed Tile"
                                                        newDescription = it.description ?: ""
                                                        isEditing = false
                                                    }
                                                }

                                                currentObject != null -> {
                                                    // Create GameObjectOptions with current scale and color variations
                                                    val options = GameObjectOptions(
                                                        scaleVariation = scaleVariation,
                                                        colorVariation = colorVariation
                                                    )

                                                    // Encode options to JSON
                                                    val optionsJson = Json.encodeToString(options)

                                                    val updatedObject = currentObject!!.copy(
                                                        name = newName,
                                                        description = newDescription,
                                                        width = newWidth.toString(),
                                                        height = newHeight.toString(),
                                                        options = optionsJson
                                                    )
                                                    api.updateGameObject(currentObject!!.id!!, updatedObject) {
                                                        // Update the current object with the new values
                                                        map.setCurrentGameObject(it)
                                                        // Update the AssetManager
                                                        assetManager.updateObject(it)
                                                        // Force reload of objects in the AssetManager
                                                        scope.launch {
                                                            api.gameObjects(
                                                                onSuccess = { objectsList ->
                                                                    assetManager.setObjects(objectsList)
                                                                }
                                                            )
                                                        }
                                                        // Update UI inputs with the latest values
                                                        newName = it.name ?: "Unnamed Object"
                                                        newDescription = it.description ?: ""
                                                        newWidth = it.width?.toDoubleOrNull() ?: 1.0
                                                        newHeight = it.height?.toDoubleOrNull() ?: 1.0
                                                        // Recalculate aspect ratio
                                                        aspectRatio = if (newHeight > 0) newWidth / newHeight else 1.0
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
                                                        map.setCurrentGameMusic(it)
                                                        // Update the AssetManager
                                                        assetManager.updateMusic(it)
                                                        // Update UI inputs with the latest values
                                                        newName = it.name ?: "Unnamed Music"
                                                        newDescription = it.description ?: ""
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
                                                        // Update the AssetManager
                                                        assetManager.updateTile(it)
                                                        // Update UI inputs with the latest values
                                                        newName = it.name ?: "Unnamed Tile"
                                                        newDescription = it.description ?: ""
                                                    }
                                                }

                                                currentObject != null -> {
                                                    // Create GameObjectOptions with current scale and color variations
                                                    val options = GameObjectOptions(
                                                        scaleVariation = scaleVariation,
                                                        colorVariation = colorVariation
                                                    )

                                                    // Encode options to JSON
                                                    val optionsJson = Json.encodeToString(options)

                                                    val updatedObject = currentObject!!.copy(
                                                        published = true,
                                                        description = currentObject!!.description,
                                                        options = optionsJson
                                                    )
                                                    api.updateGameObject(currentObject!!.id!!, updatedObject) {
                                                        // Update the current object with the new values
                                                        map.setCurrentGameObject(it)
                                                        // Update the AssetManager
                                                        assetManager.updateObject(it)
                                                        // Force reload of objects in the AssetManager
                                                        scope.launch {
                                                            api.gameObjects(
                                                                onSuccess = { objectsList ->
                                                                    assetManager.setObjects(objectsList)
                                                                }
                                                            )
                                                        }
                                                        // Update UI inputs with the latest values
                                                        newName = it.name ?: "Unnamed Object"
                                                        newDescription = it.description ?: ""
                                                        newWidth = it.width?.toDoubleOrNull() ?: 1.0
                                                        newHeight = it.height?.toDoubleOrNull() ?: 1.0
                                                        // Recalculate aspect ratio
                                                        aspectRatio = if (newHeight > 0) newWidth / newHeight else 1.0
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
                                                        // Update the AssetManager
                                                        assetManager.updateMusic(it)
                                                        // Update UI inputs with the latest values
                                                        newName = it.name ?: "Unnamed Music"
                                                        newDescription = it.description ?: ""
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
                                                            // Remove from the AssetManager
                                                            assetManager.removeTile(currentTile!!.id!!)
                                                        }
                                                    }

                                                    currentObject != null -> {
                                                        api.deleteGameObject(currentObject!!.id!!) {
                                                            // Clear the current object
                                                            map.setCurrentGameObject(null)
                                                            // Remove from the AssetManager
                                                            assetManager.removeObject(currentObject!!.id!!)
                                                        }
                                                    }

                                                    currentMusic != null -> {
                                                        api.deleteGameMusic(currentMusic!!.id!!) {
                                                            // Clear the current music
                                                            map.setCurrentGameMusic(null)
                                                            // Remove from the AssetManager
                                                            assetManager.removeMusic(currentMusic!!.id!!)
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
}

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
import app.ailaai.api.createGameObject
import app.ailaai.api.gameObjects
import app.dialog.dialog
import app.dialog.rememberChoosePhotoDialog
import app.nav.NavSearchInput
import baseUrl
import com.queatz.db.GameObject
import components.IconButton
import components.Loading
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.NumberInput
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun ObjectsSection(
    map: game.Map?,
    onObjectSelected: (() -> Unit)? = null,
    clearSelection: Boolean = false
) {
    val scope = rememberCoroutineScope()
    var objects by remember { mutableStateOf<List<GameObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    // Track object creation state separately from photo generation
    var isObjectCreationInProgress by remember { mutableStateOf(false) }
    val choosePhotoDialog = rememberChoosePhotoDialog(showUpload = true, aspectRatio = 1.0, removeBackground = true)

    // Search functionality
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Get the photo generation state
    val isGenerating by choosePhotoDialog.isGenerating.collectAsState()

    // Combine both states to determine if we're creating an object
    var isCreatingObject by remember { mutableStateOf(false) }
    LaunchedEffect(isGenerating, isObjectCreationInProgress) {
        isCreatingObject = isGenerating || isObjectCreationInProgress
    }

    // Load objects when the component is first rendered
    LaunchedEffect(Unit) {
        isLoading = true
        api.gameObjects(
            onSuccess = { objectsList ->
                objects = objectsList
                isLoading = false
            }
        )
    }

    PanelSection(
        title = "Objects",
        icon = "category",
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
                        title = "Search objects"
                    ) {
                        showSearch = true
                    }
                }
            }

            // Button to create a new object
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
                            // Show dialog for width and height inputs
                            scope.launch {
                                var widthValue by mutableStateOf(1.0)
                                var heightValue by mutableStateOf(1.0)

                                val result = dialog(
                                    title = "Set Object Dimensions",
                                    confirmButton = "Create",
                                    cancelButton = "Cancel"
                                ) { resolve ->
                                    Div({
                                        style {
                                            padding(1.r)
                                            property("display", "flex")
                                            property("flex-direction", "column")
                                            property("align-items", "center")
                                            property("gap", "1rem")
                                        }
                                    }) {
                                        // Display the selected photo
                                        Img(src = "$baseUrl$photoUrl") {
                                            style {
                                                width(200.px)
                                                property("height", "200px")
                                                property("object-fit", "contain")
                                                marginBottom(1.r)
                                            }
                                        }

                                        // Width input
                                        Div({
                                            style {
                                                property("display", "flex")
                                                property("flex-direction", "column")
                                                property("align-items", "center")
                                                property("gap", "0.5rem")
                                                width(100.percent)
                                            }
                                        }) {
                                            Text("Width")
                                            NumberInput(
                                                value = widthValue,
                                                min = .1,
                                                max = 100,
                                                attrs = {
                                                    classes(Styles.textarea)
                                                    style {
                                                        width(100.percent)
                                                    }

                                                    onInput {
                                                        widthValue = it.value?.toDouble() ?: 1.0
                                                    }
                                                }
                                            )
                                        }

                                        // Height input
                                        Div({
                                            style {
                                                property("display", "flex")
                                                property("flex-direction", "column")
                                                property("align-items", "center")
                                                property("gap", "0.5rem")
                                                width(100.percent)
                                            }
                                        }) {
                                            Text("Height")
                                            NumberInput(
                                                value = heightValue,
                                                min = .1,
                                                max = 100,
                                                attrs = {
                                                    classes(Styles.textarea)
                                                    style {
                                                        width(100.percent)
                                                    }

                                                    onInput {
                                                        heightValue = it.value?.toDouble() ?: 1.0
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                if (result == true) {
                                    // Process the photo URL with width and height
                                    scope.launch {
                                        isObjectCreationInProgress = true
                                        // Create a new object with the photo URL and dimensions
                                        api.createGameObject(
                                            gameObject = GameObject(
                                                photo = photoUrl,
                                                width = widthValue.toString(),
                                                height = heightValue.toString()
                                            ),
                                            onSuccess = { newObject ->
                                                // Add the new object to the list
                                                objects = listOf(newObject) + objects
                                                isObjectCreationInProgress = false
                                            },
                                            onError = {
                                                isObjectCreationInProgress = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }) {
                Text("Create New Object")
            }

            // Show loading indicator when creating an object
            if (isCreatingObject) {
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
                        Text("Processing object...")
                    }
                }
            }

            // Display objects in a grid
            if (isLoading) {
                Div({
                    style {
                        padding(1.r)
                        property("text-align", "center")
                    }
                }) {
                    Text("Loading objects...")
                }
            } else if (objects.isEmpty()) {
                Div({
                    style {
                        padding(1.r)
                        property("text-align", "center")
                    }
                }) {
                    Text("No objects yet. Create your first object!")
                }
            } else {
                // Filter objects based on search query
                val filteredObjects = if (searchQuery.isNotEmpty()) {
                    objects.filter { obj ->
                        // Filter by name, ID, dimensions, or creation date
                        val name = obj.name ?: ""
                        val id = obj.id ?: ""
                        val dimensions = "${obj.width}x${obj.height}"
                        val createdAtStr = obj.createdAt?.toString() ?: ""

                        name.contains(searchQuery, ignoreCase = true) || 
                        id.contains(searchQuery, ignoreCase = true) ||
                        dimensions.contains(searchQuery, ignoreCase = true) ||
                        createdAtStr.contains(searchQuery, ignoreCase = true)
                    }
                } else {
                    objects
                }

                // Convert GameObject objects to Tool objects for the ToolGrid
                val objectTools = filteredObjects.map { obj ->
                    Tool(
                        id = obj.id ?: "",
                        name = obj.name ?: "Object",
                        photoUrl = "$baseUrl${obj.photo}",
                        description = "Size: ${obj.width}x${obj.height}"
                    )
                }

                var selectedObjectId by remember { mutableStateOf<String?>(null) }

                // Clear selection when clearSelection is true
                LaunchedEffect(clearSelection) {
                    if (clearSelection) {
                        selectedObjectId = null
                        map?.setCurrentGameObject(null)
                    }
                }

                Toolbox {
                    ToolGrid(
                        tools = objectTools,
                        selectedToolId = selectedObjectId,
                        onToolSelected = { tool ->
                            // Toggle selection
                            val newSelectedId = if (tool.id == selectedObjectId) null else tool.id
                            selectedObjectId = newSelectedId

                            // Find the selected object and pass it to the map
                            if (newSelectedId != null && map != null) {
                                val selectedObject = objects.find { it.id == newSelectedId }
                                if (selectedObject != null) {
                                    map.setCurrentGameObject(selectedObject)
                                    // Notify that an object was selected
                                    onObjectSelected?.invoke()
                                }
                            } else {
                                map?.setCurrentGameObject(null)
                            }
                        }
                    )
                }
            }
        }
    }
}

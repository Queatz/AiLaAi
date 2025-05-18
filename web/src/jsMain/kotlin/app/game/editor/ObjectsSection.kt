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
import baseUrl
import com.queatz.db.GameObject
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.NumberInput
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
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
    val choosePhotoDialog = rememberChoosePhotoDialog(showUpload = true, aspectRatio = 1.0, removeBackground = true, crop = true)

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

    // Convert GameObjects to GameObjectAssets
    val objectAssets = objects.map { GameObjectAsset(it) }

    // Track selected object ID
    var selectedObjectId by remember { mutableStateOf<String?>(null) }

    // Clear selection when clearSelection is true
    LaunchedEffect(clearSelection) {
        if (clearSelection) {
            selectedObjectId = null
            map?.setCurrentGameObject(null)
        }
    }

    // Use the generic AssetSection component
    AssetSection(
        title = "Objects",
        icon = "category",
        assets = objectAssets,
        isLoading = isLoading,
        isCreating = isCreatingObject,
        selectedAssetId = selectedObjectId,
        onAssetSelected = { objectAsset ->
            // Toggle selection
            val newSelectedId = if (objectAsset?.id == selectedObjectId) null else objectAsset?.id
            selectedObjectId = newSelectedId

            // Find the selected object and pass it to the map
            if (newSelectedId != null && map != null) {
                val selectedObject = objects.find { it.id == newSelectedId }
                if (selectedObject != null) {
                    map.setCurrentGameObject(selectedObject)
                    // Notify that an object was selected
                    onObjectSelected?.invoke()
                }
            } else if (map != null) {
                // If deselected, set to null
                map.setCurrentGameObject(null)
            }
        },
        onCreateAsset = {
            // Use ChoosePhotoDialog to select, process, and upload a photo
            scope.launch {
                choosePhotoDialog.launch { photoUrl, width, height ->
                    // Show dialog for width and height inputs
                    scope.launch {
                        // Calculate aspect ratio if width and height are available
                        val aspect = if (width != null && height != null && width > 0) {
                            height.toDouble() / width.toDouble()
                        } else {
                            1.0 // Default aspect ratio if dimensions are not available
                        }

                        var widthValue by mutableStateOf(1.0)
                        // Set heightValue based on width * aspect
                        var heightValue by mutableStateOf(widthValue * aspect)

                        val result = dialog(
                            title = "Set Object Dimensions",
                            confirmButton = "Create",
                            cancelButton = "Cancel",
                            cancellable = false
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
                                                // Update height based on width and aspect ratio
                                                heightValue = widthValue * aspect
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
        },
        assetToTool = { objectAsset ->
            Tool(
                id = objectAsset.id,
                name = objectAsset.name,
                photoUrl = "$baseUrl${objectAsset.content}",
                description = objectAsset.description
            )
        },
        searchFilter = { objectAsset, query ->
            objectAsset.name.contains(query, ignoreCase = true) ||
            objectAsset.id.contains(query, ignoreCase = true) ||
            objectAsset.description.contains(query, ignoreCase = true) ||
            objectAsset.createdAt?.toString()?.contains(query, ignoreCase = true) ?: false
        },
        createButtonText = "Create New Object",
        emptyText = "No objects yet. Create your first object!",
        loadingText = "Loading objects...",
        processingText = "Processing object..."
    )
}

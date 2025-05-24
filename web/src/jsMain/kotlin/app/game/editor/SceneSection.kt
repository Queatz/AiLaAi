package app.game.editor

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.deleteGameScene
import app.ailaai.api.updateGameScene
import app.ailaai.api.uploadScreenshot
import app.dialog.dialog
import app.dialog.inputDialog
import app.group.friendsDialog
import application
import baseUrl
import com.queatz.db.GameScene
import com.queatz.db.Person
import com.queatz.db.UploadResponse
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.w3c.files.Blob
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLCanvasElement
import r
import toBytes
import components.IconButton

@Composable
fun SceneSection(
    gameScene: GameScene? = null,
    onSceneDeleted: () -> Unit = {},  // Add this parameter with a default empty function
    onSceneUpdated: (GameScene) -> Unit = {}  // Add callback for scene updates
) {
    val scope = rememberCoroutineScope()

    // Get the current user
    val me by application.me.collectAsState()

    // Check if the current user is the owner of the scene
    val isCurrentUserOwner = gameScene?.person == me?.id

    // Local state version of gameScene that we update when we rename or change the URL
    var localGameScene by remember(gameScene) { mutableStateOf(gameScene) }
    var scenePhoto by remember(gameScene) { mutableStateOf(gameScene?.photo) }

    PanelSection(
        title = "Scene",
        icon = "scene",
        initiallyExpanded = false,
        closeOtherPanels = true
    ) {
        // Main content wrapper (spacing via gap, outer padding handled by PanelSection)
        Div({
            style {
                // padding removed to prevent excessive nesting padding
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(1.r)
            }
        }) {
            // Scene Info Section with elevated style
            Div({
                style {
                    padding(1.r)
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(1.r)
                }
            }) {
                // Scene Name
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
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.SpaceBetween)
                        }
                    }) {
                        Div({
                            style {
                                fontWeight("bold")
                            }
                        }) {
                            Text("Scene Name")
                        }

                        IconButton(
                            name = "edit",
                            title = "Rename Scene",
                            onClick = {
                                scope.launch {
                                    val newName = inputDialog(
                                        title = "Rename Scene",
                                        placeholder = "Enter new scene name",
                                        defaultValue = localGameScene?.name ?: ""
                                    )

                                    if (newName != null) {
                                        localGameScene?.id?.let { sceneId ->
                                            val updatedGameScene = GameScene(name = newName)
                                            api.updateGameScene(sceneId, updatedGameScene) {
                                                // Update local state with the new name
                                                localGameScene = localGameScene?.copy(name = it.name)
                                                // Notify parent components about the update
                                                onSceneUpdated(it)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                        }
                    }) {
                        Text(localGameScene?.name ?: "New Scene")
                    }
                }

                // Scene URL
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        gap(0.5.r)
                        marginTop(1.r)
                    }
                }) {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.SpaceBetween)
                        }
                    }) {
                        Div({
                            style {
                                fontWeight("bold")
                            }
                        }) {
                            Text("Scene URL")
                        }

                        IconButton(
                            name = "edit",
                            title = "Edit Scene URL",
                            onClick = {
                                scope.launch {
                                    val newUrl = inputDialog(
                                        title = "Edit Scene URL",
                                        placeholder = "Enter scene URL",
                                        defaultValue = localGameScene?.url ?: ""
                                    )

                                    if (newUrl != null) {
                                        localGameScene?.id?.let { sceneId ->
                                            val updatedGameScene = GameScene(url = newUrl)
                                            api.updateGameScene(sceneId, updatedGameScene, onError = {
                                                dialog(
                                                    title = "Error",
                                                    confirmButton = "OK"
                                                ) {
                                                    Text("This URL is already taken. Please choose a different URL.")
                                                }
                                            }) {
                                                // Scene URL updated successfully
                                                localGameScene = it
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                        }
                    }) {
                        Text(localGameScene?.url ?: localGameScene?.id ?: "Not saved yet")
                    }
                }
            }

            // Scene Description Section
            Div({
                style {
                    padding(1.r)
                    borderRadius(1.r)
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(1.r)
                }
            }) {
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
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.SpaceBetween)
                        }
                    }) {
                        Div({
                            style {
                                fontWeight("bold")
                            }
                        }) {
                            Text("Description")
                        }

                        IconButton(
                            name = "edit",
                            title = "Edit Description",
                            onClick = {
                                scope.launch {
                                    val newDescription = inputDialog(
                                        title = "Edit Scene Description",
                                        placeholder = "Enter scene description",
                                        defaultValue = localGameScene?.description ?: "",
                                        singleLine = false
                                    )

                                    if (newDescription != null) {
                                        localGameScene?.id?.let { sceneId ->
                                            val updatedGameScene = GameScene(description = newDescription)
                                            api.updateGameScene(sceneId, updatedGameScene) { updated ->
                                                // Update local state with the new description
                                                localGameScene = updated
                                                // Notify parent about the scene update
                                                onSceneUpdated(updated)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    // Display the current description or a placeholder
                    Div({
                        style {
                            borderRadius(0.5.r)
                            minHeight(2.r)
                        }
                    }) {
                        Text(localGameScene?.description ?: "No description")
                    }
                }
            }

            // Scene Photo Section
            Div({
                style {
                    property("box-shadow", "1px 1px 4px rgba(0, 0, 0, 0.125)")
                    padding(1.r)
                    borderRadius(1.r)
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(1.r)
                }
            }) {
                Div({
                    style {
                        fontWeight("bold")
                        marginBottom(0.5.r)
                    }
                }) {
                    Text("Scene Photo")
                }

                // Display the scene photo if it exists
                if (scenePhoto != null) {
                    Div({
                        style {
                            borderRadius(0.5.r)
                            property("overflow", "hidden")
                            width(100.percent)
                            property("aspect-ratio", "2 / 1")
                        }
                    }) {
                        Img(src = "$baseUrl${scenePhoto!!}", attrs = {
                            style {
                                width(100.percent)
                                height(100.percent)
                                property("object-fit", "cover")
                            }
                        })
                    }
                } else {
                    Div({
                        style {
                            borderRadius(0.5.r)
                            width(100.percent)
                            property("aspect-ratio", "2 / 1")
                            display(DisplayStyle.Flex)
                            justifyContent(JustifyContent.Center)
                            alignItems(AlignItems.Center)
                            opacity(.5)
                        }
                    }) {
                        Text("No photo set")
                    }
                }

                // Button to take a screenshot and set as scene photo
                Button({
                    classes(Styles.button)
                    style {
                        width(100.percent)
                    }
                    onClick {
                        scope.launch {
                            localGameScene?.id?.let { sceneId ->
                                // Find the canvas element
                                val canvas = document.querySelector("canvas") as? HTMLCanvasElement

                                if (canvas != null) {
                                    // Convert canvas to blob using CompletableDeferred
                                    val blobResult = CompletableDeferred<Blob>()

                                    canvas.toBlob({
                                        if (it == null) {
                                            blobResult.completeExceptionally(Throwable("Failed to convert canvas to blob"))
                                        } else {
                                            blobResult.complete(it)
                                        }
                                    }, "image/png", 0.9)

                                    val blob = blobResult.await()

                                    // Convert blob to ByteArray
                                    val screenshotBytes = blob.toBytes()

                                    // Upload the screenshot
                                    api.uploadScreenshot(
                                        screenshot = screenshotBytes,
                                        onSuccess = { response: UploadResponse ->
                                            if (response.urls.isNotEmpty()) {
                                                // Update the scene with the photo URL
                                                val photoUrl = response.urls.first()
                                                val updatedGameScene = GameScene(photo = photoUrl)
                                                api.updateGameScene(sceneId, updatedGameScene) {
                                                    scenePhoto = it.photo
                                                    localGameScene = localGameScene?.copy(photo = it.photo)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }) {
                    Text(if (scenePhoto != null) "Update Scene Photo" else "Set Scene Photo")
                }
            }

            // Scene Actions Section
            Div({
                style {
                    property("box-shadow", "1px 1px 4px rgba(0, 0, 0, 0.125)")
                    padding(1.r)
                    borderRadius(1.r)
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(1.r)
                }
            }) {
                Div({
                    style {
                        fontWeight("bold")
                        marginBottom(0.5.r)
                    }
                }) {
                    Text("Scene Actions")
                }

                // Change Owner Button - Only show if the current user is the owner
                if (isCurrentUserOwner && localGameScene?.id != null) {
                    Button({
                        classes(Styles.outlineButton)
                        style {
                            width(100.percent)
                            marginBottom(0.5.r)
                        }
                        onClick {
                            scope.launch {
                                friendsDialog(
                                    title = "Change Scene Owner",
                                    multiple = false,
                                    confirmButton = "Cancel"
                                ) { selectedPeople ->
                                    if (selectedPeople.isNotEmpty()) {
                                        val newOwner = selectedPeople.first()

                                        // Confirm the owner change
                                        scope.launch {
                                            val confirmed = dialog(
                                                title = "Change Owner",
                                                confirmButton = "Change",
                                                cancelButton = "Cancel"
                                            ) {
                                                Text("Are you sure you want to transfer ownership of this scene to ${newOwner.name ?: "this user"}?")
                                            }

                                            if (confirmed == true) {
                                                // Update the scene with the new owner
                                                val updatedGameScene = GameScene(person = newOwner.id)
                                                localGameScene?.id?.let { sceneId ->
                                                    api.updateGameScene(sceneId, updatedGameScene) {
                                                        // Scene owner updated successfully
                                                        localGameScene = localGameScene?.copy(person = newOwner.id)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }) {
                        Text("Change Owner")
                    }
                }

                // Delete Scene Button
                Button({
                    classes(Styles.outlineButton)
                    style {
                        width(100.percent)
                        color(Styles.colors.red)
                        border(1.px, LineStyle.Solid, Styles.colors.red)
                    }
                    onClick {
                        scope.launch {
                            localGameScene?.id?.let { sceneId ->
                                val confirmed = dialog(
                                    title = "Delete Scene",
                                    confirmButton = "Delete",
                                    cancelButton = "Cancel"
                                ) {
                                    Text("Are you sure you want to delete this scene? This action cannot be undone.")
                                }

                                if (confirmed == true) {
                                    api.deleteGameScene(sceneId) {
                                        // Scene deleted successfully
                                        onSceneDeleted()  // Call the callback after successful deletion
                                    }
                                }
                            }
                        }
                    }
                }) {
                    Text("Delete Scene")
                }
            }
        }
    }
}

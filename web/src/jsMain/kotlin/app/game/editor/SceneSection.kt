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

@Composable
fun SceneSection(
    gameScene: GameScene? = null,
    onSceneDeleted: () -> Unit = {}  // Add this parameter with a default empty function
) {
    val scope = rememberCoroutineScope()

    // Get the current user
    val me by application.me.collectAsState()

    // Check if the current user is the owner of the scene
    val isCurrentUserOwner = gameScene?.person == me?.id

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

                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            gap(0.5.r)
                        }
                    }) {
                        Text(gameScene?.name ?: "New Scene")

                        Button({
                            classes(Styles.outlineButton)
                            onClick {
                                scope.launch {
                                    val newName = inputDialog(
                                        title = "Rename Scene",
                                        placeholder = "Enter new scene name",
                                        defaultValue = gameScene?.name ?: ""
                                    )

                                    if (newName != null && gameScene?.id != null) {
                                        val updatedGameScene = GameScene(name = newName)
                                        api.updateGameScene(gameScene.id!!, updatedGameScene) {
                                            // Scene renamed successfully
                                        }
                                    }
                                }
                            }
                        }) {
                            Text("Rename")
                        }
                    }
                }

                // Scene URL
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

                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            gap(0.5.r)
                        }
                    }) {
                        Text(gameScene?.url ?: gameScene?.id ?: "Not saved yet")

                        Button({
                            classes(Styles.outlineButton)
                            onClick {
                                scope.launch {
                                    val newUrl = inputDialog(
                                        title = "Edit Scene URL",
                                        placeholder = "Enter scene URL",
                                        defaultValue = gameScene?.url ?: ""
                                    )

                                    if (newUrl != null && gameScene?.id != null) {
                                        val updatedGameScene = GameScene(url = newUrl)
                                        api.updateGameScene(gameScene.id!!, updatedGameScene) {
                                            // Scene URL updated successfully
                                        }
                                    }
                                }
                            }
                        }) {
                            Text("Edit URL")
                        }
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
                            if (gameScene?.id != null) {
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
                                                api.updateGameScene(gameScene.id!!, updatedGameScene) {
                                                    scenePhoto = it.photo
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
                if (isCurrentUserOwner && gameScene?.id != null) {
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
                                                api.updateGameScene(gameScene.id!!, updatedGameScene) {
                                                    // Scene owner updated successfully
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
                            if (gameScene?.id != null) {
                                val confirmed = dialog(
                                    title = "Delete Scene",
                                    confirmButton = "Delete",
                                    cancelButton = "Cancel"
                                ) {
                                    Text("Are you sure you want to delete this scene? This action cannot be undone.")
                                }

                                if (confirmed == true) {
                                    api.deleteGameScene(gameScene.id!!) {
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

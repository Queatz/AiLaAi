package app.game.editor

import Styles
import androidx.compose.runtime.Composable
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
import baseUrl
import com.queatz.db.GameScene
import com.queatz.db.UploadResponse
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.w3c.files.Blob
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
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

    var scenePhoto by remember (gameScene) { mutableStateOf(gameScene?.photo) }

    PanelSection(
        title = "Scene",
        icon = "scene",
        initiallyExpanded = false,
        closeOtherPanels = true
    ) {
        Div({
            style {
                padding(.5.r)
            }
        }) {
            Div({
                style {
                    padding(.5.r, 0.r)
                }
            }) {
                Text("Scene name: ${gameScene?.name ?: "New Scene"}")

                Button({
                    classes(Styles.outlineButton)
                    style {
                        marginLeft(.5.r)
                    }
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

            Div({
                style {
                    padding(.5.r, 0.r)
                }
            }) {
                Text("Scene URL: ${gameScene?.url ?: gameScene?.id ?: "Not saved yet"}")

                Button({
                    classes(Styles.outlineButton)
                    style {
                        marginLeft(.5.r)
                    }
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

            // Scene Photo Section
            Div({
                style {
                    padding(.5.r, 0.r)
                }
            }) {
                // Display the scene photo if it exists
                if (scenePhoto != null) {
                    Div({
                        style {
                            marginBottom(.5.r)
                        }
                    }) {
                        Text("Scene Photo:")
                    }

                    Img(src = "$baseUrl${scenePhoto!!}", attrs = {
                        style {
                            maxWidth(100.percent)
                            height(12.r)
                            property("object-fit", "cover")
                            marginBottom(.5.r)
                        }
                    })
                }

                // Button to take a screenshot and set as scene photo
                Button({
                    classes(Styles.outlineButton)
                    style {
                        marginLeft(if (scenePhoto != null) 0.r else .5.r)
                        marginBottom(.5.r)
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

            // Delete Scene Button
            Div({
                style {
                    padding(.5.r, 0.r)
                }
            }) {
                Button({
                    classes(Styles.outlineButton)
                    style {
                        marginLeft(.5.r)
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

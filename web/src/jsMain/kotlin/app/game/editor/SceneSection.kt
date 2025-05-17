package app.game.editor

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import api
import app.ailaai.api.deleteGameScene
import app.ailaai.api.updateGameScene
import app.dialog.dialog
import app.dialog.inputDialog
import com.queatz.db.GameScene
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun SceneSection(
    gameScene: GameScene? = null,
    onSceneDeleted: () -> Unit = {}  // Add this parameter with a default empty function
) {
    val scope = rememberCoroutineScope()

    PanelSection(
        title = "Scene",
        icon = "scene",
        initiallyExpanded = false
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

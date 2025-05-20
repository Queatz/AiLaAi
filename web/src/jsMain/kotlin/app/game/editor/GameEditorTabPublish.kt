package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.updateGameScene
import com.queatz.db.GameScene
import game.Map
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lib.Engine
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun GameEditorTabPublish(engine: Engine, map: Map, gameScene: GameScene? = null, onUploaded: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var published by remember { mutableStateOf(gameScene?.published == true) }

    Div({
        style {
            padding(1.r)
        }
    }) {
        PanelSection(
            title = "Publish",
            icon = "publish",
            initiallyExpanded = true
        ) {
            if (gameScene != null) {
                if (!published) {
                    Div({
                        style {
                            padding(1.r, 0.r)
                        }
                    }) {
                        Text("Your scene is not published yet. Publish it to make it available to others.")

                        Div({
                            style {
                                padding(1.r, 0.r)
                            }
                        }) {
                            Button({
                                classes(Styles.button)

                                onClick {
                                    scope.launch {
                                        val updatedGameScene = gameScene.copy(published = true)
                                        gameScene.id?.let { id ->
                                            api.updateGameScene(id, updatedGameScene) {
                                                published = true
                                            }
                                        }
                                    }
                                }
                            }) {
                                Text("Publish")
                            }
                        }
                    }
                } else {
                    Div({
                        style {
                            padding(1.r, 0.r)
                        }
                    }) {
                        Text("Your scene is published and available to others.")

                        Div({
                            style {
                                padding(1.r, 0.r)
                            }
                        }) {
                            var isCopied by remember { mutableStateOf(false) }

                            Button({
                                classes(Styles.outlineButton, Styles.outlineButtonTonal)

                                onClick {
                                    // Copy game URL to clipboard
                                    gameScene.id?.let { id ->
                                        val gameUrl = window.location.origin + "/scene/" + id
                                        window.navigator.clipboard.writeText(gameUrl).then {
                                            isCopied = true
                                            scope.launch {
                                                delay(2000) // Wait for 2 seconds
                                                isCopied = false
                                            }
                                        }
                                    }
                                }
                            }) {
                                Text(if (isCopied) "Link copied!" else "Copy link")
                            }

                            Div({
                                style {
                                    padding(0.5.r, 0.r)
                                }
                            })

                            Button({
                                classes(Styles.button)

                                onClick {
                                    // Open game in new tab
                                    gameScene.id?.let { id ->
                                        val gameUrl = window.location.origin + "/scene/" + id
                                        window.open(gameUrl, "_blank")
                                    }
                                }
                            }) {
                                Text("Launch scene")
                            }
                        }
                    }
                }
            } else {
                Div({
                    style {
                        padding(1.r, 0.r)
                    }
                }) {
                    Text("Save your scene first to be able to publish it.")
                }
            }
        }
    }
}

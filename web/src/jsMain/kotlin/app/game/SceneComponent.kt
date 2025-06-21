package app.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.GamePage
import app.ailaai.api.gameScene
import com.queatz.db.GameScene
import components.Loading
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

/**
 * A component that loads a GameScene by ID and renders it using GamePage
 */
@Composable
fun SceneComponent(
    sceneId: String,
    showSidePanel: Boolean = false,
    showScreenshot: Boolean = false,
    editable: Boolean = false,
    styles: StyleScope.() -> Unit = {}
) {
    var gameScene by remember { mutableStateOf<GameScene?>(null) }
    var loadError by remember { mutableStateOf(false) }

    // Load the GameScene outside of GamePage
    LaunchedEffect(sceneId) {
        api.gameScene(
            id = sceneId,
            onSuccess = { scene ->
                gameScene = scene
                loadError = false
            },
            onError = {
                loadError = true
            }
        )
    }

    if (loadError) {
        // Show error message when scene fails to load
        Div(
            {
                style {
                    width(100.percent)
                    property("aspect-ratio", "16/9")
                    styles()
                }
            }
        ) { Text("Scene not found.") }
    } else if (gameScene == null) {
        // Show loading indicator while scene is loading
        Loading()
    } else {
        // Render the GamePage with the loaded GameScene
        GamePage(
            gameScene = gameScene,
            showSidePanel = showSidePanel,
            showScreenshot = showScreenshot,
            editable = editable,
            styles = {
                width(100.percent)
                property("aspect-ratio", "16/9")
                styles()
            }
        )
    }
}

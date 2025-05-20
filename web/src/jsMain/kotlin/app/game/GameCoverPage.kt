package app.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.FullPageLayout
import app.GamePage
import app.ailaai.api.gameScene
import com.queatz.db.GameScene
import components.Loading
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.alignSelf
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import web.cssom.atrule.displayMode
import web.cssom.atrule.width

@Composable
fun GameCoverPage(id: String) {
    var gameScene by remember { mutableStateOf<GameScene?>(null) }
    var loadError by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        api.gameScene(
            id = id,
            onSuccess = { scene ->
                gameScene = scene
                loadError = false
            },
            onError = {
                loadError = true
            }
        )
    }
    FullPageLayout(useVh = true, maxWidth = null) {
        if (loadError) {
            // Show error message when scene fails to load
            Div(
                {
                    style {
                        width(100.percent)
                        height(100.percent)
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        justifyContent(JustifyContent.Center)
                    }
                }
            ) { Text("Scene not found.") }
        } else if (gameScene == null) {
            Loading()
        } else {
            GamePage(
                gameScene = gameScene,
                showSidePanel = false,
                showScreenshot = false,
                editable = false,
                styles = {
                    height(100.percent)
                }
            )
        }
    }
}

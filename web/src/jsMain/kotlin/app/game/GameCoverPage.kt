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
import app.ailaai.api.gameSceneByUrl
import com.queatz.db.GameScene
import components.Loading
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.percent

@Composable
fun GameCoverPage(id: String) {
    var gameScene by remember { mutableStateOf<GameScene?>(null) }

    LaunchedEffect(id) {
        api.gameSceneByUrl(id, onSuccess = { scene ->
            gameScene = scene
        })
    }
    FullPageLayout(useVh = true, maxWidth = null) {
        if (gameScene == null) {
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

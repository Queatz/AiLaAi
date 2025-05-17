package app.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.game.editor.GameEditorInstructions
import app.game.editor.GameEditorTabEditor
import app.game.editor.GameEditorTabLibrary
import app.game.editor.GameEditorTabPublish
import app.game.editor.GameEditorTabDiscussion
import app.game.editor.TabBar
import com.queatz.db.GameScene
import game.Map
import lib.Engine
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun GameEditorPanel(
    engine: Engine,
    map: Map,
    gameScene: GameScene? = null,
    onPixelatedChanged: (Boolean) -> Unit = {},
    onSceneDeleted: () -> Unit = {},
    styles: StyleScope.() -> Unit = {}
) {
    Div(
        attrs = {
            style {
                width(24.r)
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                overflowX("hidden")
                overflowY("auto")
                height(100.percent)
                styles()
            }
        }
    ) {
        var selectedTab by remember { mutableStateOf(0) }

        // Use the reusable TabBar component
        TabBar(
            tabs = listOf("Editor", "Discussion", "Help", "Publish"),
            initialSelectedIndex = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        // Tab content
        when (selectedTab) {
            0 -> GameEditorTabEditor(engine, map, gameScene, onSceneDeleted, onPixelatedChanged)
//            1 -> GameEditorTabLibrary(engine, map, gameScene)
            1 -> GameEditorTabDiscussion(engine, map, gameScene)
            2 -> GameEditorInstructions()
            3 -> GameEditorTabPublish(engine, map, gameScene)
        }
    }
}

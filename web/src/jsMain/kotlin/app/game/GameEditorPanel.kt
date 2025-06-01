package app.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import app.game.editor.TabInfo
import application
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
    editable: Boolean,
    gameScene: GameScene? = null,
    onPixelatedChanged: (Boolean) -> Unit = {},
    onSceneDeleted: () -> Unit = {},
    onScenePublished: () -> Unit = {},
    onSceneForked: (GameScene) -> Unit = {},
    onSceneUpdated: (GameScene) -> Unit = {},
    onSceneSaved: (GameScene) -> Unit = {},
    styles: StyleScope.() -> Unit = {}
) {
    // Get the current user
    val me by application.me.collectAsState()
    var localGameScene by remember(gameScene) {
        mutableStateOf(gameScene)
    }

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

        // Create tabs list based on scene ownership
        val isCurrentUserOwner = gameScene?.person == me?.id

        val tabs = remember(isCurrentUserOwner) {
            buildList {
                if (isCurrentUserOwner && editable) {
                    add(TabInfo("Editor", "edit") {
                        GameEditorTabEditor(
                            engine = engine,
                            map = map,
                            gameScene = gameScene,
                            editable = editable,
                            onSceneDeleted = onSceneDeleted,
                            onPixelatedChanged = onPixelatedChanged,
                            onSceneForked = onSceneForked,
                            onSceneUpdated = onSceneUpdated,
                            onSceneSaved = onSceneSaved
                        )
                    })
                }

                add(TabInfo("Discussion", "forum") { GameEditorTabDiscussion(engine, map, gameScene) })

                if (isCurrentUserOwner && editable) {
                    add(TabInfo("Library", "collections_bookmark") { GameEditorTabLibrary(engine, map, gameScene) })
                    add(TabInfo("Learn", "menu_book") { GameEditorInstructions() })
                    add(TabInfo("Publish", "publish") {
                    GameEditorTabPublish(
                        gameScene = gameScene,
                        onUpdated = {
                            localGameScene = it
                            onScenePublished()
                        }
                    )
                    })
                }
            }
        }

        TabBar(
            tabs = tabs,
            initialSelectedIndex = selectedTab.coerceIn(0, tabs.lastIndex),
            onTabSelected = { selectedTab = it }
        )

        // Tab content
        tabs[selectedTab].content()
    }
}

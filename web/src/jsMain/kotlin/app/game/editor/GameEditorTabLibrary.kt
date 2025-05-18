package app.game.editor

import androidx.compose.runtime.Composable
import com.queatz.db.GameScene
import game.Map
import lib.Engine
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import r

/**
 * Library tab for the game editor.
 * Contains sections for Tiles, Objects, and Music.
 */
@Composable
fun GameEditorTabLibrary(engine: Engine, map: Map, gameScene: GameScene? = null) {
    Div({
        style {
            padding(1.r)
        }
    }) {
        // Add the current asset section at the top
        CurrentSelectionSection(map)

        // Include the three required panels with placeholder content
        TilesSection(
            map = map,
            onTileSelected = null,
            clearSelection = false
        )

        ObjectsSection(
            map = map,
            onObjectSelected = null,
            clearSelection = false
        )

        MusicSection(map.game, map)
    }
}

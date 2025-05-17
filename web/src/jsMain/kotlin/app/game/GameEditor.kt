package app.game

import androidx.compose.runtime.Composable
import game.Map
import lib.Engine

@Composable
fun GameEditor(engine: Engine, map: Map) {
    GameEditorPanel(engine, map)
}

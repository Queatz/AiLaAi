package app.game.editor

import androidx.compose.runtime.Composable

@Composable
fun CurrentSelectionSection() {
    PanelSection(
        title = "Current selection",
        icon = "select_all",
        enabled = false,
        initiallyExpanded = false
    ) {
        ToolGridExample()
    }
}

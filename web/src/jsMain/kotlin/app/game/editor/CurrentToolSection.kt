package app.game.editor

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Text

@Composable
fun CurrentToolSection() {
    PanelSection(
        title = "Current tool",
        icon = "build",
        enabled = false,
        initiallyExpanded = true
    ) {
        Text("Placeholder content for Current tool")
    }
}

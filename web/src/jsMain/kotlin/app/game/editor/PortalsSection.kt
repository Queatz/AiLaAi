package app.game.editor

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Text

@Composable
fun PortalsSection() {
    PanelSection(
        title = "Portals",
        icon = "door_front",
        enabled = false,
        initiallyExpanded = false
    ) {
        Text("Placeholder content for Portals")
    }
}

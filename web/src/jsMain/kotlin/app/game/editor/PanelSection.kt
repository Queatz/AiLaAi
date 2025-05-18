package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.AppStyles
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.events.MouseEvent

/**
 * A reusable panel section component with an expandable header and content.
 * 
 * @param title The title of the section
 * @param icon The material icon name to display (from material-symbols-outlined)
 * @param enabled Whether the section is enabled
 * @param initiallyExpanded Whether the section is initially expanded
 * @param onCtrlClick Callback when Ctrl+click is detected on the header
 * @param closeOtherPanels Whether this panel should close when another panel is Ctrl+clicked
 * @param content The content to display when the section is expanded
 */
@Composable
fun PanelSection(
    title: String,
    icon: String,
    enabled: Boolean = true,
    initiallyExpanded: Boolean = true,
    onCtrlClick: (() -> Unit)? = null,
    closeOtherPanels: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    // Listen for the global event to close other panels
    DisposableEffect(closeOtherPanels) {
        val callback = {
            if (closeOtherPanels && expanded) {
                expanded = false
            }
        }

        // Register this panel to listen for close events
        PanelSectionRegistry.addCloseListener(callback)

        // Clean up when the component is disposed
        onDispose {
            PanelSectionRegistry.removeCloseListener(callback)
        }
    }

    Div({
        classes(AppStyles.editorPanelSection)

        if (!enabled) {
            style {
                opacity(0.5)
            }
        }
    }) {
        // Header with icon and title
        Div({
            classes(AppStyles.editorPanelHeader)
            onClick { event ->
                // Check if Ctrl key is pressed
                if ((event.nativeEvent as MouseEvent).ctrlKey) {
                    // If Ctrl is pressed, notify other panels to close
                    PanelSectionRegistry.notifyCloseOtherPanels()
                    // Call the onCtrlClick callback if provided
                    onCtrlClick?.invoke()
                    // Also expand this panel if it's not already expanded
                    if (!expanded) {
                        expanded = true
                    }
                } else {
                    // Normal click behavior - toggle expanded state
                    expanded = !expanded
                }
            }
        }) {
            // Material icon
            Span({
                classes("material-symbols-outlined", AppStyles.editorPanelHeaderIcon)
            }) {
                Text(if (expanded) "expand_more" else "chevron_right")
            }

            // Icon for the section
            Span({
                classes("material-symbols-outlined", AppStyles.editorPanelHeaderIcon)
            }) {
                Text(icon)
            }

            // Section title
            Text(title)
        }

        // Expandable content
        if (expanded) {
            Div({
                classes(AppStyles.editorPanelContent)
            }) {
                content()
            }
        }
    }
}

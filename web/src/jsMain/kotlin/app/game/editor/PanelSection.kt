package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.AppStyles
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * A reusable panel section component with an expandable header and content.
 * 
 * @param title The title of the section
 * @param icon The material icon name to display (from material-symbols-outlined)
 * @param initiallyExpanded Whether the section is initially expanded
 * @param content The content to display when the section is expanded
 */
@Composable
fun PanelSection(
    title: String,
    icon: String,
    enabled: Boolean = true,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    
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
            onClick {
                expanded = !expanded
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

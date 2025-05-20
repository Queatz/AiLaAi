package app.game.editor

import Styles
import androidx.compose.runtime.Composable
import app.compose.rememberDarkMode
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import r

/**
 * Represents a tool in the ToolGrid
 * 
 * @param id Unique identifier for the tool
 * @param name Name of the tool (displayed under the photo)
 * @param photoUrl URL to the tool's photo
 * @param description Description of the tool (shown as tooltip)
 */
data class Tool(
    val id: String,
    val name: String,
    val photoUrl: String,
    val description: String
)

/**
 * A grid component that displays a list of tools with photos, names, and descriptions.
 * The grid automatically adjusts its columns and rows based on the number of tools.
 * 
 * @param tools List of tools to display in the grid
 * @param selectedToolId ID of the currently selected tool (null if none selected)
 * @param onToolSelected Callback when a tool is selected
 */
@Composable
fun ToolGrid(
    tools: List<Tool>,
    selectedToolId: String? = null,
    onToolSelected: (Tool) -> Unit = {}
) {
    // Calculate the number of columns based on the number of tools
    // Use a minimum of 2 columns and a maximum of 4
    val columns = when {
        tools.size <= 4 -> 2
        else -> 3
    }

    // Check if dark mode is enabled
    val isDarkMode = rememberDarkMode()

    Div({
        style {
            display(DisplayStyle.Grid)
            property("grid-template-columns", "repeat($columns, 1fr)")
            gap(.5.r)
        }
    }) {
        tools.forEach { tool ->
            val isSelected = tool.id == selectedToolId

            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    alignItems(AlignItems.Center)
                    padding(.5.r)
                    borderRadius(.5.r)
                    cursor("pointer")
                    textAlign("center")

                    // Apply styles based on selection state and dark mode
                    if (isDarkMode) {
                        backgroundColor(if (isSelected) Styles.colors.primary else Styles.colors.dark.background)
                        color(Color.white)
                        border(2.px, LineStyle.Solid, Color.darkgray)
                    } else {
                        backgroundColor(if (isSelected) Styles.colors.primary else Color.white)
                        color(if (isSelected) Color.white else Color.black)
                        border(2.px, LineStyle.Solid, Color.lightgray)
                    }
                }

                // Add tooltip with description
                attr("title", tool.description)

                onClick {
                    // If the tool is already selected, call onToolSelected with the same tool
                    // The parent component can check if the selected tool is the same as the current one
                    // and handle deselection appropriately
                    onToolSelected(tool)
                }
            }) {
                // Tool photo
                Img(src = tool.photoUrl) {
                    style {
                        width(100.percent)
                        height(64.px)
                        property("object-fit", "contain")
                        marginBottom(.25.r)
                    }
                }

                // Tool name
                Text(tool.name)
            }
        }
    }
}

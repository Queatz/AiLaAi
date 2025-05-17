package app.game.editor

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.compose.rememberDarkMode
import org.jetbrains.compose.web.css.*
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

/**
 * Example usage of the ToolGrid component
 */
@Composable
fun ToolGridExample() {
    val exampleTools = listOf(
        Tool(
            id = "brush",
            name = "Brush",
            photoUrl = "/assets/icons/brush.svg",
            description = "Paint brush tool for drawing"
        ),
        Tool(
            id = "eraser",
            name = "Eraser",
            photoUrl = "/assets/icons/eraser.svg",
            description = "Eraser tool for removing content"
        ),
        Tool(
            id = "select",
            name = "Select",
            photoUrl = "/assets/icons/select.svg",
            description = "Selection tool for selecting objects"
        ),
        Tool(
            id = "move",
            name = "Move",
            photoUrl = "/assets/icons/move.svg",
            description = "Move tool for repositioning objects"
        ),
        Tool(
            id = "text",
            name = "Text",
            photoUrl = "/assets/icons/text.svg",
            description = "Text tool for adding text"
        )
    )

    var selectedToolId by remember { mutableStateOf<String?>(null) }

    ToolGrid(
        tools = exampleTools,
        selectedToolId = selectedToolId,
        onToolSelected = { tool ->
            // If the tool is already selected, deselect it
            // Otherwise, select the tool
            val isDeselecting = tool.id == selectedToolId
            selectedToolId = if (isDeselecting) null else tool.id
            println("Selected tool: ${if (isDeselecting) "None (deselected)" else tool.name}")
        }
    )
}

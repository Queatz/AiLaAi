package app.game.editor

import androidx.compose.runtime.Composable
import components.IconButton
import game.Game
import kotlinx.browser.document
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLCanvasElement
import r
import kotlin.js.Date

@Composable
fun ExportSection(game: Game?) {
    PanelSection(
        title = "Export",
        icon = "file_download",
        enabled = game != null,
        initiallyExpanded = false,
        closeOtherPanels = true
    ) {
        if (game == null) {
            Text("No game loaded")
            return@PanelSection
        }

        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(1.r)
                marginBottom(1.r)
            }
        }) {
            // Screenshot button
            Div({
                style {
                    display(DisplayStyle.Flex)
                    gap(0.5.r)
                    marginBottom(0.5.r)
                }
            }) {
                IconButton(
                    name = "photo_camera",
                    title = "Take Screenshot",
                    onClick = {
                        takeScreenshot(game.canvas)
                    }
                )
                Text("Take Screenshot")
            }
        }
    }
}

/**
 * Takes a screenshot of the canvas and downloads it as a PNG file
 */
private fun takeScreenshot(canvas: HTMLCanvasElement) {
    // Get the data URL of the canvas
    val dataUrl = canvas.toDataURL("image/png")

    // Create a timestamp for the filename
    val timestamp = Date().toISOString().replace(":", "-").replace(".", "-")
    val filename = "screenshot-$timestamp.png"

    // Create a temporary link element to trigger the download
    val link = document.createElement("a") as org.w3c.dom.HTMLAnchorElement
    link.href = dataUrl
    link.download = filename

    // Append to the document, click to download, then remove
    document.body?.appendChild(link)
    link.click()
    document.body?.removeChild(link)
}

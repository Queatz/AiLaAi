package app.game.editor

import Styles
import androidx.compose.runtime.Composable
import components.IconButton
import game.Game
import kotlinx.browser.document
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
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
                padding(1.r)
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(1.r)
            }
        }) {
            // Export Options Section with elevated style
            Div({
                style {
                    property("box-shadow", "1px 1px 4px rgba(0, 0, 0, 0.125)")
                    padding(1.r)
                    borderRadius(1.r)
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(1.r)
                }
            }) {
                Div({
                    style {
                        fontWeight("bold")
                        marginBottom(0.5.r)
                    }
                }) {
                    Text("Export Options")
                }

                // Screenshot button
                Button({
                    classes(Styles.button)
                    style {
                        width(100.percent)
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        justifyContent(JustifyContent.Center)
                        gap(0.5.r)
                    }
                    onClick {
                        takeScreenshot(game.canvas)
                    }
                }) {
                    // Material icon
                    Div({
                        classes("material-symbols-outlined")
                    }) {
                        Text("photo_camera")
                    }
                    Text("Take Screenshot")
                }
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

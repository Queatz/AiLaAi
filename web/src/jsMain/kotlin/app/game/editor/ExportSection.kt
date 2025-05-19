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
import kotlin.js.unsafeCast
import kotlin.js.Date
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.dom.Progress
import org.w3c.dom.url.URL
import web.timers.setTimeout
import web.timers.setInterval
import web.timers.clearInterval
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

@Composable
fun ExportSection(game: Game?) {
    var isRecording by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0.0) }
    val totalDuration = game?.animationData?.totalDuration ?: 0.0
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

        // Wrapper for export options (spacing via gap, no extra padding)
        Div({
            style {
                // padding removed to avoid double-padding
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

                if (!isRecording) {
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

                    // Download Video button
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
                            game.map.setCurrentGameMusic(null)
                            game.map.setCurrentGameTile(null)
                            game.map.setCurrentGameObject(null)
                            progress = 0.0
                            isRecording = true
                            val fps = 30
                            val totalMs = (totalDuration * 1000).toInt()
                            val stream = game.canvas.unsafeCast<dynamic>().captureStream(fps)
                            val chunks = mutableListOf<dynamic>()
                            val recorder = js("new MediaRecorder(stream, { mimeType: 'video/webm' })")
                            recorder.ondataavailable = { event: dynamic -> chunks.add(event.data) }
                            val intervalId = setInterval({
                                progress = (game.animationData.currentTime / totalDuration).coerceAtMost(1.0)
                            }, 100)
                            recorder.onstop = {
                                clearInterval(intervalId)
                                val blob = Blob(chunks.toTypedArray(), BlobPropertyBag("video/webm"))
                                val url = URL.createObjectURL(blob)
                                val timestamp = Date().toISOString().replace(":", "-").replace(".", "-")
                                val link = document.createElement("a") as org.w3c.dom.HTMLAnchorElement
                                link.href = url
                                link.download = "video-$timestamp.webm"
                                document.body?.appendChild(link)
                                link.click()
                                document.body?.removeChild(link)
                                URL.revokeObjectURL(url)
                                game.pause()
                                isRecording = false
                            }
                            recorder.start()
                            game.setTime(0.0)
                            game.play()
                            setTimeout({ recorder.stop() }, totalMs)
                        }
                    }) {
                        Div({ classes("material-symbols-outlined") }) {
                            Text("videocam")
                        }
                        Text("Download Video")
                    }
                } else {
                    Div({ style { fontWeight("bold") } }) {
                        Text("Rendering video... Please do not make changes to the scene until video is complete.")
                    }
                    Progress({
                        style { width(100.percent) }
                        attr("value", progress.toString())
                        attr("max", "1") }
                    ) {}
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

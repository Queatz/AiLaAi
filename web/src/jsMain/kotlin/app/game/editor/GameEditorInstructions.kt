package app.game.editor

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import r
import web.cssom.BorderCollapse
import web.cssom.PropertyName.Companion.borderCollapse

@Composable
fun GameEditorInstructions() {
    Div({
        style {
            padding(1.r)
        }
    }) {
        PanelSection(title = "View controls", icon = "videocam") {
            Table({
                style {
                    width(100.percent)
                    property(borderCollapse.toString(), BorderCollapse.collapse.toString())
                }
            }) {
                Tbody {
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("Tab") }
                        Td({ style { padding(0.5.r) } }) { Text("Switch camera view") }
                    }
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("W/A/S/D") }
                        Td({ style { padding(0.5.r) } }) { Text("Move") }
                    }
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("Ctrl+Shift+Mouse") }
                        Td({ style { padding(0.5.r) } }) { Text("Move") }
                    }
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("Arrows") }
                        Td({ style { padding(0.5.r) } }) { Text("Rotate") }
                    }
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("Shift+Mouse") }
                        Td({ style { padding(0.5.r) } }) { Text("Rotate") }
                    }
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("Alt+Arrows") }
                        Td({ style { padding(0.5.r) } }) { Text("Zoom") }
                    }
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("Mousewheel") }
                        Td({ style { padding(0.5.r) } }) { Text("Zoom") }
                    }
                }
            }
        }

        PanelSection(title = "Space controls", icon = "window") {
        Table({
                style {
                    width(100.percent)
                    property(borderCollapse.toString(), BorderCollapse.collapse.toString())
                }
            }) {
                Tbody {
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("Space") }
                        Td({ style { padding(0.5.r) } }) { Text("Toggle drawing plane") }
                    }
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("Shift+Space") }
                        Td({ style { padding(0.5.r) } }) { Text("Toggle drawing plane (reversed)") }
                    }
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("Ctrl+Space") }
                        Td({ style { padding(0.5.r) } }) { Text("Toggle drawing axis") }
                    }
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("Ctrl+Shift+Space") }
                        Td({ style { padding(0.5.r) } }) { Text("Toggle drawing axis (reversed)") }
                    }
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("Ctrl+Mouse") }
                        Td({ style { padding(0.5.r) } }) { Text("Auto adjust drawing plane") }
                    }
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("[ and ]") }
                        Td({ style { padding(0.5.r) } }) { Text("Manually adjust drawing plane") }
                    }
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("R") }
                        Td({ style { padding(0.5.r) } }) { Text("Toggle auto rotate") }
                    }
                }
            }
        }

        PanelSection(title = "Drawing", icon = "brush") {
            Table({
                style {
                    width(100.percent)
                    property(borderCollapse.toString(), BorderCollapse.collapse.toString())
                }
            }) {
                Tbody {
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("Mouse") }
                        Td({ style { padding(0.5.r) } }) { Text("Draw") }
                    }
                    Tr {
                        Th({ style { padding(0.5.r); textAlign("left"); fontWeight("bold") } }) { Text("Alt+Mouse") }
                        Td({ style { padding(0.5.r) } }) { Text("Erase") }
                    }
                }
            }
        }
    }
}

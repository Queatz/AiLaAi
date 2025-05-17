package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLDivElement
import r

@Composable
fun GameEditorInstructions() {
    Div({
        style {
            padding(1.r)
        }
    }) {
        B({
            style {
                color(Color.forestgreen)
                fontSize(18.px)
            }
        }) { Text("View controls") }
        Br()
        B { Text("Tab") }; Text(" Switch camera view"); Br()
        B { Text("W/A/S/D") }; Text(" Move"); Br()
        B { Text("Ctrl+Shift+Mouse") }; Text(" Move"); Br()
        B { Text("Arrows") }; Text(" Rotate"); Br()
        B { Text("Shift+Mouse") }; Text(" Rotate"); Br()
        B { Text("Alt+Arrows") }; Text(" Zoom"); Br()
        B { Text("Mousewheel") }; Text(" Zoom"); Br()
        Br()
        B({
            style {
                color(Color.forestgreen)
                fontSize(18.px)
            }
        }) { Text("Space controls") }
        Br()
        B { Text("Space") }; Text(" Toggle drawing plane"); Br()
        B { Text("Shift+Space") }; Text(" Toggle drawing plane (reversed)"); Br()
        B { Text("Ctrl+Space") }; Text(" Toggle drawing axis"); Br()
        B { Text("Ctrl+Shift+Space") }; Text(" Toggle drawing axis (reversed)"); Br()
        B { Text("Ctrl+Mouse") }; Text(" Auto adjust drawing plane"); Br()
        B { Text("[ and ]") }; Text(" Manually adjust drawing plane"); Br()
        B { Text("R") }; Text(" Toggle auto rotate"); Br()
        Br()
        B({
            style {
                color(Color.forestgreen)
                fontSize(18.px)
            }
        }) { Text("Drawing") }
        Br()
        B { Text("Mouse") }; Text(" Draw"); Br()
        B { Text("Alt+Mouse") }; Text(" Erase"); Br()
    }
}

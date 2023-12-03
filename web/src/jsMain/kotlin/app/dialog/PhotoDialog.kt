package app.dialog

import application
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.dom.Img
import r

suspend fun photoDialog(src: String) = dialog(
    title = null,
    confirmButton = application.appString { close },
    cancelButton = null
) { resolve ->
    Img(src = src) {
        style {
            borderRadius(1.r)
            maxHeight(75.vh)
        }

        onClick {
            resolve(false)
        }
    }
}

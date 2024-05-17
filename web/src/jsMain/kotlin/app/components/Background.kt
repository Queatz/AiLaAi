package app.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import application
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.Image

@Composable
fun Background(attrs: AttrsScope<HTMLDivElement>.() -> Unit = {}, content: @Composable () -> Unit) {
    val background by application.background.collectAsState(null)
    var loaded by remember { mutableStateOf(false) }
    var preload by remember { mutableStateOf<Image?>(null) }

    LaunchedEffect(background) {
        loaded = false
        background?.let { background ->
            preload = Image().apply {
                addEventListener("load", {
                    loaded = true
                    preload = null
                })
                src = background
            }
        }
    }

    Div(attrs) {
        Div({
            if (loaded) {
                classes(Styles.backgroundPhoto, Styles.backgroundPhotoLoaded)
            } else {
                classes(Styles.backgroundPhoto)
            }

            style {
                if (background != null && loaded) {
                    backgroundImage("url($background)")
                }
            }
        }) {

        }
        content()
    }
}

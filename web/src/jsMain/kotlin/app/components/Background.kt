package app.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import application
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.dom.Div

@Composable
fun Background(content: @Composable () -> Unit) {
    val background by application.background.collectAsState(null)

    Div({
        classes(Styles.background)

        style {
            if (background != null) {
                backgroundImage("url($background)")
            }
        }
    }) {
        content()
    }
}

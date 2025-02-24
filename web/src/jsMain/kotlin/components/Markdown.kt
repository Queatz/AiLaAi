package components

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import lib.marked
import org.jetbrains.compose.web.dom.Span
import org.w3c.dom.Element

@Composable
fun Markdown(content: String) {
    val html = remember(content) {
        marked.parse(content)
    }
    var element by remember { mutableStateOf<Element?>(null) }

    LaunchedEffect(html, element) {
        element?.innerHTML = html
    }

    Span(
        {
            classes(Styles.markdown)

            ref {
                element = it

                onDispose {
                    element = null
                }
            }
        }
    )
}

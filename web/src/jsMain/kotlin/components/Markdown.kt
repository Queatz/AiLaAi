package components

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import lib.marked
import org.jetbrains.compose.web.dom.Span
import org.w3c.dom.Element

@Composable
fun Markdown(content: String) {
    val html = remember(content) {
        val renderer = marked.Renderer()
        val originalLinkRenderer: dynamic = renderer.link

        renderer.link = { data ->
            val localLink = data.href.startsWith("${window.location.protocol}//${window.location.hostname}")
            val html = originalLinkRenderer.call(renderer, data) as String
            if (localLink) {
                html
            } else {
                html.replace(
                    regex = "^<a ".toRegex(),
                    replacement = "<a target=\"_blank\" rel=\"noreferrer noopener nofollow\" "
                )
            }
        }

        marked.parse(
            text = content,
            options = js("{ renderer: renderer }")
        )
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

package components

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import org.jetbrains.compose.web.dom.TagElement

@Composable
fun Wbr() {
    TagElement(elementBuilder = { document.createElement("wbr") }, applyAttrs = null, content = null)
}

package components

import androidx.compose.runtime.Composable
import com.queatz.db.splitByUrls
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun LinkifyText(text: String) {
    text.splitByUrls().forEach { (part, isUrl) ->
        if (isUrl) {
            A(
                href = part.let {
                    when {
                        it.contains("@") && !it.contains("/") -> "mailto:$it"
                        it.contains("://") -> it
                        else -> "https://$it"
                    }
                }, {
                    target(ATarget.Blank)
                    onClick {
                        it.stopPropagation()
                    }
                }) {
                Text(part)
            }
        } else {
            Span {
                Markdown(part)
            }
        }
    }
}

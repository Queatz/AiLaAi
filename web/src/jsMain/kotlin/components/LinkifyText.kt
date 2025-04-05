package components

import androidx.compose.runtime.Composable
import com.queatz.db.splitByUrls

@Composable
fun LinkifyText(text: String) = Markdown(
    text
        .splitByUrls()
        .joinToString("") { (part, isUrl) ->
            if (isUrl) {
                part.let {
                    when {
                        it.contains("@") && !it.contains("/") -> "[$it](mailto:$it)"
                        it.contains("://") -> it
                        else -> "[$it](https://$it)"
                    }
                }
            } else {
                part
            }
        }
)

package com.queatz.ailaai.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.queatz.db.splitByUrls

@Composable
fun LinkifyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    style: TextStyle = LocalTextStyle.current,
) {
    val textProcessed = remember(text) {
        text.splitByUrls().joinToString("") { (part, isUrl) ->
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
    }
    Markdown(
        content = textProcessed,
        colors = markdownColor(
            text = color,
            codeBackground = MaterialTheme.colorScheme.surfaceVariant,
        ),
        typography = markdownTypography(
            text = style.copy(color = color),
            code = style.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            ),
            inlineCode = style.copy(
                color = MaterialTheme.colorScheme.secondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
            textLink = TextLinkStyles(
                style = style.copy(color = MaterialTheme.colorScheme.primary).toSpanStyle()
            )
        ),
        modifier = modifier
    )
}

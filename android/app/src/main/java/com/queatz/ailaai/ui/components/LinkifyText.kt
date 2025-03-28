package com.queatz.ailaai.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.BasicRichText
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.RichTextThemeProvider
import com.halilibo.richtext.ui.string.RichTextString.Format.Code
import com.halilibo.richtext.ui.string.RichTextStringStyle
import com.queatz.db.splitByUrls

@Composable
fun LinkifyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    style: TextStyle = LocalTextStyle.current,
) {
    val text = remember(text) {
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
    RichTextThemeProvider(
        contentColorProvider = {
            color
        },
        textStyleProvider = {
            style.copy(color = color)
        },
        contentColorBackProvider = { newContentColor, content ->
            CompositionLocalProvider(LocalContentColor provides newContentColor) {
                content()
            }
        },
        textStyleBackProvider = { newTextStyle, content ->
            ProvideTextStyle(newTextStyle, content)
        }
    ) {
        BasicRichText(
            modifier = modifier,
            style = RichTextStyle(
                // todo - this isn't applied to code blocks - investigate
                codeBlockStyle = CodeBlockStyle(
                    textStyle = style.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                ),
                stringStyle = RichTextStringStyle(
                    codeStyle = style.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ).toSpanStyle(), linkStyle = TextLinkStyles(
                        style = style.copy(color = MaterialTheme.colorScheme.primary).toSpanStyle()
                    )
                )
            )
        ) {
            Markdown(
                content = text
            )
        }
    }
}

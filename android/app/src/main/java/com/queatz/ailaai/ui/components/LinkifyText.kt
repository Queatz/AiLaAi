package com.queatz.ailaai.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.BasicRichText
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.RichTextThemeProvider
import com.halilibo.richtext.ui.string.RichTextStringStyle

@Composable
fun LinkifyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    RichTextThemeProvider(
        textStyleProvider = {
            style.copy(
                color = color,
                textAlign = textAlign ?: LocalTextStyle.current.textAlign
            )
        }
    ) {
        BasicRichText(
            modifier = modifier,
            style = RichTextStyle(
                stringStyle = RichTextStringStyle(
                    linkStyle = TextLinkStyles(
                        style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.primary).toSpanStyle()
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

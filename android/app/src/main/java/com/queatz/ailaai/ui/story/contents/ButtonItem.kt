package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import com.queatz.db.ButtonStyle
import com.queatz.db.StoryContent

fun LazyGridScope.buttonItem(
    content: StoryContent.Button,
    onButtonClick: ((script: String, data: String?) -> Unit)?
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        if (content.style == ButtonStyle.Secondary) {
            OutlinedButton(
                onClick = {
                    onButtonClick?.invoke(content.script, content.data)
                }
            ) {
                Text(content.text)
            }
        } else {
            Button(
                onClick = {
                    onButtonClick?.invoke(content.script, content.data)
                }
            ) {
                Text(content.text)
            }
        }
    }
}

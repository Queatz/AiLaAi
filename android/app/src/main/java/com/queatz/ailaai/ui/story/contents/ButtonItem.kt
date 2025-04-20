package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.queatz.db.ButtonStyle
import com.queatz.db.StoryContent

fun LazyGridScope.buttonItem(
    content: StoryContent.Button,
    onButtonClick: ((script: String, data: String?) -> Unit)?
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        var isDisabled by remember(content, onButtonClick) { mutableStateOf(false) }

        if (content.style == ButtonStyle.Secondary) {
            OutlinedButton(
                onClick = {
                    onButtonClick?.invoke(content.script, content.data)
                    isDisabled = true
                },
                enabled = !isDisabled
            ) {
                Text(content.text)
            }
        } else {
            Button(
                onClick = {
                    onButtonClick?.invoke(content.script, content.data)
                    isDisabled = true
                },
                enabled = !isDisabled
            ) {
                Text(content.text)
            }
        }
    }
}

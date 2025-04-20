package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.queatz.db.ButtonStyle
import com.queatz.db.StoryContent
import kotlinx.coroutines.launch

fun LazyGridScope.buttonItem(
    content: StoryContent.Button,
    onButtonClick: (suspend (script: String, data: String?) -> Unit)?
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        val scope = rememberCoroutineScope()
        var isDisabled by remember(content) { mutableStateOf(false) }

        if (content.style == ButtonStyle.Secondary) {
            OutlinedButton(
                onClick = {
                    isDisabled = true
                    scope.launch {
                        onButtonClick?.invoke(content.script, content.data)
                        isDisabled = false
                    }
                },
                enabled = !isDisabled
            ) {
                Text(content.text)
            }
        } else {
            Button(
                onClick = {
                    isDisabled = true
                    scope.launch {
                        onButtonClick?.invoke(content.script, content.data)
                        isDisabled = false
                    }
                },
                enabled = !isDisabled
            ) {
                Text(content.text)
            }
        }
    }
}

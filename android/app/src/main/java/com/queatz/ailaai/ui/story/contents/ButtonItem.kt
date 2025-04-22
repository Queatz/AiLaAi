package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.core.graphics.toColorInt

fun LazyGridScope.buttonItem(
    content: StoryContent.Button,
    onButtonClick: (suspend (script: String, data: String?) -> Unit)?
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        val scope = rememberCoroutineScope()
        var isDisabled by remember(content) { mutableStateOf(false) }

        val onClick: () -> Unit = remember(content) {
            {
                isDisabled = true
                scope.launch {
                    onButtonClick?.invoke(content.script, content.data)
                    isDisabled = false
                }
            }
        }

        val customColor = try {
            content.color?.let {
                androidx.compose.ui.graphics.Color(it.toColorInt())
            }
        } catch (e: IllegalArgumentException) {
            null
        }

        if (content.style == ButtonStyle.Secondary) {
            OutlinedButton(
                onClick = onClick,
                enabled = !isDisabled,
                colors = if (customColor == null) {
                    ButtonDefaults.outlinedButtonColors()
                } else {
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = customColor
                    )
                }
            ) {
                Text(content.text)
            }
        } else {
            Button(
                onClick = onClick,
                enabled = !isDisabled,
                colors = if (customColor == null) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = customColor
                    )
                }
            ) {
                Text(content.text)
            }
        }
    }
}

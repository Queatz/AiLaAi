package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.db.StoryContent

fun LazyGridScope.inputItem(
    content: StoryContent.Input,
    onValueChange: (String) -> Unit
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        var value by rememberStateOf(content.value)
        OutlinedTextField(
            value = value.orEmpty(),
            onValueChange = {
                value = it
                onValueChange(it)
            },
            shape = MaterialTheme.shapes.large,
            placeholder = content.hint?.notBlank?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

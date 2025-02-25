package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.queatz.ailaai.ui.components.LinkifyText
import com.queatz.db.StoryContent

fun LazyGridScope.textItem(content: StoryContent.Text) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        LinkifyText(
            text = content.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.queatz.db.StoryContent

fun LazyGridScope.sectionItem(content: StoryContent.Section) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            content.section,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

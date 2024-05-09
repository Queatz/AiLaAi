package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import com.queatz.ailaai.ui.story.StoryAuthors
import com.queatz.db.StoryContent

fun LazyGridScope.authorsItem(content: StoryContent.Authors) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        StoryAuthors(
            content.publishDate,
            content.authors
        )
    }
}

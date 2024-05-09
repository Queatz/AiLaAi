package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.Text

fun LazyGridScope.commentsItem() {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text("Comments!")
    }
}

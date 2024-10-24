package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.queatz.ailaai.data.api
import com.queatz.ailaai.ui.components.Audio
import com.queatz.db.StoryContent

fun LazyGridScope.audioItem(content: StoryContent.Audio) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        DisableSelection {
            Card(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
            ) {
                Audio(
                    url = api.url(content.audio),
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}

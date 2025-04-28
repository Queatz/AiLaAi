package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.queatz.ailaai.data.api
import com.queatz.ailaai.ui.components.Video
import com.queatz.db.StoryContent

fun LazyGridScope.videoItem(content: StoryContent.Video) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        DisableSelection {
            Video(
                url = content.video.let(api::url),
                showController = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(MaterialTheme.shapes.large)
            )
        }
    }
}

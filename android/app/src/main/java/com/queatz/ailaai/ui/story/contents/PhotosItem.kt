package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.inDp
import com.queatz.db.StoryContent

fun LazyGridScope.photosItem(content: StoryContent.Photos, viewHeight: Float) {
    itemsIndexed(
        content.photos,
        span = { index, item ->
            GridItemSpan(if (index == 0) maxLineSpan else if (index % 3 == 1) 1 else maxCurrentLineSpan)
        }
    ) { index, it ->
        DisableSelection {
            AsyncImage(
                ImageRequest.Builder(LocalContext.current)
                    .data(api.url(it))
                    .crossfade(true)
                    .build(),
                contentDescription = "",
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .fillMaxWidth()
                    .then(
                        if (content.aspect != null) {
                            Modifier
                                .aspectRatio(content.aspect!!)
                                .heightIn(min = 240.dp.coerceAtMost(viewHeight.inDp()), max = viewHeight.inDp())
                        } else {
                            Modifier
                        }
                    )
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            )
        }
    }
}

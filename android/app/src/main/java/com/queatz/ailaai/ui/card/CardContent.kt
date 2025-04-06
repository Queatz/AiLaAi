package com.queatz.ailaai.ui.card

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.queatz.ailaai.ui.story.StoryContents
import com.queatz.ailaai.ui.story.StorySource
import com.queatz.ailaai.ui.story.asStoryContents

@Composable
fun CardContent(
    source: StorySource,
    content: String,
) {
    val state = rememberLazyGridState()
    val contents by remember(content) { mutableStateOf(content.asStoryContents()) }

    StoryContents(
        source = source,
        content = contents,
        state = state,
        fade = true
    )
}

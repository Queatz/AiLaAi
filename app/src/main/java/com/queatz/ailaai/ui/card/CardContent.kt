package com.queatz.ailaai.ui.card

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.queatz.ailaai.ui.story.StoryContents
import com.queatz.ailaai.ui.story.asStoryContents
import com.queatz.db.Person

@Composable
fun CardContent(
    content: String,
    navController: NavController,
    me: () -> Person?
) {
    val state = rememberLazyGridState()
    val contents by remember(content) { mutableStateOf(content.asStoryContents()) }

    StoryContents(
        contents,
        state,
        navController,
        me
    )
}

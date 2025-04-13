package com.queatz.ailaai.ui.story

import com.queatz.db.StoryContent
import kotlin.random.Random.Default.nextInt

class CreatorScope<T : StoryContent>(
    val id: Int = nextInt(),
    val source: StorySource,
    val part: T,
    val partIndex: Int,
    val currentFocus: Int,
    val onCurrentFocus: (Int) -> Unit,
    val add: (part: StoryContent, position: Int?) -> Unit,
    val remove: (position: Int) -> Unit,
    val edit: (block: T.() -> T) -> Unit
)

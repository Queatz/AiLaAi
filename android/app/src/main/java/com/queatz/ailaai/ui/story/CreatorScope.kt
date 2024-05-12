package com.queatz.ailaai.ui.story

import com.queatz.db.StoryContent

class CreatorScope<T : StoryContent>(
    val source: StorySource,
    val part: T,
    val partIndex: Int,
    val currentFocus: Int,
    val onCurrentFocus: (Int) -> Unit,
    val add: (part: StoryContent, position: Int?) -> Unit,
    val remove: (position: Int) -> Unit,
    val edit: (block: T.() -> Unit) -> Unit
)

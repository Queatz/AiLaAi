package com.queatz.ailaai.ui.story.creator

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.story.CreatorScope
import com.queatz.ailaai.ui.story.EditorTextField
import com.queatz.db.StoryContent

fun LazyGridScope.titleCreatorItem(creatorScope: CreatorScope<StoryContent.Title>) = with(creatorScope) {
    item(span = { GridItemSpan(maxLineSpan) }, key = part.hashCode()) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(currentFocus) {
            if (currentFocus == partIndex) {
                focusRequester.requestFocus()
            }
        }
        EditorTextField(
            value = part.title,
            onValueChange = {
                edit {
                    copy(
                        title = it
                    )
                }
            },
            focusRequester = focusRequester,
            placeholder = stringResource(R.string.title),
            singleLine = false,
            onFocus = {
                onCurrentFocus(partIndex)
            },
            textStyle = { headlineMedium }
        )
    }
}

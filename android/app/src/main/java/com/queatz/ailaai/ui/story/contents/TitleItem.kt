package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.extensions.navigate
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.StoryContent

fun LazyGridScope.titleItem(
    content: StoryContent.Title,
    actions: @Composable ((storyId: String) -> Unit)?
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        val nav = nav

        Row(
            horizontalArrangement = Arrangement.spacedBy(1.pad),
        ) {
            Text(
                content.title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .let {
                        if (actions == null) {
                            it
                        } else {
                            it.padding(top = 1.pad)
                                .clickable(
                                    remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    nav.navigate(AppNav.Story(content.id))
                                }
                        }
                    }
            )
            // https://issuetracker.google.com/issues/300781578
            DisableSelection {
                actions?.invoke(content.id)
            }
        }
    }
}

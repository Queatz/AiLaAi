package com.queatz.ailaai.ui.story

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Story

@Composable
fun StoryCard(
    story: Story?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = {
            onClick()
        },
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(.5f.pad),
            modifier = Modifier
                .padding(2.pad)
        ) {
            story?.also { story ->
                Text(
                    story.title?.notBlank ?: stringResource(R.string.empty_story_name),
                    style = MaterialTheme.typography.headlineSmall
                )
                StoryAuthors(
                    nav,
                    story.publishDate,
                    story.authors ?: emptyList()
                )
                story.textContent().notBlank?.let {
                    Text(it, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            } ?: run {
                Text(
                    if (!isLoading) stringResource(R.string.story_not_found) else stringResource(R.string.please_wait),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.alpha(0.5f)
                )
            }
        }
    }
}

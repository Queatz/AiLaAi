package com.queatz.ailaai.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.comment
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.contactPhoto
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.navigate
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.components.LinkifyText
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.CommentExtended

@Composable
fun StoryComments(comments: List<CommentExtended>) {
    val scope = rememberCoroutineScope()
    val nav = nav

    var loadedCommentReplies by remember {
        mutableStateOf(emptyMap<String, List<CommentExtended>>())
    }

    suspend fun loadCommentReplies(comment: CommentExtended) {
        api.comment(comment.comment!!.id!!) {
            loadedCommentReplies = loadedCommentReplies + (it.comment!!.id!! to it.replies!!)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(1.pad)
    ) {
        comments.forEach { comment ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.pad)
            ) {
                GroupPhoto(
                    comment.person!!.contactPhoto().inList(),
                    padding = 0.pad,
                    size = 32.dp,
                    modifier = Modifier
                        .clickable {
                            nav.navigate(AppNav.Profile(comment.person!!.id!!))
                        }
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(.25f.pad),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(.25f.pad),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(1.pad)
                    ) {
                        Text(
                            comment.person!!.name ?: stringResource(R.string.someone),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        LinkifyText(comment.comment!!.comment!!)
                    }
                    Text(
                        comment.comment!!.createdAt!!.timeAgo(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .padding(start = 1.pad)
                    )
                }
            }
        }
    }
}

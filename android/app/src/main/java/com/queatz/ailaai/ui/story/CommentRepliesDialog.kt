package com.queatz.ailaai.ui.story

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ailaai.api.comment
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.CommentExtended
import kotlinx.coroutines.launch

@Composable
fun CommentRepliesDialog(
    onDismissRequest: () -> Unit,
    comment: CommentExtended,
    onCommentDeleted: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var comment by rememberStateOf(comment)

    fun reloadComment() {
        scope.launch {
            api.comment(comment.comment!!.id!!, onError = {
                onCommentDeleted()
            }) {
                comment = it
            }
        }
    }

    DialogBase(onDismissRequest) {
        DialogLayout(
            content = {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.comment_replies),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(bottom = 1.pad)
                    )
                }
                SelectionContainer {
                    StoryComments(
                        listOf(comment),
                        loadRepliesInline = true,
                        onCommentUpdated = {
                            reloadComment()
                        }
                    )
                }
            },
             actions = {
                 DialogCloseButton(onDismissRequest)
            }
        )
    }
}

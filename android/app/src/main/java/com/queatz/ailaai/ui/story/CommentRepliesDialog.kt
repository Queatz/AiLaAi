package com.queatz.ailaai.ui.story

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.CommentExtended

@Composable
fun CommentRepliesDialog(onDismissRequest: () -> Unit, comment: CommentExtended) {
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
                StoryComments(listOf(comment), loadRepliesInline = true)
            },
             actions = {
                TextButton(onDismissRequest) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

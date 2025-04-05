package com.queatz.ailaai.ui.story.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.ailaai.api.sendMessage
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.db.Message
import com.queatz.db.StoryAttachment
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Composable
fun SendStoryDialog(
    onDismissRequest: () -> Unit,
    storyId: String,
) {
    val context = LocalContext.current
    val sent = stringResource(R.string.sent)
    val someone = stringResource(R.string.someone)
    val emptyGroup = stringResource(R.string.empty_group_name)
    val me = me

    ChooseGroupDialog(
        {
            onDismissRequest()
        },
        title = stringResource(R.string.send_story),
        confirmFormatter = defaultConfirmFormatter(
            R.string.send_story,
            R.string.send_story_to_group,
            R.string.send_story_to_groups,
            R.string.send_story_to_x_groups
        ) { it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()) },
    ) { groups ->
        coroutineScope {
            var hasError = false
            groups.map { group ->
                async {
                    api.sendMessage(
                        group.id!!,
                        Message(attachment = json.encodeToString(StoryAttachment(storyId))),
                        onError = {
                            hasError = true
                        }
                    )
                }
            }.awaitAll()
            if (hasError) {
                context.showDidntWork()
            } else {
                context.toast(sent)
            }
        }
    }
}

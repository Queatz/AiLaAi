package com.queatz.ailaai.group

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.db.GroupAttachment
import com.queatz.db.Message
import kotlinx.serialization.encodeToString

@Composable
fun SendGroupDialog(
    onDismissRequest: () -> Unit,
    groupId: String
) {
    val someone = stringResource(R.string.someone)
    val emptyGroup = stringResource(R.string.empty_group_name)
    val me = me

    SendMessageDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.send_group),
        confirmFormatter = defaultConfirmFormatter(
            R.string.send_group,
            R.string.send_group_to_group,
            R.string.send_group_to_groups,
            R.string.send_group_to_x_groups
        ) { it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()) },
        message = Message(attachment = json.encodeToString(GroupAttachment(groupId)))
    )
}

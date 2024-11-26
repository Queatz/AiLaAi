package com.queatz.ailaai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.bold
import com.queatz.ailaai.extensions.isGroupLike
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Group

@Composable
fun ChooseGroups(
    groups: List<Group>,
    hint: String? = null,
    onGroups: (List<Group>) -> Unit)
{
    val me = me
    var showGroupsDialog by rememberStateOf(false)

    if (showGroupsDialog) {
        val someone = stringResource(R.string.someone)
        val emptyGroup = stringResource(R.string.empty_group_name)
        ChooseGroupDialog(
            onDismissRequest = {
                showGroupsDialog = false
            },
            title = stringResource(R.string.share),
            confirmFormatter = defaultConfirmFormatter(
                R.string.choose_none,
                R.string.choose_x,
                R.string.choose_x_and_x,
                R.string.choose_x_groups
            ) { it.name(someone, emptyGroup, omit = me?.id?.let(::listOf) ?: emptyList()) },
            filter = { it.isGroupLike() },
            allowNone = true,
            preselect = groups
        ) {
            onGroups(it)
        }
    }

    if (groups.isNotEmpty()) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = if (groups.size == 1) Alignment.CenterVertically else Alignment.Top,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                    showGroupsDialog = true
                }
                .padding(vertical = .5f.pad)
        ) {
            Icon(
                imageVector = Icons.Outlined.Forum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 1.pad)
            )
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.shared_in_))
                    append(" ")
                    groups.forEachIndexed { index, group ->
                        if (index > 0) append(", ")
                        bold {
                            append(group.name ?: "")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f, fill = false)
            )
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = .5f.pad)
                    .size(16.dp)
            )
        }
    } else {
        OutlinedButton(
            onClick = {
                showGroupsDialog = true
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.GroupAdd,
                contentDescription = null,
                modifier = Modifier.padding(end = 1.pad)
            )
            Text(stringResource(R.string.share_to_groups))
        }
        hint?.let { hint ->
            Text(
                text = hint,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

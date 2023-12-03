package com.queatz.ailaai.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.ailaai.api.updateGroup
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.db.Group

@Composable
fun RenameGroupDialog(onDismissRequest: () -> Unit, group: Group, onGroupUpdated: (Group) -> Unit) {
    TextFieldDialog(
        onDismissRequest,
        stringResource(R.string.rename_group),
        stringResource(R.string.rename),
        true,
        group.name ?: "",
    ) { value ->
        api.updateGroup(group.id!!, Group().apply { name = value }) { group ->
            onGroupUpdated(group)
            onDismissRequest()
        }
    }
}

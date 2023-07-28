package com.queatz.ailaai.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.data.Group
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.api.updateGroup

@Composable
fun GroupDescriptionDialog(onDismissRequest: () -> Unit, group: Group, onGroupUpdated: (Group) -> Unit) {
    TextFieldDialog(
        onDismissRequest,
        stringResource(R.string.introduction),
        stringResource(R.string.update),
        false,
        group.description ?: "",
    ) { value ->
        api.updateGroup(group.id!!, Group().apply { description = value }) { group ->
            onGroupUpdated(group)
            onDismissRequest()
        }
    }
}

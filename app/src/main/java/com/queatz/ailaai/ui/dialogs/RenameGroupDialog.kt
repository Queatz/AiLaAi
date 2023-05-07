package com.queatz.ailaai.ui.dialogs

import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.Group
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.extensions.showDidntWork

@Composable
fun RenameGroupDialog(onDismissRequest: () -> Unit, group: Group, onGroupUpdated: (Group) -> Unit) {
    val context = LocalContext.current

    TextFieldDialog(
        onDismissRequest,
        stringResource(R.string.rename_group),
        stringResource(R.string.rename),
        true,
        group.name ?: "",
    ) { value ->
        try {
            val group = api.updateGroup(group.id!!, Group().apply { name = value })
            onGroupUpdated(group)
            onDismissRequest()
        } catch (ex: Exception) {
            ex.printStackTrace()
            context.showDidntWork()
        }
    }
}

package com.queatz.ailaai.ui.dialogs

import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.Group
import com.queatz.ailaai.R
import com.queatz.ailaai.api

@Composable
fun GroupDescriptionDialog(onDismissRequest: () -> Unit, group: Group, onGroupUpdated: (Group) -> Unit) {
    val context = LocalContext.current
    val didntWork = stringResource(R.string.didnt_work)

    TextFieldDialog(
        onDismissRequest,
        stringResource(R.string.introduction),
        stringResource(R.string.update),
        false,
        group.description ?: "",
    ) { value ->
        try {
            val group = api.updateGroup(group.id!!, Group().apply { description = value })
            onGroupUpdated(group)
            onDismissRequest()
        } catch (ex: Exception) {
            Toast.makeText(context, didntWork, LENGTH_SHORT).show()
            ex.printStackTrace()
        }
    }
}

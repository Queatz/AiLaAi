package com.queatz.ailaai.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.extensions.showDidntWork

@Composable
fun EditProfileNameDialog(onDismissRequest: () -> Unit, initialValue: String, onUpdated: () -> Unit) {
    val context = LocalContext.current

    TextFieldDialog(
        onDismissRequest,
        stringResource(R.string.your_name),
        stringResource(R.string.update_your_name),
        true,
        initialValue,
    ) { value ->
        try {
            api.updateMe(Person(name = value.trim()))
            onUpdated()
            onDismissRequest()
        } catch (ex: Exception) {
            ex.printStackTrace()
            context.showDidntWork()
        }
    }
}

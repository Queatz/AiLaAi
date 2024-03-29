package com.queatz.ailaai.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.ailaai.api.updateMe
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.db.Person

@Composable
fun EditProfileNameDialog(onDismissRequest: () -> Unit, initialValue: String, onUpdated: () -> Unit) {
    TextFieldDialog(
        onDismissRequest,
        stringResource(R.string.your_name),
        stringResource(R.string.update_your_name),
        true,
        initialValue,
    ) { value ->
        api.updateMe(Person(name = value.trim())) {
            onUpdated()
            onDismissRequest()
        }
    }
}

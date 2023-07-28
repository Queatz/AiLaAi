package com.queatz.ailaai.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.data.Profile
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.api.updateProfile

@Composable
fun EditProfileAboutDialog(onDismissRequest: () -> Unit, initialValue: String, onUpdated: () -> Unit) {
    TextFieldDialog(
        onDismissRequest,
        stringResource(R.string.introduction),
        stringResource(R.string.update),
        false,
        initialValue,
    ) { value ->
        api.updateProfile(Profile(about = value.trim())) {
            onUpdated()
            onDismissRequest()
        }
    }
}

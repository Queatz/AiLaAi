package com.queatz.ailaai.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.Profile
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.api.updateProfile
import com.queatz.ailaai.extensions.showDidntWork

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

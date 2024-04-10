package com.queatz.ailaai.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.ailaai.api.updateProfile
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.db.Profile

@Composable
fun EditProfileAboutDialog(onDismissRequest: () -> Unit, initialValue: String, onUpdated: () -> Unit) {
    TextFieldDialog(
        onDismissRequest,
        stringResource(R.string.introduction),
        stringResource(R.string.update),
        false,
        initialValue,
        align = TextAlign.Center
    ) { value ->
        api.updateProfile(Profile(about = value.trim())) {
            onUpdated()
            onDismissRequest()
        }
    }
}

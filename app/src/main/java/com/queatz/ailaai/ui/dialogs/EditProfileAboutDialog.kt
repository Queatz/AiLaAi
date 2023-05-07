package com.queatz.ailaai.ui.dialogs

import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.Profile
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.extensions.showDidntWork

@Composable
fun EditProfileAboutDialog(onDismissRequest: () -> Unit, initialValue: String, onUpdated: () -> Unit) {
    val context = LocalContext.current

    TextFieldDialog(
        onDismissRequest,
        stringResource(R.string.about_you),
        stringResource(R.string.update),
        false,
        initialValue,
    ) { value ->
        try {
            api.updateProfile(Profile(about = value.trim()))
            onUpdated()
            onDismissRequest()
        } catch (ex: Exception) {
            ex.printStackTrace()
            context.showDidntWork()
        }
    }
}

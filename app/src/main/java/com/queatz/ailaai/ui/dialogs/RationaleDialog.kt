package com.queatz.ailaai.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.goToSettings
import com.queatz.ailaai.nav

@Composable
fun RationaleDialog(onDismissRequest: () -> Unit, message: String) {
    val nav = nav

    AlertDialog(
        onDismissRequest,
        text = {
            Text(message)
        },
        confirmButton = {
            TextButton(
                {
                    onDismissRequest()
                    nav.goToSettings()
                }
            ) {
                Text(stringResource(R.string.open_settings))
            }
        }
    )
}

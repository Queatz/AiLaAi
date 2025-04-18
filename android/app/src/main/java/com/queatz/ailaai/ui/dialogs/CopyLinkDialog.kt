package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.queatz.ailaai.R
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.extensions.copyToClipboard
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.theme.pad

@Composable
fun CopyLinkDialog(
    onDismissRequest: () -> Unit,
    code: String
) {
    val context = LocalContext.current
    val inviteLink = "$appDomain/invite/$code"

    DialogBase(onDismissRequest) {
        DialogLayout(
            horizontalAlignment = Alignment.CenterHorizontally,
            content = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1.pad)
                ) {
                    Text(
                        text = stringResource(R.string.copy_link),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(bottom = 1.pad)
                            .fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inviteLink,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                }
            },
            actions = {
                DialogCloseButton(onDismissRequest)

                TextButton(
                    onClick = {
                        inviteLink.copyToClipboard(context)
                        context.toast(context.getString(R.string.copied))
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.copy))
                }
            }
        )
    }
}

package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.api.invite
import com.queatz.ailaai.extensions.shareAsText
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun InviteDialog(meName: String, onDismissRequest: () -> Unit) {
    var inviteCode by remember { mutableStateOf("") }
    val errorString = stringResource(R.string.error)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        inviteCode = ""
        api.invite(
            onError = {
                inviteCode = errorString
            }
        ) {
            inviteCode = it.code ?: ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        {
            if (inviteCode != errorString && inviteCode.isNotBlank()) {
                TextButton(
                    {
                        onDismissRequest()
                        context.getString(R.string.invite_text, meName, inviteCode).shareAsText(context)
                    }
                ) {
                    Text(stringResource(R.string.share))
                }
            } else {
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(R.string.invite_code)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(PaddingDefault)
            ) {
                if (inviteCode.isBlank()) {
                    CircularProgressIndicator()
                } else {
                    SelectionContainer {
                        Text(inviteCode, style = MaterialTheme.typography.displayMedium)
                    }
                    Text(
                        stringResource(R.string.invite_code_description),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    )
}

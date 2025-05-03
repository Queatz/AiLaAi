package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import app.ailaai.api.createQuickInvite
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.extensions.shareAsText
import com.queatz.ailaai.ui.theme.pad

@Composable
fun InviteDialog(meName: String, onDismissRequest: () -> Unit) {
    var inviteCode by remember { mutableStateOf("") }
    val errorString = stringResource(R.string.error)
    val context = LocalContext.current

    val inviteUrl = "$appDomain/invite/$inviteCode"

    val qrCode = remember(inviteUrl) {
        inviteUrl.takeIf { it.isNotBlank() }?.buildQrBitmap()
    }

    LaunchedEffect(Unit) {
        inviteCode = ""
        api.createQuickInvite(
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
                DialogCloseButton(onDismissRequest)
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(R.string.invite_code)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(1.pad),
                horizontalAlignment = CenterHorizontally,
            ) {
                if (inviteCode.isBlank()) {
                    CircularProgressIndicator()
                } else {
                    qrCode?.let {
                        Image(
                            it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .background(Color.White)

                        )
                    }
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

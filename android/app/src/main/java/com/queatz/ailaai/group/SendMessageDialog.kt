package com.queatz.ailaai.group

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.ailaai.api.sendMessage
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.db.GroupExtended
import com.queatz.db.Message
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Composable
fun SendMessageDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmFormatter: @Composable (List<GroupExtended>) -> String,
    message: Message
) {
    val context = LocalContext.current
    val sent = stringResource(R.string.sent)

    ChooseGroupDialog(
        onDismissRequest = {
            onDismissRequest()
        },
        title = title,
        confirmFormatter = confirmFormatter,
    ) { groups ->
        coroutineScope {
            var hasError = false
            groups.map { group ->
                async {
                    api.sendMessage(
                        group.id!!,
                        message,
                        onError = {
                            hasError = true
                        }
                    )
                }
            }.awaitAll()
            if (hasError) {
                context.showDidntWork()
            } else {
                context.toast(sent)
            }
        }
    }
}

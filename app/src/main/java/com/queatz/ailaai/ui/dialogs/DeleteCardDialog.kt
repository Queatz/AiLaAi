package com.queatz.ailaai.ui.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.data.Card
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.api.deleteCard
import com.queatz.ailaai.extensions.rememberStateOf
import kotlinx.coroutines.launch

@Composable
fun DeleteCardDialog(card: Card, onDismissRequest: () -> Unit, onChange: () -> Unit) {
    var disableSubmit by rememberStateOf(false)
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest,
        confirmButton = {
            TextButton(
                {
                    disableSubmit = true

                    coroutineScope.launch {
                        api.deleteCard(card.id!!) {
                            onChange()
                            onDismissRequest()
                        }
                        disableSubmit = false
                    }
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                enabled = !disableSubmit
            ) {
                Text(stringResource(R.string.delete_card))
            }
        },
        dismissButton = {
            TextButton({
                onDismissRequest()
            }) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = {
            Text(stringResource(R.string.delete_this_card_q))
        },
        text = {
            Text(stringResource(R.string.you_cannot_undo_this_card))
        })
}

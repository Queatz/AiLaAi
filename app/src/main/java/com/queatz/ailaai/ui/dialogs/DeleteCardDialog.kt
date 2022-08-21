package com.queatz.ailaai.ui.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.Card
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import kotlinx.coroutines.launch

@Composable
fun DeleteCardDialog(card: Card, onDismissRequest: () -> Unit, onChange: () -> Unit) {
    var disableSubmit by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(onDismissRequest,
        confirmButton = {
            TextButton(
                {
                    disableSubmit = true

                    coroutineScope.launch {
                        try {
                            api.deleteCard(card.id!!)
                            onChange()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        } finally {
                            disableSubmit = false
                            onDismissRequest()
                        }
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
            Text(stringResource(R.string.you_cannot_undo_this))
        })
}
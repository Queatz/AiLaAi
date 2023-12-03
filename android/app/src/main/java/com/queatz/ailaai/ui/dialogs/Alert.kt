package com.queatz.ailaai.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun Alert(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    dismissButton: String,
    confirmButton: String,
    confirmColor: Color? = null,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest,
        title = {
            Text(title)
        },
        text = {
            Text(text)
        },
        dismissButton = {
            TextButton(
                {
                    onDismissRequest()
                }
            ) {
                Text(dismissButton)
            }
        },
        confirmButton = {
            TextButton(
                {
                    onConfirm()
                }
            ) {
                Text(confirmButton, color = confirmColor ?: Color.Unspecified)
            }
        }
    )
}

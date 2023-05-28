package com.queatz.ailaai.ui.story.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun SaveChangesDialog(
    onDismissRequest: () -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        {
            onDismissRequest()
        },
        title = {
            Text(stringResource(R.string.save_changes))
        },
        text = {
            Text(stringResource(R.string.all_changes_will_be_lost))
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(PaddingDefault)) {
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    {
                        onDiscard()
                    }
                ) {
                    Text(stringResource(R.string.discard_changes), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                {
                    onSave()
                }
            ) {
                Text(stringResource(R.string.save))
            }
        }
    )
}

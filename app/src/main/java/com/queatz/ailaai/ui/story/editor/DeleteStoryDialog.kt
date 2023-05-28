package com.queatz.ailaai.ui.story.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.dialogs.Alert

@Composable
fun DeleteStoryDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit
) {
    Alert(
        { onDismissRequest() },
        title = stringResource(R.string.delete_story),
        text = stringResource(R.string.you_cannot_undo_this_story),
        dismissButton = stringResource(R.string.cancel),
        confirmButton = stringResource(R.string.delete),
        confirmColor = MaterialTheme.colorScheme.error
    ) {
        onDelete()
    }
}

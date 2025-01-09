package com.queatz.ailaai.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.ui.components.ReactQuickLayout

@Composable
fun AddReactionDialog(
    onDismissRequest: () -> Unit,
    onReaction: (String) -> Unit
) {
    TextFieldDialog(
        onDismissRequest = onDismissRequest,
        title = null,
        button = stringResource(R.string.add),
        requireNotBlank = true,
        placeholder = stringResource(R.string.custom),
        maxLength = 64,
        extraContent = {
            ReactQuickLayout(onReaction = onReaction)
        }
    ) { prompt ->
        prompt.trim().notBlank?.let {
            onReaction(it)
        }
    }
}

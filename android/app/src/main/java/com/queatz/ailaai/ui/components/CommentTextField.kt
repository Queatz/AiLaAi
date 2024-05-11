package com.queatz.ailaai.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R

@Composable
fun CommentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    onDismissRequest: () -> Unit = {},
    onSubmit: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        trailingIcon = {
            Crossfade(targetState = value.isNotBlank()) { show ->
                when (show) {
                    true -> IconButton({ onSubmit() }) {
                        Icon(
                            Icons.AutoMirrored.Default.Send,
                            stringResource(R.string.send),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    false -> {}
                }
            }
        },
        placeholder = {
            placeholder?.let {
                DisableSelection {
                    Text(
                        it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(.5f)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Default
        ),
        keyboardActions = KeyboardActions(
            onSend = { onSubmit() }
        ),
        shape = MaterialTheme.shapes.large,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 128.dp)
            .onKeyEvent { keyEvent ->
                if (value.isEmpty() && keyEvent.key == Key.Backspace) {
                    onDismissRequest()
                    true
                } else {
                    false
                }
            }
    )
}

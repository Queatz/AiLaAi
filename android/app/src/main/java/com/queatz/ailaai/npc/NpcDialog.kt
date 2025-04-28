package com.queatz.ailaai.npc

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.SetPhotoButton
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Npc
import kotlinx.coroutines.launch

@Composable
fun NpcDialog(
    npc: Npc?,
    onDismissRequest: () -> Unit,
    onUpdate: suspend (Npc) -> Unit
) {
    val hasNpc by rememberStateOf(npc != null)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current!!
    var npc by rememberStateOf(npc ?: Npc())
    var isLoading by rememberStateOf(false)

    DialogBase(
        onDismissRequest = onDismissRequest
    ) {
        DialogLayout(
            scrollable = true,
            content = {
                SetPhotoButton(
                    photoText = npc.text.orEmpty(),
                    photo = npc.photo.orEmpty(),
                    aspect = .75,
                    transparentBackground = true,
                    onPhoto = {
                        npc = npc.copy(photo = it)
                    }
                )
                OutlinedTextField(
                    value = npc.name.orEmpty(),
                    onValueChange = { npc = npc.copy(name = it) },
                    label = { Text(stringResource(R.string.name)) },
                    shape = MaterialTheme.shapes.large,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController.hide()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 1.pad)
                )
                OutlinedTextField(
                    value = npc.text.orEmpty(),
                    onValueChange = { npc = npc.copy(text = it) },
                    label = { Text(stringResource(R.string.message)) },
                    shape = MaterialTheme.shapes.large,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController.hide()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 1.pad)
                )
            },
            actions = {
                DialogCloseButton(onDismissRequest)
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            onUpdate(npc)
                            isLoading = false
                        }
                    },
                    enabled = !isLoading && (hasNpc || !npc.name.isNullOrBlank())
                ) {
                    when {
                        !hasNpc -> Text(stringResource(R.string.add_npc))
                        npc.name.isNullOrBlank() -> Text(stringResource(R.string.remove_npc))
                        else -> Text(stringResource(R.string.update))
                    }
                }
            }
        )
    }
}

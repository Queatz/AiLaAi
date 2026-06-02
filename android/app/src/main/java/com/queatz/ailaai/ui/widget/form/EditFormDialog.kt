package com.queatz.ailaai.ui.widget.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.theme.pad
import com.queatz.widgets.widgets.FormData
import com.queatz.widgets.widgets.FormField
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Composable
fun EditFormDialog(
    onDismissRequest: () -> Unit,
    initialFormData: FormData,
    onFormData: suspend (formData: FormData) -> Unit
) {
    val scope = rememberCoroutineScope()
    var formData by rememberStateOf(initialFormData)
    var isLoading by rememberStateOf(false)
    val onAdd = remember {
        MutableSharedFlow<Unit>()
    }

    fun add(formField: FormField) {
        formData = formData.copy(
            fields = formData.fields + formField
        )
        scope.launch {
            onAdd.emit(Unit)
        }
    }

    DialogBase(
        onDismissRequest = onDismissRequest,
        dismissable = false
    ) {
        DialogLayout(
            scrollable = false,
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.form),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                EditFormFields(
                    formFields = formData.fields,
                    onFormFields = {
                        formData = formData.copy(
                            fields = it
                        )
                    },
                    formData = formData,
                    onFormData = {
                        formData = it
                    },
                    onMove = { from: Int, to: Int ->
                        formData = formData.copy(
                            fields = formData.fields.toMutableList().apply {
                                add(to, removeAt(from))
                            }.toList()
                        )
                    },
                    onDrag = { draggedOver, dragging ->
                        draggedOver <= formData.fields.lastIndex && dragging <= formData.fields.lastIndex
                    },
                    onAdd = onAdd,
                    add = ::add,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 1.pad)
                )
            },
            actions = {
                TextButton(
                    onClick = {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            onFormData(formData)
                            isLoading = false
                            onDismissRequest()
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.update))
                }
            }
        )
    }
}

package com.queatz.ailaai.ui.widget.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.replace
import com.queatz.ailaai.extensions.token
import com.queatz.ailaai.ui.components.Check
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.theme.pad
import com.queatz.widgets.widgets.FormData
import com.queatz.widgets.widgets.FormField
import com.queatz.widgets.widgets.FormFieldData
import com.queatz.widgets.widgets.FormFieldType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import org.burnoutcrew.reorderable.ItemPosition
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.util.logging.Logger

@Composable
fun EditFormFields(
    formFields: List<FormField>,
    onFormFields: (List<FormField>) -> Unit,
    formData: FormData,
    onFormData: (FormData) -> Unit,
    modifier: Modifier = Modifier,
    onMove: (from: ItemPosition, to: ItemPosition) -> Unit,
    onDrag: (from: ItemPosition, to: ItemPosition) -> Boolean,
    onAdd: SharedFlow<Unit>,
    add: (FormField) -> Unit,
) {
    val reorderState = rememberReorderableLazyListState(
        onMove = onMove,
        canDragOver = onDrag
    )

    LaunchedEffect(Unit) {
        onAdd.collect {
            delay(100)
            reorderState.listState.animateScrollToItem(
                (reorderState.listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            )
        }
    }

    LazyColumn(
        state = reorderState.listState,
        verticalArrangement = Arrangement.spacedBy(1.pad),
        modifier = modifier
            .reorderable(reorderState)
            .detectReorder(reorderState)
            .detectReorderAfterLongPress(reorderState)
    ) {
        itemsIndexed(items = formFields, key = { _, it -> it.data.key }) { index, formField ->
            ReorderableItem(reorderState, key = formField.data.key) {
                EditFormField(
                    formField = formField,
                    onFormField = {
                        onFormFields(
                            formFields.replace(index, it)
                        )
                    },
                    onDelete = {
                        onFormFields(
                            formFields.toMutableList().apply {
                                removeAt(index)
                            }.toList()
                        )
                    }
                )
            }
        }

        item {
            var showAddMenu by rememberStateOf(false)

            OutlinedButton(
                onClick = {
                    showAddMenu = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.add))

                Dropdown(
                    onDismissRequest = {
                        showAddMenu = false
                    },
                    expanded = showAddMenu
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.text))
                        },
                        onClick = {
                            showAddMenu = false
                            add(
                                FormField(
                                    type = FormFieldType.Text,
                                    data = FormFieldData.Text((1..8).token(), "", "")
                                )
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.input))
                        },
                        onClick = {
                            showAddMenu = false
                            add(
                                FormField(
                                    type = FormFieldType.Input,
                                    data = FormFieldData.Input((1..8).token(), false, "", "", "", "")
                                )
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.checkbox))
                        },
                        onClick = {
                            showAddMenu = false
                            add(
                                FormField(
                                    type = FormFieldType.Checkbox,
                                    data = FormFieldData.Checkbox((1..8).token(), false, "", "", false)
                                )
                            )
                        }
                    )
                }
            }

            EditForm(
                formData = formData,
                onFormData = onFormData
            )
        }
    }
}

@Composable
fun EditFormField(
    formField: FormField,
    onFormField: (FormField) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.pad),
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.pad)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(.5f.pad),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.pad)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = when (formField.type) {
                            FormFieldType.Text -> stringResource(R.string.text)
                            FormFieldType.Input -> stringResource(R.string.input)
                            FormFieldType.Checkbox -> stringResource(R.string.checkbox)
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onDelete
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                when (formField.type) {
                    FormFieldType.Text -> {
                        OutlinedTextField(
                            shape = MaterialTheme.shapes.large,
                            label = {
                                Text(stringResource(R.string.text))
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                            value = (formField.data as FormFieldData.Text).title,
                            onValueChange = {
                                onFormField(
                                    formField.copy(
                                        data = (formField.data as FormFieldData.Text).copy(
                                            title = it
                                        )
                                    )
                                )
                            }
                        )
                    }

                    FormFieldType.Input -> {
                        OutlinedTextField(
                            shape = MaterialTheme.shapes.large,
                            label = {
                                Text(stringResource(R.string.placeholder))
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                            value = (formField.data as FormFieldData.Input).placeholder,
                            onValueChange = {
                                onFormField(
                                    formField.copy(
                                        data = (formField.data as FormFieldData.Input).copy(
                                            placeholder = it
                                        )
                                    )
                                )
                            }
                        )
                        Check(
                            checked = (formField.data as FormFieldData.Input).required,
                            onCheckChange = {
                                onFormField(
                                    formField.copy(
                                        data = (formField.data as FormFieldData.Input).copy(
                                            required = it
                                        )
                                    )
                                )
                            }
                        ) {
                            Text(stringResource(R.string.required))
                        }
                    }

                    FormFieldType.Checkbox -> {
                        OutlinedTextField(
                            shape = MaterialTheme.shapes.large,
                            label = {
                                Text(stringResource(R.string.title))
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                            value = (formField.data as FormFieldData.Checkbox).title,
                            onValueChange = {
                                onFormField(
                                    formField.copy(
                                        data = (formField.data as FormFieldData.Checkbox).copy(
                                            title = it
                                        )
                                    )
                                )
                            }
                        )
                        Check(
                            checked = (formField.data as FormFieldData.Checkbox).initialValue,
                            onCheckChange = {
                                onFormField(
                                    formField.copy(
                                        data = (formField.data as FormFieldData.Checkbox).copy(
                                            initialValue = it
                                        )
                                    )
                                )
                            }
                        ) {
                            Text(stringResource(R.string.inital_value))
                        }
                        Check(
                            checked = (formField.data as FormFieldData.Checkbox).required,
                            onCheckChange = {
                                onFormField(
                                    formField.copy(
                                        data = (formField.data as FormFieldData.Checkbox).copy(
                                            required = it
                                        )
                                    )
                                )
                            }
                        ) {
                            Text(stringResource(R.string.required))
                        }
                    }
                }
            }
        }
    }
}

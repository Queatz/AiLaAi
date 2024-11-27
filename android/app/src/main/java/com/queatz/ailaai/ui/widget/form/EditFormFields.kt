package com.queatz.ailaai.ui.widget.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ailaai.api.group
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.replace
import com.queatz.ailaai.extensions.token
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Group
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
    var shareToGroups by rememberStateOf<List<Group>?>(null)

    LaunchedEffect(formData.groups) {
        shareToGroups = formData.groups?.mapNotNull {
            var group: Group? = null
            api.group(it) {
                group = it.group
            }
            group
        }
    }

    val reorderState = rememberReorderableLazyListState(
        onMove = onMove,
        canDragOver = onDrag
    )

    LaunchedEffect(Unit) {
        onAdd.collect {
            delay(100)
            reorderState.listState.animateScrollToItem(
                (reorderState.listState.layoutInfo.totalItemsCount - 1 - 1).coerceAtLeast(0)
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
                                    data = FormFieldData.Input((1..8).token(), true, "", "", "", "")
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
                                    data = FormFieldData.Checkbox((1..8).token(), false, "", "", "", false)
                                )
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.photo))
                        },
                        onClick = {
                            showAddMenu = false
                            add(
                                FormField(
                                    type = FormFieldType.Photos,
                                    data = FormFieldData.Photos((1..8).token(), true, "", "", listOf())
                                )
                            )
                        }
                    )
                }
            }

            EditForm(
                shareToGroups = shareToGroups.orEmpty(),
                formData = formData,
                onFormData = onFormData
            )
        }
    }
}

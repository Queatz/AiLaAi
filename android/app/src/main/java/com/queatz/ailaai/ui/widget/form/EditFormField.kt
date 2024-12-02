package com.queatz.ailaai.ui.widget.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.Check
import com.queatz.ailaai.ui.theme.pad
import com.queatz.widgets.widgets.FormField
import com.queatz.widgets.widgets.FormFieldData
import com.queatz.widgets.widgets.FormFieldType

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
                            FormFieldType.Photos -> stringResource(R.string.photo)
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
                                Text(stringResource(R.string.title))
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
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                        OutlinedTextField(
                            shape = MaterialTheme.shapes.large,
                            label = {
                                Text(stringResource(R.string.description))
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                            value = (formField.data as FormFieldData.Text).description,
                            onValueChange = {
                                onFormField(
                                    formField.copy(
                                        data = (formField.data as FormFieldData.Text).copy(
                                            description = it
                                        )
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }

                    FormFieldType.Input -> {
                        OutlinedTextField(
                            shape = MaterialTheme.shapes.large,
                            label = {
                                Text(stringResource(R.string.title))
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                            value = (formField.data as FormFieldData.Input).title,
                            onValueChange = {
                                onFormField(
                                    formField.copy(
                                        data = (formField.data as FormFieldData.Input).copy(
                                            title = it
                                        )
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                        OutlinedTextField(
                            shape = MaterialTheme.shapes.large,
                            label = {
                                Text(stringResource(R.string.description))
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                            value = (formField.data as FormFieldData.Input).description,
                            onValueChange = {
                                onFormField(
                                    formField.copy(
                                        data = (formField.data as FormFieldData.Input).copy(
                                            description = it
                                        )
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        )
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
                            },
                            modifier = Modifier
                                .fillMaxWidth()
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
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                        OutlinedTextField(
                            shape = MaterialTheme.shapes.large,
                            label = {
                                Text(stringResource(R.string.description))
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                            value = (formField.data as FormFieldData.Checkbox).description,
                            onValueChange = {
                                onFormField(
                                    formField.copy(
                                        data = (formField.data as FormFieldData.Checkbox).copy(
                                            description = it
                                        )
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                        OutlinedTextField(
                            shape = MaterialTheme.shapes.large,
                            label = {
                                Text(stringResource(R.string.label))
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                            value = (formField.data as FormFieldData.Checkbox).label,
                            onValueChange = {
                                onFormField(
                                    formField.copy(
                                        data = (formField.data as FormFieldData.Checkbox).copy(
                                            label = it
                                        )
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
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
                            Text(stringResource(R.string.initally_checked))
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

                    FormFieldType.Photos -> {
                        OutlinedTextField(
                            shape = MaterialTheme.shapes.large,
                            label = {
                                Text(stringResource(R.string.title))
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                            value = (formField.data as FormFieldData.Photos).title,
                            onValueChange = {
                                onFormField(
                                    formField.copy(
                                        data = (formField.data as FormFieldData.Photos).copy(
                                            title = it
                                        )
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                        OutlinedTextField(
                            shape = MaterialTheme.shapes.large,
                            label = {
                                Text(stringResource(R.string.description))
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                            value = (formField.data as FormFieldData.Photos).description,
                            onValueChange = {
                                onFormField(
                                    formField.copy(
                                        data = (formField.data as FormFieldData.Photos).copy(
                                            description = it
                                        )
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                        Check(
                            checked = (formField.data as FormFieldData.Photos).required,
                            onCheckChange = {
                                onFormField(
                                    formField.copy(
                                        data = (formField.data as FormFieldData.Photos).copy(
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

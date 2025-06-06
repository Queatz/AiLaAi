package app.dialog

import Styles
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.queatz.widgets.widgets.FormData
import com.queatz.widgets.widgets.FormField
import com.queatz.widgets.widgets.FormFieldData
import com.queatz.widgets.widgets.FormFieldType
import com.queatz.widgets.widgets.FormOptions
import components.LabeledCheckbox
import components.Loading
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import app.components.FlexInput
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import r
import token
import web.cssom.PropertyName.Companion.fontWeight

private fun generateRandomKey(): String = (1..8).token()

suspend fun editFormDialog(
    initialFormData: FormData,
    isEdit: Boolean = false,
    onFormData: suspend (FormData) -> Unit
): Boolean? {

    return dialog(
        title = "Form",
        confirmButton = null,
        cancelButton = null
    ) { resolve ->
        var formData by mutableStateOf(initialFormData)
        var isLoading by mutableStateOf(false)
        val scope = rememberCoroutineScope()
        if (isLoading) {
            Loading()
        } else {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(1.r)
                    padding(1.r)
                }
            }) {
                // Display current fields
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        gap(1.r)
                    }
                }) {
                    formData.fields.forEachIndexed { index, field ->
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Column)
                                gap(1.r)
                            }
                        }) {
                            // todo: translate
                            Div({
                                style {
                                    fontWeight("bold")
                                    fontSize(1.2.r)
                                }
                            }) {
                                Text(field.type.toString())
                            }

                            when (field.type) {
                                FormFieldType.Text -> {
                                    val textData = field.data as FormFieldData.Text
                                    Div({
                                        style {
                                            display(DisplayStyle.Flex)
                                            flexDirection(FlexDirection.Column)
                                            gap(1.r)
                                        }
                                    }) {
                                        // Title input
                                        Div({
                                            style {
                                                display(DisplayStyle.Flex)
                                                flexDirection(FlexDirection.Column)
                                                gap(0.5.r)
                                            }
                                        }) {
                                            Text("Title")
                                            FlexInput(
                                                value = textData.title,
                                                placeholder = "Title",
                                                singleLine = true,
                                                onChange = {
                                                    formData = formData.copy(
                                                        fields = formData.fields.toMutableList().apply {
                                                            this[index] = field.copy(
                                                                data = textData.copy(
                                                                    title = it
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                            )
                                        }

                                        // Description input
                                        Div({
                                            style {
                                                display(DisplayStyle.Flex)
                                                flexDirection(FlexDirection.Column)
                                                gap(0.5.r)
                                            }
                                        }) {
                                            Text("Description")
                                            FlexInput(
                                                value = textData.description,
                                                placeholder = "Description",
                                                singleLine = true,
                                                onChange = {
                                                    formData = formData.copy(
                                                        fields = formData.fields.toMutableList().apply {
                                                            this[index] = field.copy(
                                                                data = textData.copy(
                                                                    description = it
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                                FormFieldType.Checkbox -> {
                                    val checkboxData = field.data as FormFieldData.Checkbox
                                    Div({
                                        style {
                                            display(DisplayStyle.Flex)
                                            flexDirection(FlexDirection.Column)
                                            gap(1.r)
                                        }
                                    }) {
                                        // Title input
                                        Div({
                                            style {
                                                display(DisplayStyle.Flex)
                                                flexDirection(FlexDirection.Column)
                                                gap(0.5.r)
                                            }
                                        }) {
                                            Text("Title")
                                            FlexInput(
                                                value = checkboxData.title,
                                                placeholder = "Title",
                                                singleLine = true,
                                                onChange = {
                                                    formData = formData.copy(
                                                        fields = formData.fields.toMutableList().apply {
                                                            this[index] = field.copy(
                                                                data = checkboxData.copy(
                                                                    title = it
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                            )
                                        }

                                        // Description input
                                        Div({
                                            style {
                                                display(DisplayStyle.Flex)
                                                flexDirection(FlexDirection.Column)
                                                gap(0.5.r)
                                            }
                                        }) {
                                            Text("Description")
                                            FlexInput(
                                                value = checkboxData.description,
                                                placeholder = "Description",
                                                singleLine = true,
                                                onChange = {
                                                    formData = formData.copy(
                                                        fields = formData.fields.toMutableList().apply {
                                                            this[index] = field.copy(
                                                                data = checkboxData.copy(
                                                                    description = it
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                            )
                                        }

                                        // Label input
                                        Div({
                                            style {
                                                display(DisplayStyle.Flex)
                                                flexDirection(FlexDirection.Column)
                                                gap(0.5.r)
                                            }
                                        }) {
                                            Text("Label")
                                            FlexInput(
                                                value = checkboxData.label,
                                                placeholder = "Label",
                                                singleLine = true,
                                                onChange = {
                                                    formData = formData.copy(
                                                        fields = formData.fields.toMutableList().apply {
                                                            this[index] = field.copy(
                                                                data = checkboxData.copy(
                                                                    label = it
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                            )
                                        }

                                        // Initially checked option
                                        LabeledCheckbox(
                                            value = checkboxData.initialValue,
                                            onValue = {
                                                formData = formData.copy(
                                                    fields = formData.fields.toMutableList().apply {
                                                        this[index] = field.copy(
                                                            data = checkboxData.copy(
                                                                initialValue = it
                                                            )
                                                        )
                                                    }
                                                )
                                            },
                                            "Initially checked"
                                        )

                                        // Required option
                                        LabeledCheckbox(
                                            value = checkboxData.required,
                                            onValue = {
                                                formData = formData.copy(
                                                    fields = formData.fields.toMutableList().apply {
                                                        this[index] = field.copy(
                                                            data = checkboxData.copy(
                                                                required = it
                                                            )
                                                        )
                                                    }
                                                )
                                            },
                                            "Required"
                                        )
                                    }
                                }
                                FormFieldType.Photos -> {
                                    val photosData = field.data as FormFieldData.Photos
                                    Div({
                                        style {
                                            display(DisplayStyle.Flex)
                                            flexDirection(FlexDirection.Column)
                                            gap(1.r)
                                        }
                                    }) {
                                        // Title input
                                        Div({
                                            style {
                                                display(DisplayStyle.Flex)
                                                flexDirection(FlexDirection.Column)
                                                gap(0.5.r)
                                            }
                                        }) {
                                            Text("Title")
                                            FlexInput(
                                                value = photosData.title,
                                                placeholder = "Title",
                                                singleLine = true,
                                                onChange = {
                                                    formData = formData.copy(
                                                        fields = formData.fields.toMutableList().apply {
                                                            this[index] = field.copy(
                                                                data = photosData.copy(
                                                                    title = it
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                            )
                                        }

                                        // Description input
                                        Div({
                                            style {
                                                display(DisplayStyle.Flex)
                                                flexDirection(FlexDirection.Column)
                                                gap(0.5.r)
                                            }
                                        }) {
                                            Text("Description")
                                            FlexInput(
                                                value = photosData.description,
                                                placeholder = "Description",
                                                singleLine = true,
                                                onChange = {
                                                    formData = formData.copy(
                                                        fields = formData.fields.toMutableList().apply {
                                                            this[index] = field.copy(
                                                                data = photosData.copy(
                                                                    description = it
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                            )
                                        }

                                        // Required option
                                        LabeledCheckbox(
                                            value = photosData.required,
                                            onValue = {
                                                formData = formData.copy(
                                                    fields = formData.fields.toMutableList().apply {
                                                        this[index] = field.copy(
                                                            data = photosData.copy(
                                                                required = it
                                                            )
                                                        )
                                                    }
                                                )
                                            },
                                            "Required"
                                        )
                                    }
                                }
                                FormFieldType.Input -> {
                                    val inputData = field.data as FormFieldData.Input
                                    Div({
                                        style {
                                            display(DisplayStyle.Flex)
                                            flexDirection(FlexDirection.Column)
                                            gap(1.r)
                                        }
                                    }) {
                                        // Title input
                                        Div({
                                            style {
                                                display(DisplayStyle.Flex)
                                                flexDirection(FlexDirection.Column)
                                                gap(0.5.r)
                                            }
                                        }) {
                                            Text("Title")
                                            FlexInput(
                                                value = inputData.title,
                                                placeholder = "Title",
                                                singleLine = true,
                                                onChange = {
                                                    formData = formData.copy(
                                                        fields = formData.fields.toMutableList().apply {
                                                            this[index] = field.copy(
                                                                data = inputData.copy(
                                                                    title = it
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                            )
                                        }

                                        // Description input
                                        Div({
                                            style {
                                                display(DisplayStyle.Flex)
                                                flexDirection(FlexDirection.Column)
                                                gap(0.5.r)
                                            }
                                        }) {
                                            Text("Description")
                                            FlexInput(
                                                value = inputData.description,
                                                placeholder = "Description",
                                                singleLine = true,
                                                onChange = {
                                                    formData = formData.copy(
                                                        fields = formData.fields.toMutableList().apply {
                                                            this[index] = field.copy(
                                                                data = inputData.copy(
                                                                    description = it
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                            )
                                        }

                                        // Placeholder input
                                        Div({
                                            style {
                                                display(DisplayStyle.Flex)
                                                flexDirection(FlexDirection.Column)
                                                gap(0.5.r)
                                            }
                                        }) {
                                            Text("Placeholder")
                                            FlexInput(
                                                value = inputData.placeholder,
                                                placeholder = "Placeholder",
                                                singleLine = true,
                                                onChange = {
                                                    formData = formData.copy(
                                                        fields = formData.fields.toMutableList().apply {
                                                            this[index] = field.copy(
                                                                data = inputData.copy(
                                                                    placeholder = it
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                            )
                                        }

                                        // Initial value input
                                        Div({
                                            style {
                                                display(DisplayStyle.Flex)
                                                flexDirection(FlexDirection.Column)
                                                gap(0.5.r)
                                            }
                                        }) {
                                            Text("Initial Value")
                                            FlexInput(
                                                value = inputData.initialValue,
                                                placeholder = "Initial Value",
                                                singleLine = true,
                                                onChange = {
                                                    formData = formData.copy(
                                                        fields = formData.fields.toMutableList().apply {
                                                            this[index] = field.copy(
                                                                data = inputData.copy(
                                                                    initialValue = it
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                            )
                                        }

                                        // Required option
                                        LabeledCheckbox(
                                            value = inputData.required,
                                            onValue = {
                                                formData = formData.copy(
                                                    fields = formData.fields.toMutableList().apply {
                                                        this[index] = field.copy(
                                                            data = inputData.copy(
                                                                required = it
                                                            )
                                                        )
                                                    }
                                                )
                                            },
                                            "Required"
                                        )
                                    }
                                }
                            }

                            Button({
                                classes(Styles.outlineButton)
                                onClick {
                                    formData = formData.copy(
                                        fields = formData.fields.toMutableList().apply {
                                            removeAt(index)
                                        }
                                    )
                                }
                            }) {
                                Text("Remove")
                            }
                        }
                    }
                }
                // Add field buttons
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Row)
                        gap(1.r)
                    }
                }) {
                    Button({
                        classes(Styles.outlineButton, Styles.outlineButtonTonal)
                        onClick {
                            formData = formData.copy(
                                fields = formData.fields + FormField(
                                    type = FormFieldType.Text,
                                    data = FormFieldData.Text(generateRandomKey(), "", "")
                                )
                            )
                        }
                    }) {
                        Text("Add Text")
                    }

                    Button({
                        classes(Styles.outlineButton, Styles.outlineButtonTonal)
                        onClick {
                            formData = formData.copy(
                                fields = formData.fields + FormField(
                                    type = FormFieldType.Input,
                                    data = FormFieldData.Input(generateRandomKey(), true, "", "", "", "")
                                )
                            )
                        }
                    }) {
                        Text("Add Input")
                    }

                    Button({
                        classes(Styles.outlineButton, Styles.outlineButtonTonal)
                        onClick {
                            formData = formData.copy(
                                fields = formData.fields + FormField(
                                    type = FormFieldType.Checkbox,
                                    data = FormFieldData.Checkbox(generateRandomKey(), false, "", "", "", false)
                                )
                            )
                        }
                    }) {
                        Text("Add Checkbox")
                    }

                    Button({
                        classes(Styles.outlineButton, Styles.outlineButtonTonal)
                        onClick {
                            formData = formData.copy(
                                fields = formData.fields + FormField(
                                    type = FormFieldType.Photos,
                                    data = FormFieldData.Photos(generateRandomKey(), true, "", "", listOf())
                                )
                            )
                        }
                    }) {
                        Text("Add Photos")
                    }
                }

                // Enable anonymous replies option
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Row)
                        gap(1.r)
                    }
                }) {
                    LabeledCheckbox(
                        value = formData.options?.enableAnonymousReplies ?: false,
                        onValue = {
                            formData = formData.copy(
                                options = formData.options?.copy(
                                    enableAnonymousReplies = it
                                ) ?: FormOptions(
                                    enableAnonymousReplies = true
                                )
                            )
                        },
                        "Allow anonymous replies"
                    )
                }
            }
        }

        Button({
            classes(Styles.button)
            onClick {
                scope.launch {
                    isLoading = true
                    onFormData(formData)
                    isLoading = false
                    resolve(true)
                }
            }
        }) {
            Text(if (isEdit) "Update form" else "Create form")
        }
    }
}

package com.queatz.widgets.widgets

import kotlinx.serialization.Serializable

@Serializable
data class FormData(
    val fields: List<FormField> = emptyList(),
    val options: FormOptions? = null,
    val group: String? = null,
    val script: String? = null,
    val submitButtonText: String? = null,
)

@Serializable
data class FormOptions(
    val enableAnonymousReplies: Boolean? = null,
)

@Serializable
data class FormField(
    val type: FormFieldType,
    val data: FormFieldData,
)

@Serializable
sealed class FormFieldData {
    abstract val key: String

    @Serializable
    data class Text(
        override val key: String,
        val title: String,
        val description: String,
    ) : FormFieldData()

    @Serializable
    data class Input(
        override val key: String,
        val required: Boolean,
        val title: String,
        val description: String,
        val placeholder: String,
        val initialValue: String,
    ) : FormFieldData()

    @Serializable
    data class Checkbox(
        override val key: String,
        val required: Boolean,
        val title: String,
        val description: String,
        val initialValue: Boolean,
    ) : FormFieldData()
}

@Serializable
enum class FormFieldType {
    Text,
    Input,
    Checkbox
}

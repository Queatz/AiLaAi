package app.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.uploadPhotos
import app.components.Spacer
import app.dialog.dialog
import app.widget.form.FormFieldCheckbox
import app.widget.form.FormFieldDescription
import app.widget.form.FormFieldInput
import app.widget.form.FormFieldTitle
import appString
import application
import baseUrl
import com.queatz.db.Widget
import com.queatz.widgets.FormValue
import com.queatz.widgets.widgets.FormData
import com.queatz.widgets.widgets.FormFieldData
import com.queatz.widgets.widgets.FormFieldType
import components.Icon
import json
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.AlignSelf
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.alignSelf
import org.jetbrains.compose.web.css.background
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontStyle
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import pickPhotos
import r
import toBytes
import widget

@Composable
fun FormWidget(widgetId: String) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()
    var isLoading by remember {
        mutableStateOf(false)
    }

    var widget by remember(widgetId) {
        mutableStateOf<Widget?>(null)
    }

    var data by remember {
        mutableStateOf<FormData?>(null)
    }

    LaunchedEffect(widgetId) {
        isLoading = true
        api.widget(widgetId) {
            it.data ?: return@widget
            widget = it
            data = try {
                json.decodeFromString<FormData>(it.data!!)
            } catch (e: SerializationException) {
                e.printStackTrace()
                null
            }
        }
        isLoading = false
    }

    data?.let { data ->
        val me by application.me.collectAsState()
        val isEnabled = data.options?.enableAnonymousReplies == true || me != null
        var formValues by remember(data) {
            mutableStateOf(
                buildMap {
                    data.fields.forEach { field ->
                        val fieldData = field.data
                        put(
                            fieldData.key,
                            when (fieldData) {
                                is FormFieldData.Text -> return@forEach
                                is FormFieldData.Checkbox -> {
                                    FormValue(
                                        key = fieldData.key,
                                        title = fieldData.title,
                                        value = JsonPrimitive(fieldData.initialValue)
                                    )
                                }

                                is FormFieldData.Input -> {
                                    FormValue(
                                        key = fieldData.key,
                                        title = fieldData.title,
                                        value = JsonPrimitive(fieldData.initialValue)
                                    )
                                }

                                is FormFieldData.Photos -> {
                                    FormValue(
                                        key = fieldData.key,
                                        title = fieldData.title,
                                        value = JsonArray(emptyList())
                                    )
                                }
                            }
                        )
                    }
                }
            )
        }

        val enableSubmit = remember(formValues) {
            data.fields.all {
                when (val field = it.data) {
                    is FormFieldData.Checkbox -> !field.required || (formValues[field.key]!!.value as JsonPrimitive).boolean
                    is FormFieldData.Input -> !field.required || (formValues[field.key]!!.value as JsonPrimitive).content.isNotBlank()
                    is FormFieldData.Photos -> !field.required || (formValues[field.key]!!.value as JsonArray).isNotEmpty()
                    else -> true
                }
            }
        }

        Div({
            classes(Styles.formContent)
        }) {
            data.fields.forEach { field ->
                when (field.type) {
                    FormFieldType.Text -> {
                        val field = field.data as FormFieldData.Text

                        FormFieldTitle(field.title)
                        FormFieldDescription(field.description)
                    }

                    FormFieldType.Input -> {
                        val field = field.data as FormFieldData.Input

                        FormFieldTitle(field.title, required = field.required)
                        FormFieldDescription(field.description)
                        Spacer()
                        FormFieldInput(
                            value = (formValues[field.key]!!.value as JsonPrimitive).content,
                            onValue = {
                                formValues = formValues.toMutableMap().apply {
                                    put(field.key, get(field.key)!!.copy(value = JsonPrimitive(it)))
                                }.toMap()
                            },
                            placeholder = field.placeholder,
                            isEnabled = isEnabled
                        )
                    }

                    FormFieldType.Checkbox -> {
                        val field = field.data as FormFieldData.Checkbox

                        FormFieldTitle(field.title, required = field.required)
                        FormFieldDescription(field.description)
                        Spacer()
                        FormFieldCheckbox(
                            checked = (formValues[field.key]!!.value as JsonPrimitive).boolean,
                            onChecked = {
                                formValues = formValues.toMutableMap().apply {
                                    put(field.key, get(field.key)!!.copy(value = JsonPrimitive(it)))
                                }.toMap()
                            },
                            label = field.label,
                            isEnabled = isEnabled
                        )
                    }

                    FormFieldType.Photos -> {
                        val field = field.data as FormFieldData.Photos

                        FormFieldTitle(field.title, required = field.required)
                        FormFieldDescription(field.description)
                        Spacer()
                        (formValues[field.key]!!.value as JsonArray).let { value ->
                            if (value.isNotEmpty()) {
                                Div({
                                    style {
                                        marginBottom(1.r)
                                        gap(1.r)
                                        display(DisplayStyle.Flex)
                                        flexWrap(FlexWrap.Wrap)
                                    }
                                }) {
                                    (formValues[field.key]!!.value as JsonArray).forEachIndexed { index, it ->
                                        Img(src = "$baseUrl${(it as JsonPrimitive).content}", attrs = {
                                            style {
                                                height(12.r)
                                                borderRadius(1.r)
                                                cursor("pointer")
                                            }

                                            // todo: translate
                                            title("Tap to remove")

                                            onClick {
                                                scope.launch {
                                                    val result = dialog(
                                                        // todo:translate
                                                        title = "Remove this photo?"
                                                    )

                                                    if (result == true) {
                                                        formValues = formValues.toMutableMap().apply {
                                                            put(
                                                                field.key, get(field.key)!!.copy(
                                                                    value = JsonArray(
                                                                        (formValues[field.key]!!.value as JsonArray).toMutableList()
                                                                            .apply {
                                                                                removeAt(index)
                                                                            }.toList()
                                                                    )
                                                                )
                                                            )
                                                        }.toMap()
                                                    }
                                                }
                                            }
                                        })
                                    }
                                }
                            }
                        }
                        Button({
                            classes(Styles.outlineButton)

                            style {
                                alignSelf(AlignSelf.FlexStart)
                            }

                            if (!isEnabled) {
                                disabled()
                            }

                            onClick {
                                pickPhotos {
                                    scope.launch {
                                        api.uploadPhotos(
                                            it.map { it.toBytes() }
                                        ) { urls ->
                                            formValues = formValues.toMutableMap().apply {
                                                put(
                                                    key = field.key,
                                                    value = get(field.key)!!.copy(
                                                        value = JsonArray(
                                                            (formValues[field.key]!!.value as JsonArray).toList() + urls.urls.map {
                                                                JsonPrimitive(it)
                                                            }
                                                        )
                                                    )
                                                )
                                            }.toMap()
                                        }
                                    }
                                }
                            }
                        }) {
                            // todo: translate
                            Text("Add photo(s)")
                        }
                    }
                }
            }

            Button({
                classes(Styles.button)

                style {
                    marginTop(1.r)
                    alignSelf(AlignSelf.FlexStart)
                }

                if (!isEnabled || !enableSubmit) {
                    disabled()
                }
            }) {
                Text(data.submitButtonText ?: appString { submit })
            }

            if (!isEnabled) {
                Div({
                    style {
                        opacity(.8f)
                        fontStyle("italic")
                    }
                }) {
                    Text(
                        // todo: translate
                        "Sign in to submit this form"
                    )
                }
            }
        }
    }
}

package app.components

import Styles
import androidx.compose.runtime.*
import appString
import appText
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.autoFocus
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.events.Event
import r
import resize

@Composable
fun EditField(
    value: String = "",
    placeholder: String = "",
    selectAll: Boolean = false,
    styles: StyleScope.() -> Unit = {},
    buttonBarStyles: StyleScope.() -> Unit = {},
    enabled: Boolean = true,
    showDiscard: Boolean = true,
    autoFocus: Boolean = false,
    resetOnSubmit: Boolean = false,
    button: String = appString { save },
    onSave: suspend (String) -> Boolean
) {
    val scope = rememberCoroutineScope()

    var messageText by remember(value) {
        mutableStateOf(value)
    }

    var messageChanged by remember(value) { mutableStateOf(false) }

    var isSaving by remember(value) { mutableStateOf(false) }
    var onValueChange by remember { mutableStateOf({}) }

    LaunchedEffect(messageText) {
        onValueChange()
    }

    fun save() {
        scope.launch {
            isSaving = true

            val result = onSave(messageText)

            if (result) {
                messageChanged = false
                if (resetOnSubmit) {
                    messageText = value
                }
            }

            isSaving = false
        }
    }

    TextArea(messageText) {
        classes(Styles.textarea)
        style {
            height(3.5.r)
            maxHeight(18.r)
            flexShrink(0)
            backgroundColor(Color.transparent)
            styles()
        }

        placeholder(placeholder)

        if (!enabled) {
            disabled()
        }

        onKeyDown {
            if (it.key == "Enter" && it.ctrlKey) {
                it.preventDefault()
                it.stopPropagation()
                save()
            }
        }

        onInput {
            messageText = it.value
            it.target.resize()
            messageChanged = true
        }

        onChange {
            it.target.resize()
        }

        if (autoFocus) {
            autoFocus()
        }

        ref { element ->
            if (autoFocus) {
                element.focus()
            }

            element.resize()

            onValueChange = { element.dispatchEvent(Event("change")) }

            if (selectAll) {
                element.select()
            }

            onDispose {
                onValueChange = {}
            }
        }
    }

    if (messageChanged && messageText != value) {
        Div({
            style {
                margin(.5.r)
                flexShrink(0)
                display(DisplayStyle.Flex)
                buttonBarStyles()
            }
        }) {
            Button({
                classes(Styles.button)

                if (showDiscard) {
                    style {
                        marginRight(.5.r)
                    }
                }

                onClick {
                    save()
                }

                if (isSaving) {
                    disabled()
                }
            }) {
                Text(button)
            }

            if (showDiscard) {
                Button({
                    classes(Styles.outlineButton)
                    style {
                        marginRight(.5.r)
                    }
                    onClick {
                        messageText = value
                        messageChanged = false
                    }

                    if (isSaving) {
                        disabled()
                    }
                }) {
                    appText { discard }
                }
            }
        }
    }
}

package app.components

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import appString
import appText
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.autoFocus
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.fontFamily
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.maxHeight
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
    autoSize: Boolean = true,
    monospace: Boolean = false,
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
            if (autoSize) {
                maxHeight(18.r)
            }
            flexShrink(0)
            backgroundColor(Color.transparent)
            styles()

            if (monospace) {
                fontFamily("monospace")
            }
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
            if (autoSize) {
                it.target.resize()
            }
            messageChanged = true
        }

        onChange {
            if (autoSize) {
                it.target.resize()
            }
        }

        if (autoFocus) {
            autoFocus()
        }

        ref { element ->
            if (autoFocus) {
                element.focus()
            }

            if (autoSize) {
                element.resize()
            }

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

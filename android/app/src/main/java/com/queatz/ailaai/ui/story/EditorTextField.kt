package com.queatz.ailaai.ui.story

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditorTextField(
    initialValue: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onFocus: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    textStyle: Typography.() -> TextStyle = { bodyMedium },
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(initialValue))
    }

    BasicEditorTextField(
        value = textFieldValue,
        onValueChange = {
            if (it.text != initialValue) {
                onValueChange(it.text)
            }
            textFieldValue = it
        },
        placeholder = {
            Text(
                placeholder,
                style = textStyle(MaterialTheme.typography),
                modifier = Modifier.alpha(0.5f)
            )
        },
        singleLine = singleLine,
        keyboardOptions = if (onNext == null) KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences
        ) else KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next
        ),
        keyboardActions = if (onNext == null) KeyboardActions.Default else KeyboardActions(
            onNext = {
                onNext()
            }
        ),
        textStyle = textStyle(MaterialTheme.typography),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (it.hasFocus) {
                    onFocus?.invoke()
                }
            }
            .focusProperties {
                val atStart = textFieldValue.selection.length == 0 && textFieldValue.selection.start == 0
                val atEnd = textFieldValue.selection.length == 0 && textFieldValue.selection.start == textFieldValue.text.length
                up = if (atStart) FocusRequester.Default else FocusRequester.Cancel
                down = if (atEnd) FocusRequester.Default else FocusRequester.Cancel
                left = if (atStart) FocusRequester.Default else FocusRequester.Cancel
                right = if (atEnd) FocusRequester.Default else FocusRequester.Cancel
            }
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown) {
                    when (it.key) {
                        Key.Backspace -> {
                            if (initialValue.isEmpty()) {
                                onDelete?.invoke()
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    )
}

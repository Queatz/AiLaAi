package com.queatz.ailaai.ui.story

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditorTextField(
    value: String,
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
    BasicEditorTextField(
        value,
        onValueChange,
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
            .onKeyEvent {
                if (value.isEmpty() && it.key == Key.Backspace) {
                    onDelete?.invoke()
                    true
                } else {
                    false
                }
            }
    )
}

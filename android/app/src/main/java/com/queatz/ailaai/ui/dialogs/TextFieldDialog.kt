package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.theme.pad
import kotlinx.coroutines.launch

@Composable
fun TextFieldDialog(
    onDismissRequest: () -> Unit,
    title: String?,
    button: String,
    singleLine: Boolean = false,
    initialValue: String = "",
    placeholder: String = "",
    showDismiss: Boolean = false,
    dismissButtonText: String? = null,
    requireModification: Boolean = true,
    requireNotBlank: Boolean = false,
    valueFormatter: ((String) -> String?)? = null,
    keyboardOptions: KeyboardOptions? = null,
    align: TextAlign = TextAlign.Start,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null,
    bottomContent: (@Composable ColumnScope.() -> Unit)? = null,
    onSubmit: suspend (value: String) -> Unit,
) {
    var disableSubmit by remember { mutableStateOf(requireModification) }
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DialogBase(onDismissRequest, modifier = Modifier.wrapContentHeight()) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(3.pad)
                .verticalScroll(scrollState)
        ) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 1.pad)
                )
            }
            extraContent?.invoke(this)
            OutlinedTextField(
                text,
                onValueChange = {
                    if (valueFormatter == null) {
                        text = it
                    } else {
                        valueFormatter(it)?.let {
                            text = it
                        }
                    }
                    if (requireModification || requireNotBlank) {
                        disableSubmit = if (requireNotBlank) it.isBlank() else false
                    }
                },
                shape = MaterialTheme.shapes.large,
                singleLine = singleLine,
                keyboardOptions = keyboardOptions ?: KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                placeholder = { Text(placeholder, modifier = Modifier.alpha(0.5f)) },
                textStyle = LocalTextStyle.current.copy(textAlign = align),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 1.pad)
                    .focusRequester(focusRequester)
            )
            bottomContent?.invoke(this)
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showDismiss) {
                    TextButton({
                        onDismissRequest()
                    }) {
                        Text(dismissButtonText ?: stringResource(R.string.close), color = MaterialTheme.colorScheme.secondary)
                    }
                }
                TextButton(
                    {
                        disableSubmit = true

                        coroutineScope.launch {
                            try {
                                onSubmit(text)
                            } finally {
                                disableSubmit = false
                            }
                        }
                    },
                    enabled = !disableSubmit
                ) {
                    Text(button)
                }
            }
        }
    }
}


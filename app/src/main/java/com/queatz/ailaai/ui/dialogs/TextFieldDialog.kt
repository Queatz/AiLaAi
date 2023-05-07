package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@Composable
fun TextFieldDialog(
    onDismissRequest: () -> Unit,
    title: String,
    button: String,
    singleLine: Boolean = false,
    initialValue: String = "",
    onSubmit: suspend (value: String) -> Unit,
) {
    var disableSubmit by remember { mutableStateOf(true) }
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
                .padding(PaddingDefault * 3)
                .verticalScroll(scrollState)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = PaddingDefault)
            )
            OutlinedTextField(
                text,
                onValueChange = {
                    text = it
                    disableSubmit = false
                },
                shape = MaterialTheme.shapes.large,
                singleLine = singleLine,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PaddingDefault)
                    .focusRequester(focusRequester)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
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
                    enabled = !disableSubmit,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(button, textAlign = TextAlign.End)
                }
            }
        }
    }
}


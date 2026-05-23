package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import aiJson
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.AiJsonRequest
import com.queatz.db.Signal
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*

@Composable
fun CreateSignalDialog(
    onDismissRequest: () -> Unit,
    onSubmit: (Signal) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }
    var isEmojiSuggested by remember { mutableStateOf(false) }
    var isEmojiLoading by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(name) {
        if (name.isBlank()) {
            isEmojiLoading = false
            return@LaunchedEffect
        }

        delay(500)

        if (emoji.isNotBlank() && !isEmojiSuggested) {
            return@LaunchedEffect
        }

        isEmojiLoading = true
        api.aiJson(
            AiJsonRequest(
                prompt = "What is the best single emoji for '$name'? Return only the emoji in a json field named 'emoji'.",
                schema = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("emoji") {
                            put("type", "string")
                        }
                    }
                    putJsonArray("required") {
                        add("emoji")
                    }
                    put("additionalProperties", false)
                }
            ),
            onSuccess = { response ->
                isEmojiLoading = false
                val suggestedEmoji = api.httpJson.decodeFromString<JsonObject>(response.json)["emoji"]?.jsonPrimitive?.content
                if (suggestedEmoji != null && (emoji.isBlank() || isEmojiSuggested)) {
                    emoji = suggestedEmoji
                    isEmojiSuggested = true
                }
            },
            onError = {
                isEmojiLoading = false
                it.printStackTrace()
                // Ignore errors for auto-suggestion
            }
        )
    }

    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(3.pad)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.create_signal),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 2.pad)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 1.pad)
                    .focusRequester(focusRequester)
            )

            OutlinedTextField(
                value = emoji,
                onValueChange = {
                    emoji = it
                    isEmojiSuggested = false
                },
                label = { Text("Emoji") },
                shape = MaterialTheme.shapes.large,
                trailingIcon = {
                    if (isEmojiLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 1.pad)
            )

            OutlinedTextField(
                value = categories,
                onValueChange = { categories = it },
                label = { Text("Categories") },
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().padding(bottom = 1.pad)
            )

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(top = 2.pad)
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onSubmit(
                            Signal(
                                name = name,
                                emoji = emoji,
                                categories = categories.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            )
                        )
                    },
                    enabled = name.isNotBlank() && emoji.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

package com.queatz.ailaai.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import app.ailaai.api.groupTopReactions
import app.ailaai.api.myTopReactions
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.ui.components.groupTopReactionsCache
import com.queatz.ailaai.ui.theme.pad

private var myTopReactionsCache = emptyList<String>()
private val groupTopReactionsCache = mutableMapOf<String, List<String>>()

@Composable
fun ReactLayout(
    modifier: Modifier = Modifier,
    group: String? = null,
    maxLength: Int = 64,
    placeholder: String = stringResource(R.string.react),
    onReaction: (String) -> Unit
) {
    var value by remember { mutableStateOf("") }

    var myTopReactions by remember { mutableStateOf(myTopReactionsCache) }
    var topGroupReactions by remember(group) {
        mutableStateOf(
            group?.let { groupTopReactionsCache[it] } ?: emptyList<String>()
        )
    }

    LaunchedEffect(Unit) {
        if (myTopReactionsCache.isEmpty()) {
            api.myTopReactions {
                myTopReactions = it.take(5).map { it.reaction }
                myTopReactionsCache = myTopReactions
            }
        }
    }

    LaunchedEffect(group) {
        topGroupReactions = emptyList()
        group?.let { group ->
            api.groupTopReactions(group) {
                topGroupReactions = it.take(5).map { it.reaction }
                groupTopReactionsCache[group] = topGroupReactions
            }
        }
    }

    fun send() {
        value.trim().notBlank?.let {
            onReaction(it)
        }
    }

    Column(
        modifier = modifier
    ) {
        ReactQuickLayout(
            quickReactions = topGroupReactions + myTopReactions,
            onReaction = onReaction
        )

        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it.take(maxLength)
            },
            shape = MaterialTheme.shapes.large,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    send()
                }
            ),
            trailingIcon = {
                Crossfade(targetState = value.isNotBlank()) { show ->
                    when (show) {
                        true -> IconButton(
                            onClick = {
                                send()
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Default.Send,
                                Icons.AutoMirrored.Default.Send.name,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        false -> Unit
                    }
                }
            },
            placeholder = { Text(placeholder, modifier = Modifier.alpha(0.5f)) },
            textStyle = LocalTextStyle.current,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 1.pad)
        )
    }
}

package com.queatz.ailaai.ui.components

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.Card
import com.queatz.ailaai.LinkifyText
import com.queatz.ailaai.R
import com.queatz.ailaai.json
import com.queatz.ailaai.ui.theme.PaddingDefault

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CardConversation(
    card: Card,
    modifier: Modifier = Modifier,
    interactable: Boolean = true,
    onReply: (List<String>) -> Unit = {},
    isMine: Boolean = false,
    isMineToolbar: Boolean = true,
    showTitle: Boolean = true,
    selectingText: ((Boolean) -> Unit)? = null,
    conversationChange: ((List<ConversationItem>) -> Unit)? = null,
    onCategoryClick: ((String) -> Unit)? = null,
    onSetCategoryClick: (() -> Unit)? = null
) {
    val categories = card.categories ?: emptyList()
    val conversation = remember(card.conversation) {
        json.decodeFromString<ConversationItem>(card.conversation ?: "{}")
    }
    var current by remember(conversation) { mutableStateOf(conversation) }
    var stack by remember(conversation) { mutableStateOf(emptyList<ConversationItem>()) }

    LaunchedEffect(stack) {
        conversationChange?.invoke(stack)
    }

    Column(modifier = modifier) {
        if (showTitle || stack.isNotEmpty()) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        MaterialTheme.typography.titleMedium.toSpanStyle().copy(fontWeight = FontWeight.Bold)
                    ) {
                        append(current.title.takeIf { stack.isNotEmpty() } ?: card.name ?: "")
                    }

                    append("  ")

                    withStyle(
                        MaterialTheme.typography.titleSmall.toSpanStyle()
                            .copy(color = MaterialTheme.colorScheme.secondary)
                    ) {
                        append(card.name?.takeIf { stack.size == 1 } ?: stack.lastOrNull()?.title ?: card.location ?: "")
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier
                    .fillMaxWidth()
                   .padding(bottom = if (stack.isNotEmpty() || (categories.isEmpty() && !(isMine && isMineToolbar))) PaddingDefault / 2 else 0.dp)
            )
        }

        if (categories.isNotEmpty()) {
            AnimatedVisibility(stack.isEmpty()) {
                categories.forEach { category ->
                    SuggestionChip(
                        onClick = {
                            onCategoryClick?.invoke(category)
                        },
                        shape = MaterialTheme.shapes.medium,
                        label = {
                            Text(category)
                        }
                    )
                }
            }
        } else if (isMine && isMineToolbar) {
            AnimatedVisibility(stack.isEmpty()) {
                AssistChip(
                    onClick = {
                        onSetCategoryClick?.invoke()
                    },
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = {
                        Icon(Icons.Outlined.Edit, null)
                    },
                    label = {
                        Text(stringResource(R.string.set_category))
                    }
                )
            }
        }

        if (current.message.isNotBlank()) {
            SelectionContainer(
                modifier = Modifier
                    .motionEventSpy {
                        if (it.action == MotionEvent.ACTION_UP || it.action == MotionEvent.ACTION_CANCEL) {
                            selectingText?.invoke(false)
                        } else if (it.action != MotionEvent.ACTION_MOVE && it.action != MotionEvent.ACTION_DOWN) {
                            selectingText?.invoke(false)
                        }
                    }
                    .pointerInteropFilter {
                        if (it.action == MotionEvent.ACTION_DOWN) {
                            selectingText?.invoke(true)
                        }
                        false
                    }
            ) {
                LinkifyText(
                    text = current.message,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = PaddingDefault * 2)
                )
            }
        }

        if (interactable) {
            current.items.forEach {
                Button({
                    when (it.action) {
                        ConversationAction.Message -> {
                            onReply(stack.map { it.title } + current.title)
                        }
                        else -> {
                            stack = stack + current
                            current = it
                        }
                    }
                }) {//, enabled = it.action == null || !isMine
                    if (it.action == ConversationAction.Message) {
                        Icon(Icons.Outlined.Message, "", modifier = Modifier.padding(end = PaddingDefault))
                    }
                    Text(it.title, overflow = TextOverflow.Ellipsis, maxLines = 1)
                }
            }

            if (current.items.isEmpty()) {
                Button({
                    onReply(stack.map { it.title } + current.title)
                }, enabled = !isMine) {
                    Icon(Icons.Outlined.Message, "", modifier = Modifier.padding(end = PaddingDefault))
                    Text(
                        stringResource(R.string.message),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }

            AnimatedVisibility(
                stack.isNotEmpty(),
                modifier = Modifier.align(Alignment.Start)
            ) {
                TextButton({
                    if (stack.isNotEmpty()) {
                        current = stack.last()
                        stack = stack.dropLast(1)
                    }
                }) {
                    Icon(Icons.Outlined.ArrowBack, stringResource(R.string.go_back))
                    Text(stringResource(R.string.go_back), modifier = Modifier.padding(start = PaddingDefault))
                }
            }
        }
    }
}

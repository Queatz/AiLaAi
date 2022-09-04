package com.queatz.ailaai.ui.components

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.outlined.ArrowBack
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
import com.queatz.ailaai.Card
import com.queatz.ailaai.LinkifyText
import com.queatz.ailaai.R
import com.queatz.ailaai.gson
import com.queatz.ailaai.ui.theme.PaddingDefault

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CardConversation(
    card: Card,
    modifier: Modifier = Modifier,
    interactable: Boolean = true,
    onReply: () -> Unit = {},
    isMine: Boolean = false,
    showTitle: Boolean = true,
    selectingText: ((Boolean) -> Unit)? = null
) {
    val conversation = gson.fromJson(card.conversation ?: "{}", ConversationItem::class.java)
    var current by remember { mutableStateOf(conversation) }
    val stack = remember { mutableListOf<ConversationItem>() }

    Column(modifier = modifier) {
        if (showTitle) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        MaterialTheme.typography.titleMedium.toSpanStyle().copy(fontWeight = FontWeight.Bold)
                    ) {
                        append(card.name ?: stringResource(R.string.someone))
                    }

                    append("  ")

                    withStyle(
                        MaterialTheme.typography.titleSmall.toSpanStyle()
                            .copy(color = MaterialTheme.colorScheme.secondary)
                    ) {
                        append(card.location ?: "")
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PaddingDefault)
            )
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
                    stack.add(current)
                    current = it
                }) {
                    Text(it.title, overflow = TextOverflow.Ellipsis, maxLines = 1)
                }
            }

            if (current.items.isEmpty()) {
                Button({
                    onReply()
                }, enabled = !isMine) {
                    Icon(Icons.Filled.MailOutline, "", modifier = Modifier.padding(end = PaddingDefault))
                    Text(
                        stringResource(R.string.reply),
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
                        current = stack.removeLast()
                    }
                }) {
                    Icon(Icons.Outlined.ArrowBack, stringResource(R.string.go_back))
                    Text(stringResource(R.string.go_back), modifier = Modifier.padding(start = PaddingDefault))
                }
            }
        }
    }
}

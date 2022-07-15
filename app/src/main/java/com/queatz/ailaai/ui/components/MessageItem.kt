package com.queatz.ailaai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat.getSystemService
import coil.compose.AsyncImage
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(message: Message, getPerson: (String) -> Person?, isMe: Boolean, onDeleted: () -> Unit) {
    var showMessageDialog by remember { mutableStateOf(false) }
    var showDeleteMessageDialog by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (showDeleteMessageDialog) {
        var disableSubmit by remember { mutableStateOf(false) }
        AlertDialog(
            {
                showDeleteMessageDialog = false
            },
            title = {
                Text(stringResource(R.string.delete_this_message))
            },
            text = {
                Text(stringResource(R.string.delete_message_description))
            },
            confirmButton = {
                TextButton(
                    {
                        coroutineScope.launch {
                            try {
                                disableSubmit = true
                                api.deleteMessage(message.id!!)
                                onDeleted()
                                showDeleteMessageDialog = false
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            } finally {
                                disableSubmit = false
                            }
                        }
                    },
                    enabled = !disableSubmit,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton({
                    showDeleteMessageDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showMessageDialog) {
        Dialog({
            showMessageDialog = false
        }) {
            Surface(
                shape = MaterialTheme.shapes.large
            ) {
                Column {
                    val messageString = stringResource(R.string.message)
                    DropdownMenuItem({ Text(stringResource(R.string.copy)) }, {
                        getSystemService(context, ClipboardManager::class.java)?.setPrimaryClip(
                            ClipData.newPlainText(messageString, message.text ?: "")
                        )
                        showMessageDialog = false
                    })

                    if (isMe) {
                        DropdownMenuItem({ Text(stringResource(R.string.delete)) }, {
                            showDeleteMessageDialog = true
                            showMessageDialog = false
                        })
                    }
                }
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        if (!isMe) ProfileImage(
            getPerson(message.member!!),
            PaddingValues(PaddingDefault, PaddingDefault, 0.dp, PaddingDefault)
        )

        Column(modifier = Modifier.weight(1f)) {
            LinkifyText(
                message.text ?: "",
                linkColor = MaterialTheme.colorScheme.primary,
                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(if (isMe) Alignment.End else Alignment.Start)
                    .padding(PaddingDefault)
                    .let {
                        when (isMe) {
                            true -> it.padding(PaddingValues(start = PaddingDefault * 8))
                            false -> it.padding(PaddingValues(end = PaddingDefault * 8))
                        }
                    }
                    .clip(MaterialTheme.shapes.large)
                    .background(if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
                    .border(
                        if (isMe) 0.dp else 1.dp,
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.shapes.large
                    )
                    .combinedClickable(
                        onClick = { showTime = !showTime },
                        onLongClick = { showMessageDialog = true }
                    )
                    .padding(PaddingDefault * 2, PaddingDefault)
            )
            AnimatedVisibility(showTime) {
                Text(
                    message.createdAt!!.timeAgo(),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = if (isMe) TextAlign.End else TextAlign.Start,
                    modifier = Modifier
                        .padding(horizontal = PaddingDefault)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ProfileImage(person: Person?, padding: PaddingValues) {
    AsyncImage(
        model = person?.photo?.let { api.url(it) } ?: "",
        contentDescription = "",
        contentScale = ContentScale.Crop,
        alignment = Alignment.Center,
        modifier = Modifier
            .padding(padding)
            .requiredSize(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
    )
}

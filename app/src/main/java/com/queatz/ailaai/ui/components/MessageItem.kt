package com.queatz.ailaai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.queatz.ailaai.Message
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlin.random.Random

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(message: Message, isMe: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        var showMessageDialog by remember { mutableStateOf(false) }
        var showTime by remember { mutableStateOf(false) }

        if (!isMe) ProfileImage(PaddingValues(PaddingDefault, PaddingDefault, 0.dp, PaddingDefault))

        if (showMessageDialog) {
            Dialog({
                showMessageDialog = false
            }) {
                Surface(
                    shape = MaterialTheme.shapes.large
                ) {
                    Column {
                        DropdownMenuItem({ Text("Copy") }, { showMessageDialog = false })
                        DropdownMenuItem({ Text("Delete") }, { showMessageDialog = false })
                        DropdownMenuItem({ Text("Report") }, { showMessageDialog = false })
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                message.text ?: "",
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
                        onClick =  { showTime = !showTime },
                        onLongClick = { showMessageDialog = true }
                    )
                    .padding(PaddingDefault * 2, PaddingDefault)
            )
            AnimatedVisibility(showTime) {
                Text(message.createdAt!!.timeAgo(),
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
fun ProfileImage(padding: PaddingValues) {
    AsyncImage(
        model = "https://minimaltoolkit.com/images/randomdata/${
            listOf("female", "male").random()
        }/${Random.nextInt(1, 100)}.jpg",
        "",
        Modifier
            .padding(padding)
            .requiredSize(32.dp)
            .clip(CircleShape)
    )
}

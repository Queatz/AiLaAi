package com.queatz.ailaai.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import at.bluesource.choicesdk.maps.common.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.Card
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.gson
import com.queatz.ailaai.ui.dialogs.DeleteCardDialog
import com.queatz.ailaai.ui.dialogs.EditCardDialog
import com.queatz.ailaai.ui.dialogs.EditCardLocationDialog
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun BasicCard(
    onClick: () -> Unit,
    onReply: () -> Unit = {},
    onChange: () -> Unit = {},
    activity: Activity,
    card: Card,
    edit: Boolean = false,
    isMine: Boolean = false,
    isChoosing: Boolean = false
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        var hideContent by remember { mutableStateOf(false) }
        val alpha by animateFloatAsState(if (!hideContent) 1f else 0f, tween())
        val scale by animateFloatAsState(if (!hideContent) 1f else 1.125f, tween(DefaultDurationMillis * 2))

        LaunchedEffect(hideContent) {
            if (hideContent) {
                delay(2.seconds)
                hideContent = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(.75f)
                .combinedClickable(
                    onClick = {
                        onClick()
                    },
                    onLongClick = {
                        hideContent = true
                    }
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            card.photo?.also {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(api.url(it))
                        .crossfade(true)
                        .build(),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier.matchParentSize().scale(scale)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(PaddingDefault)
                    .align(Alignment.TopEnd)
            ) {
                if (isMine) {
                    val coroutineScope = rememberCoroutineScope()
                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                        if (it == null) return@rememberLauncherForActivityResult

                        coroutineScope.launch {
                            try {
                                api.uploadCardPhoto(card.id!!, it)
                                onChange()
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    }
                    IconButton({
                        launcher.launch("image/*")
                    }, modifier = Modifier) {
                        Icon(Icons.Outlined.Edit, "")
                    }
                }

                if ((card.cardCount ?: 0) > 0) {
                    Text(
                        pluralStringResource(R.plurals.number_of_cards, card.cardCount ?: 0, card.cardCount ?: 0),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.background.copy(alpha = .8f),
                                MaterialTheme.shapes.extraLarge
                            )
                            .padding(vertical = PaddingDefault, horizontal = PaddingDefault * 2)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .alpha(alpha)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = .8f))
                    .padding(PaddingDefault * 2)
            ) {
                val conversation = gson.fromJson(card.conversation ?: "{}", ConversationItem::class.java)
                var current by remember { mutableStateOf(conversation) }
                val stack = remember { mutableListOf<ConversationItem>() }

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

                Text(
                    text = current.message,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = PaddingDefault * 2)
                )

                if (!isChoosing) {
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

                    if (isMine) {
                        CardToolbar(activity, onChange, card, edit)
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission", "UnrememberedMutableState")
@Composable
private fun ColumnScope.CardToolbar(activity: Activity, onChange: () -> Unit, card: Card, edit: Boolean) {
    var openDeleteDialog by remember { mutableStateOf(false) }
    var openEditDialog by remember { mutableStateOf(false) }
    var openLocationDialog by remember { mutableStateOf(edit) }

    Row(
        modifier = Modifier
            .background(Color.Transparent)
            .align(Alignment.End)
            .padding(PaddingValues(top = PaddingDefault))
    ) {
        var active by remember { mutableStateOf(card.active ?: false) }
        var activeCommitted by remember { mutableStateOf(active) }
        val coroutineScope = rememberCoroutineScope()

        Switch(active, {
            active = it
            coroutineScope.launch {
                try {
                    val update = api.updateCard(card.id!!, Card(active = active))
                    card.active = update.active
                    activeCommitted = update.active ?: false
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        })
        Text(
            if (activeCommitted) stringResource(R.string.card_active) else stringResource(R.string.card_inactive),
            style = MaterialTheme.typography.labelMedium,
            color = if (activeCommitted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = PaddingDefault)
        )
        Box(modifier = Modifier.weight(1f))
        IconButton({
            openLocationDialog = true
        }) {
            Icon(Icons.Outlined.Place, "")
        }
        IconButton({
            openEditDialog = true
        }) {
            Icon(Icons.Outlined.Edit, "")
        }
        IconButton({
            openDeleteDialog = true
        }) {
            Icon(Icons.Outlined.Delete, "", tint = MaterialTheme.colorScheme.error)
        }
    }

    if (openLocationDialog) {
        EditCardLocationDialog(card, activity, {
            openLocationDialog = false
        }, onChange)
    }

    if (openEditDialog) {
        EditCardDialog(card, {
            openEditDialog = false
        }, onChange)
    }

    if (openDeleteDialog) {
        DeleteCardDialog(card, {
            openDeleteDialog = false
        }, onChange)
    }
}

data class ConversationItem(
    var title: String = "",
    var message: String = "",
    var items: MutableList<ConversationItem> = mutableListOf()
)

fun LatLng.toList() = listOf(latitude, longitude)

enum class CardParentType {
    Map,
    Card,
    Person
}

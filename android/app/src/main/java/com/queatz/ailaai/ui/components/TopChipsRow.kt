package com.queatz.ailaai.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.ailaai.api.sendMessage
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.rememberAnimatedGradientBrush
import com.queatz.ailaai.extensions.shortAgo
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.me
import com.queatz.ailaai.services.chips
import com.queatz.ailaai.services.push
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.ChipAttachment
import com.queatz.db.ChipFormatType
import com.queatz.db.ChipValueKind
import com.queatz.db.Group
import com.queatz.db.Message
import com.queatz.db.PersonChip
import com.queatz.db.PersonChipExtended
import com.queatz.db.computeChipGradientPalette
import com.queatz.db.evaluateChipDisplay
import com.queatz.db.parseChipHexColor
import com.queatz.db.resolveChipBaseHex
import com.queatz.push.ChipPushData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun TopChipsRow(
    onSignalsClick: () -> Unit,
) {
    val items by chips.items.collectAsState()
    val me = me
    var showOverlay by remember { mutableStateOf(false) }
    var selectedChipId by remember { mutableStateOf<String?>(null) }

    val aboveItems = remember(items) { items.filter { it.chip.isAbove != false } }
    val belowItems = remember(items) { items.filter { it.chip.isAbove == false } }

    val scrollState = rememberScrollState()

    var containerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var signalsCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var signalsCenterInContent by remember { mutableFloatStateOf(0f) }
    var viewportWidth by remember { mutableIntStateOf(0) }

    LaunchedEffect(me?.id) {
        if (me?.id != null) {
            chips.reload()
        }
    }

    LaunchedEffect(push) {
        push.events
            .filter { it is ChipPushData }
            .collect {
                chips.reload()
            }
    }

    val chipConfigsKey = remember(items) {
        items.map { Triple(it.chip.id, it.chip.isAbove, it.value != null) }
    }
    var hasInitialCentered by remember(chipConfigsKey) { mutableStateOf(false) }

    LaunchedEffect(chipConfigsKey, signalsCenterInContent, viewportWidth, scrollState.maxValue) {
        if (!hasInitialCentered && !scrollState.isScrollInProgress && viewportWidth > 0 && signalsCenterInContent > 0f) {
            if (scrollState.maxValue > 0) {
                val desiredScroll = (signalsCenterInContent - viewportWidth / 2f).roundToInt()
                val targetScroll = desiredScroll.coerceIn(0, scrollState.maxValue)
                scrollState.scrollTo(targetScroll)
                hasInitialCentered = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                containerCoordinates = coordinates
                viewportWidth = coordinates.size.width
                val signals = signalsCoordinates
                if (coordinates.isAttached && signals != null && signals.isAttached) {
                    val pos = coordinates.localPositionOf(signals, Offset.Zero)
                    val signalsLeftInContent = pos.x + scrollState.value
                    signalsCenterInContent = signalsLeftInContent + signals.size.width / 2f
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { showOverlay = true })
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 1.pad),
        ) {
            aboveItems.forEach { ext ->
                ChipItem(
                    chip = ext.chip,
                    value = ext.value ?: "...",
                    onClick = { selectedChipId = ext.chip.id },
                    onLongClick = { showOverlay = true },
                )
            }

            Box(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    signalsCoordinates = coordinates
                    val container = containerCoordinates
                    if (coordinates.isAttached && container != null && container.isAttached) {
                        val pos = container.localPositionOf(coordinates, Offset.Zero)
                        val signalsLeftInContent = pos.x + scrollState.value
                        signalsCenterInContent = signalsLeftInContent + coordinates.size.width / 2f
                        viewportWidth = container.size.width
                    }
                },
            ) {
                SignalsButton(
                    onClick = onSignalsClick,
                    onLongClick = { showOverlay = true },
                )
            }

            belowItems.forEach { ext ->
                ChipItem(
                    chip = ext.chip,
                    value = ext.value ?: "...",
                    onClick = { selectedChipId = ext.chip.id },
                    onLongClick = { showOverlay = true },
                )
            }
        }
    }

    if (showOverlay) {
        TopChipsOverlay(
            onDismiss = { showOverlay = false },
            onSignalsClick = onSignalsClick,
        )
    }

    selectedChipId?.let { chipId ->
        val ext = items.find { it.chip.id == chipId }
        if (ext == null) {
            selectedChipId = null
            return@let
        }

        var isEditing by remember(chipId) { mutableStateOf(false) }

        if (isEditing) {
            Dialog(
                onDismissRequest = {
                    isEditing = false
                    selectedChipId = null
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable {
                            isEditing = false
                            selectedChipId = null
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(enabled = false) {}
                            .padding(2.pad),
                        verticalArrangement = Arrangement.spacedBy(2.pad),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Edit Chip", style = MaterialTheme.typography.titleLarge)
                            IconButton(
                                onClick = {
                                    isEditing = false
                                    selectedChipId = null
                                },
                            ) {
                                Icon(Icons.Outlined.Close, null)
                            }
                        }

                        ChipEditor(
                            item = ext,
                            initiallyEditing = true,
                            onCancelStandalone = {
                                isEditing = false
                            },
                        )
                    }
                }
            }
        } else {
            ChipDetailsDialog(
                item = ext,
                onDismiss = { selectedChipId = null },
                onEdit = {
                    isEditing = true
                },
            )
        }
    }
}

@Composable
fun ChipDetailsDialog(
    item: PersonChipExtended,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
) {
    val chip = item.chip
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf("") }
    var isSharing by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    val sentText = stringResource(R.string.sent)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(3.pad),
                verticalArrangement = Arrangement.spacedBy(2.pad),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = chip.name?.takeIf { it.isNotBlank() } ?: "Unnamed Chip",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, "Edit")
                    }
                }

                ChipItem(
                    chip = chip,
                    value = item.value ?: "...",
                    onClick = {},
                    onLongClick = {},
                )

                Text(
                    text = "Last synced: ${item.syncedAt?.shortAgo() ?: "Never"}",
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (syncStatus.isNotBlank()) {
                    Text(
                        text = syncStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Button(
                    onClick = {
                        val id = chip.id ?: return@Button
                        if (isSyncing) return@Button
                        coroutineScope.launch {
                            isSyncing = true
                            syncStatus = "Syncing..."
                            val success = chips.sync(id)
                            syncStatus = if (success) "Sync successful" else "Sync failed"
                            isSyncing = false
                            delay(3000.milliseconds)
                            syncStatus = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isSyncing) "Syncing..." else "Sync Now")
                }

                Button(
                    onClick = { showShareDialog = true },
                    enabled = !isSharing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Sharing...")
                    } else {
                        Icon(Icons.Outlined.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.share))
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        ShareChipDialog(
            chip = chip,
            onDismissRequest = { showShareDialog = false },
            onShare = { groups, commentText ->
                showShareDialog = false
                coroutineScope.launch {
                    isSharing = true
                    var hasError = false
                    try {
                        val chipAttachment = ChipAttachment(
                            name = chip.name,
                            url = chip.url,
                            format = (chip.format ?: ChipFormatType.TEXT).name,
                            prefix = chip.prefix,
                            color = chip.color,
                            backgroundColor = parseChipHexColor(chip.color)?.toArgbInt(),
                            isAbove = chip.isAbove,
                            jsonPath = chip.jsonPath,
                        )
                        val attachmentJson = json.encodeToString(chipAttachment)

                        coroutineScope {
                            groups.map { group ->
                                async {
                                    api.sendMessage(
                                        group.id!!,
                                        Message(
                                            text = commentText,
                                            attachment = attachmentJson,
                                        ),
                                        onError = {
                                            hasError = true
                                        },
                                    )
                                }
                            }.awaitAll()
                        }
                        if (hasError) {
                            context.showDidntWork(R.string.error_send_message)
                        } else {
                            context.toast(sentText)
                        }
                    } catch (_: Exception) {
                        context.showDidntWork(R.string.error_send_message)
                    } finally {
                        isSharing = false
                    }
                }
            },
        )
    }
}

@Composable
fun ShareChipDialog(
    chip: PersonChip,
    onDismissRequest: () -> Unit,
    onShare: (groups: List<Group>, comment: String?) -> Unit,
) {
    var comment by remember { mutableStateOf("") }
    var showGroupPicker by remember { mutableStateOf(false) }
    val someone = stringResource(R.string.someone)
    val emptyGroup = stringResource(R.string.empty_group_name)
    val me = me

    if (!showGroupPicker) {
        Dialog(onDismissRequest = onDismissRequest) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(3.pad),
                    verticalArrangement = Arrangement.spacedBy(2.pad),
                ) {
                    Text("Share Chip", style = MaterialTheme.typography.titleLarge)

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        ChipItem(
                            chip = chip,
                            value = chip.name?.takeIf { it.isNotBlank() } ?: "...",
                            onClick = {},
                            onLongClick = {},
                        )
                    }

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Comment (optional)") },
                        placeholder = { Text("Say something about this chip...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text(stringResource(R.string.cancel))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { showGroupPicker = true }) {
                            Text("Choose conversation")
                        }
                    }
                }
            }
        }
    } else {
        ChooseGroupDialog(
            onDismissRequest = {
                showGroupPicker = false
                onDismissRequest()
            },
            title = stringResource(R.string.share),
            confirmFormatter = defaultConfirmFormatter(
                R.string.send_text,
                R.string.send_text_to_group,
                R.string.send_text_to_groups,
                R.string.send_text_to_x_groups,
            ) { it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()) },
        ) { groups ->
            val commentText = comment.trim().takeIf { it.isNotEmpty() }
            onShare(groups, commentText)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChipItem(
    chip: PersonChip,
    value: String,
    legacyBackgroundColor: Int? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val format = chip.format ?: ChipFormatType.TEXT
    val evaluated = remember(value, chip.prefix, chip.jsonPath, format) {
        evaluateChipDisplay(
            format = format,
            prefix = chip.prefix.orEmpty(),
            jsonPath = chip.jsonPath.orEmpty(),
            rawValue = value,
        )
    }

    val baseHex = remember(chip.color, legacyBackgroundColor, format, evaluated.kind) {
        resolveChipBaseHex(
            color = chip.color,
            legacyBackgroundColor = legacyBackgroundColor,
            format = format,
            kind = evaluated.kind,
        )
    }

    val palette = remember(baseHex) {
        computeChipGradientPalette(baseHex = baseHex)
    }

    val colors = remember(palette) {
        palette.stops.map { Color(it.toArgbInt()) }
    }
    val borderColors = remember(palette) {
        palette.borderStops.map { Color(it.toArgbInt()) }
    }

    val brush = rememberAnimatedGradientBrush(
        colors = colors,
        label = "ChipGradient",
    )
    val borderBrush = rememberAnimatedGradientBrush(
        colors = borderColors,
        label = "ChipBorderGradient",
    )

    val onColor = if (palette.useLightForeground) {
        Color(0xFFFFFFFF.toInt())
    } else {
        Color(0xFF1A1A1A.toInt())
    }

    val icon: ImageVector? = when (evaluated.kind) {
        ChipValueKind.BooleanTrue -> Icons.Outlined.ToggleOn
        ChipValueKind.BooleanFalse -> Icons.Outlined.ToggleOff
        ChipValueKind.Null -> Icons.Outlined.RemoveCircleOutline
        ChipValueKind.Undefined -> Icons.AutoMirrored.Outlined.HelpOutline
        ChipValueKind.Normal -> null
    }

    val iconTint: Color = when (evaluated.kind) {
        ChipValueKind.BooleanTrue -> if (palette.useLightForeground) Color(0xFFB9F6CA) else Color(0xFF1B5E20)
        ChipValueKind.BooleanFalse -> if (palette.useLightForeground) Color(0xFFE0E0E0) else Color(0xFF616161)
        ChipValueKind.Null -> if (palette.useLightForeground) Color(0xFFE0E0E0) else Color(0xFF616161)
        ChipValueKind.Undefined -> if (palette.useLightForeground) Color(0xFFFFCC80) else Color(0xFFE65100)
        ChipValueKind.Normal -> onColor
    }

    val prefixToDisplay = evaluated.prefix.take(100)
    val remainingChars = (100 - prefixToDisplay.length).coerceAtLeast(0)
    val textToDisplay = evaluated.text.take(remainingChars)

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(brush)
            .border(2.dp, borderBrush, CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 1.5f.pad, vertical = 0.5f.pad),
        contentAlignment = Alignment.Center,
    ) {
        if (icon == null) {
            Text(
                text = "$prefixToDisplay$textToDisplay",
                style = MaterialTheme.typography.labelLarge,
                color = onColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (prefixToDisplay.isNotEmpty()) {
                    Text(
                        text = prefixToDisplay,
                        style = MaterialTheme.typography.labelLarge,
                        color = onColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                    Spacer(Modifier.width(2.dp))
                }
                Icon(
                    imageVector = icon,
                    contentDescription = textToDisplay,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
                if (textToDisplay.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = textToDisplay,
                        style = MaterialTheme.typography.labelLarge,
                        color = onColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

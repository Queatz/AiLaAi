package com.queatz.ailaai.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.greetingRes
import com.queatz.ailaai.services.ChipConfig
import com.queatz.ailaai.services.ChipFormatType
import com.queatz.ailaai.services.chips
import com.queatz.ailaai.services.signals
import com.queatz.ailaai.services.ChipCache
import com.queatz.ailaai.ui.theme.pad
import java.text.DecimalFormat
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.Close
import com.queatz.ailaai.extensions.shortAgo
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import app.ailaai.api.sendMessage
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.me
import com.queatz.ailaai.services.JsonParsedResult
import com.queatz.ailaai.services.evaluateJson
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.db.ChipAttachment
import com.queatz.db.Message
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun TopChipsRow(
    onSignalsClick: () -> Unit
) {
    val configs by chips.configs.collectAsState()
    val cache by chips.cache.collectAsState()
    var showOverlay by remember { mutableStateOf(false) }
    var selectedChipConfig by remember { mutableStateOf<ChipConfig?>(null) }

    val aboveConfigs = configs.filter { it.isAbove }
    val belowConfigs = configs.filter { !it.isAbove }

    val scrollState = rememberScrollState()

    var containerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var signalsCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var signalsCenterInContent by remember { mutableFloatStateOf(0f) }
    var viewportWidth by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        chips.refreshCacheIfNeeded()
    }

    val chipConfigsKey = remember(configs, configs.all { cache.containsKey(it.id) }) {
        configs.map { it.id to it.isAbove to cache.containsKey(it.id) }
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
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 1.pad)
        ) {
            aboveConfigs.forEach { config ->
                val value = cache[config.id]?.value ?: "..."
                ChipItem(config = config, value = value, onClick = { selectedChipConfig = config }, onLongClick = { showOverlay = true })
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
                }
            ) {
                SignalsButton(
                    onClick = onSignalsClick,
                    onLongClick = { showOverlay = true }
                )
            }

            belowConfigs.forEach { config ->
                val value = cache[config.id]?.value ?: "..."
                ChipItem(config = config, value = value, onClick = { selectedChipConfig = config }, onLongClick = { showOverlay = true })
            }
        }
    }

    if (showOverlay) {
        TopChipsOverlay(
            onDismiss = { showOverlay = false },
            onSignalsClick = onSignalsClick
        )
    }

    selectedChipConfig?.let { baseConfig ->
        val config = configs.find { it.id == baseConfig.id }
        if (config == null) {
            selectedChipConfig = null
            return@let
        }

        var isEditing by remember { mutableStateOf(false) }

        if (isEditing) {
            Dialog(
                onDismissRequest = { 
                    isEditing = false
                    selectedChipConfig = null
                },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable {
                            isEditing = false
                            selectedChipConfig = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(enabled = false) {}
                            .padding(2.pad),
                        verticalArrangement = Arrangement.spacedBy(2.pad)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Edit Chip", style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = {
                                isEditing = false
                                selectedChipConfig = null
                            }) {
                                Icon(Icons.Outlined.Close, null)
                            }
                        }

                        ChipEditor(config = config, initiallyEditing = true, onCancelStandalone = { 
                            isEditing = false
                        })
                    }
                }
            }
        } else {
            val cached = cache[config.id]
            ChipDetailsDialog(
                config = config,
                cached = cached,
                onDismiss = { selectedChipConfig = null },
                onEdit = {
                    isEditing = true
                }
            )
        }
    }
}

@Composable
fun ChipDetailsDialog(config: ChipConfig, cached: ChipCache?, onDismiss: () -> Unit, onEdit: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf("") }
    var showShareDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(3.pad),
                verticalArrangement = Arrangement.spacedBy(2.pad),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = config.name.takeIf { it.isNotBlank() } ?: "Unnamed Chip",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, "Edit")
                    }
                }

                ChipItem(config = config, value = cached?.value ?: "...", onClick = {}, onLongClick = {})

                Text(
                    text = "Last synced: ${cached?.timestamp?.takeIf { it > 0 }?.let { kotlin.time.Instant.fromEpochMilliseconds(it).shortAgo() } ?: "Never"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (syncStatus.isNotBlank()) {
                    Text(
                        text = syncStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = {
                        if (isSyncing) return@Button
                        coroutineScope.launch {
                            isSyncing = true
                            syncStatus = "Syncing..."
                            val success = chips.syncChip(config)
                            syncStatus = if (success) "Sync successful" else "Sync failed"
                            isSyncing = false
                            delay(3000.milliseconds)
                            syncStatus = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isSyncing) "Syncing..." else "Sync Now")
                }

                Button(
                    onClick = { showShareDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Share, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.share))
                }
            }
        }
    }

    if (showShareDialog) {
        ShareChipDialog(
            config = config,
            onDismissRequest = { showShareDialog = false }
        )
    }
}

@Composable
fun ShareChipDialog(
    config: ChipConfig,
    onDismissRequest: () -> Unit
) {
    var comment by remember { mutableStateOf("") }
    var showGroupPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val someone = stringResource(R.string.someone)
    val emptyGroup = stringResource(R.string.empty_group_name)
    val me = me

    val sentText = stringResource(R.string.sent)

    if (!showGroupPicker) {
        Dialog(onDismissRequest = onDismissRequest) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(3.pad),
                    verticalArrangement = Arrangement.spacedBy(2.pad)
                ) {
                    Text("Share Chip", style = MaterialTheme.typography.titleLarge)

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        ChipItem(
                            config = config,
                            value = config.name.takeIf { it.isNotBlank() } ?: "...",
                            onClick = {},
                            onLongClick = {}
                        )
                    }

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Comment (optional)") },
                        placeholder = { Text("Say something about this chip...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
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
                R.string.send_text_to_x_groups
            ) { it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()) }
        ) { groups ->
            val chipAttachment = ChipAttachment(
                name = config.name,
                url = config.url,
                format = config.type.name,
                prefix = config.prefix,
                backgroundColor = config.backgroundColor,
                isAbove = config.isAbove,
                jsonPath = config.jsonPath
            )
            val attachmentJson = json.encodeToString(chipAttachment)
            val commentText = comment.trim().takeIf { it.isNotEmpty() }

            coroutineScope.launch {
                coroutineScope {
                    groups.map { group ->
                        async {
                            api.sendMessage(
                                group.id!!,
                                Message(
                                    text = commentText,
                                    attachment = attachmentJson
                                )
                            )
                        }
                    }.awaitAll()
                }
                context.toast(sentText)
                onDismissRequest()
            }
        }
    }
}

private data class ChipDisplayInfo(
    val prefix: String = "",
    val text: String = "",
    val icon: ImageVector? = null,
    val iconTint: Color? = null,
    val isBoolean: Boolean = false,
    val booleanValue: Boolean? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChipItem(config: ChipConfig, value: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    val displayInfo = remember(value, config) {
        if (value == "..." || value.isBlank()) {
            return@remember ChipDisplayInfo(text = "...")
        }
        when (config.type) {
            ChipFormatType.FIXED_2 -> {
                val parsed = value.toDoubleOrNull()
                val text = if (parsed != null) DecimalFormat("#,##0.00").format(parsed / 100.0) else value
                ChipDisplayInfo(prefix = config.prefix, text = text)
            }
            ChipFormatType.NUMBER -> {
                val parsed = value.toDoubleOrNull()
                val text = if (parsed != null) DecimalFormat("#,###.##").format(parsed) else value
                ChipDisplayInfo(prefix = config.prefix, text = text)
            }
            ChipFormatType.TEXT -> {
                ChipDisplayInfo(prefix = config.prefix, text = value)
            }
            ChipFormatType.JSON -> {
                when (val jsonResult = evaluateJson(value, config.jsonPath)) {
                    is JsonParsedResult.BooleanValue -> {
                        ChipDisplayInfo(
                            prefix = config.prefix,
                            text = if (jsonResult.value) "true" else "false",
                            icon = if (jsonResult.value) Icons.Outlined.ToggleOn else Icons.Outlined.ToggleOff,
                            iconTint = if (jsonResult.value) Color(0xFF1B5E20) else Color(0xFF757575),
                            isBoolean = true,
                            booleanValue = jsonResult.value
                        )
                    }
                    is JsonParsedResult.NullValue -> {
                        ChipDisplayInfo(
                            prefix = config.prefix,
                            text = "null",
                            icon = Icons.Outlined.RemoveCircleOutline,
                            iconTint = Color(0xFF757575)
                        )
                    }
                    is JsonParsedResult.UndefinedValue -> {
                        ChipDisplayInfo(
                            prefix = config.prefix,
                            text = "undefined",
                            icon = Icons.AutoMirrored.Outlined.HelpOutline,
                            iconTint = Color(0xFFE65100)
                        )
                    }
                    is JsonParsedResult.TextValue -> {
                        ChipDisplayInfo(
                            prefix = config.prefix,
                            text = jsonResult.value
                        )
                    }
                }
            }
        }
    }

    val defaultGrey = 0xFFE0E0E0.toInt()
    val backgroundColorInt = when {
        config.type == ChipFormatType.JSON && displayInfo.isBoolean && (config.backgroundColor == 0 || config.backgroundColor == defaultGrey) -> {
            if (displayInfo.booleanValue == true) 0xFFBAFFC9.toInt() // Pastel Green
            else 0xFFFFB3BA.toInt() // Pastel Pink/Red
        }
        config.backgroundColor == 0 -> defaultGrey
        else -> config.backgroundColor
    }
    val color = Color(backgroundColorInt)
    val onColor = Color(0xFF000000.toInt())

    val prefixToDisplay = displayInfo.prefix.take(100)
    val remainingChars = (100 - prefixToDisplay.length).coerceAtLeast(0)
    val textToDisplay = displayInfo.text.take(remainingChars)

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 1.5f.pad, vertical = 0.5f.pad),
        contentAlignment = Alignment.Center
    ) {
        if (displayInfo.icon == null) {
            Text(
                text = "${prefixToDisplay}${textToDisplay}",
                style = MaterialTheme.typography.labelLarge,
                color = onColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (prefixToDisplay.isNotEmpty()) {
                    Text(
                        text = prefixToDisplay,
                        style = MaterialTheme.typography.labelLarge,
                        color = onColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                    Spacer(Modifier.width(2.dp))
                }
                Icon(
                    imageVector = displayInfo.icon,
                    contentDescription = textToDisplay,
                    tint = displayInfo.iconTint ?: onColor,
                    modifier = Modifier.size(18.dp)
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
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
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

    LaunchedEffect(Unit) {
        chips.refreshCacheIfNeeded()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = { showOverlay = true })
        }
    ) {
        aboveConfigs.forEach { config ->
            val value = cache[config.id]?.value ?: "..."
            ChipItem(config = config, value = value, onClick = { selectedChipConfig = config }, onLongClick = { showOverlay = true })
        }

        SignalsButton(
            onClick = onSignalsClick,
            onLongClick = { showOverlay = true }
        )

        belowConfigs.forEach { config ->
            val value = cache[config.id]?.value ?: "..."
            ChipItem(config = config, value = value, onClick = { selectedChipConfig = config }, onLongClick = { showOverlay = true })
        }
    }

    if (showOverlay) {
        TopChipsOverlay(
            onDismiss = { showOverlay = false },
            onSignalsClick = onSignalsClick
        )
    }

    selectedChipConfig?.let { config ->
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
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
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

                        // We can just pull from the configs again so we have the live reactive version
                        val liveConfig = configs.find { it.id == config.id }
                        if (liveConfig != null) {
                            ChipEditor(config = liveConfig, initiallyEditing = true, onCancelStandalone = { 
                                isEditing = false
                            })
                        }
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
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChipItem(config: ChipConfig, value: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    val formattedValue = remember(value, config.type, config.prefix) {
        if (value == "..." || value.isBlank()) return@remember "..."
        val parsed = value.toDoubleOrNull()
        val text = when (config.type) {
            ChipFormatType.FIXED_2 -> if (parsed != null) DecimalFormat("#,##0.00").format(parsed / 100.0) else value
            ChipFormatType.NUMBER -> if (parsed != null) DecimalFormat("#,###.##").format(parsed) else value
            ChipFormatType.TEXT -> value
        }
        "${config.prefix}$text"
    }

    // Colors mapping (for pastel we can use static colors or map from config.backgroundColor)
    // Here we can map config.backgroundColor to a Color, but for simplicity let's use MaterialTheme colors if 0, else custom.
    val color = androidx.compose.ui.graphics.Color(if (config.backgroundColor == 0) 0xFFE0E0E0.toInt() else config.backgroundColor)
    val onColor = androidx.compose.ui.graphics.Color(0xFF000000.toInt())

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 1.5f.pad, vertical = 0.5f.pad),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formattedValue,
            style = MaterialTheme.typography.labelLarge,
            color = onColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 120.dp)
        )
    }
}

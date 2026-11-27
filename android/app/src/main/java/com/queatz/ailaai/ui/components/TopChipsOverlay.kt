package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.queatz.ailaai.services.chips
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.ChipFormatType
import com.queatz.db.ChipOrderItem
import com.queatz.db.PersonChip
import com.queatz.db.PersonChipExtended
import com.queatz.db.chipPastelHexColors
import com.queatz.db.defaultChipHexColor
import com.queatz.db.parseChipHexColor
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private sealed interface OverlayReorderEntry {
    val key: String

    data class Chip(
        val item: PersonChipExtended,
    ) : OverlayReorderEntry {
        override val key: String = "chip:${item.chip.id}"
    }

    data object Signals : OverlayReorderEntry {
        override val key: String = "signals"
    }
}

private fun buildOverlayEntries(
    items: List<PersonChipExtended>,
): List<OverlayReorderEntry> {
    val above = items.filter { it.chip.isAbove != false }.map { OverlayReorderEntry.Chip(it) }
    val below = items.filter { it.chip.isAbove == false }.map { OverlayReorderEntry.Chip(it) }
    return above + OverlayReorderEntry.Signals + below
}

@Composable
fun TopChipsOverlay(
    onDismiss: () -> Unit,
    onSignalsClick: () -> Unit,
) {
    val remoteItems by chips.items.collectAsState()
    val scope = rememberCoroutineScope()

    var entries by remember { mutableStateOf(buildOverlayEntries(remoteItems)) }
    var isDraggingAny by remember { mutableStateOf(false) }
    var newlyCreatedId by remember { mutableStateOf<String?>(null) }
    var isCreating by remember { mutableStateOf(false) }

    LaunchedEffect(remoteItems, isDraggingAny) {
        if (!isDraggingAny) {
            val remoteMap = remoteItems.associateBy { it.chip.id }
            val updated = entries.mapNotNull { entry ->
                when (entry) {
                    is OverlayReorderEntry.Signals -> entry
                    is OverlayReorderEntry.Chip -> {
                        val id = entry.item.chip.id
                        remoteMap[id]?.let { OverlayReorderEntry.Chip(it) }
                    }
                }
            }
            val existingIds = updated.mapNotNull { (it as? OverlayReorderEntry.Chip)?.item?.chip?.id }.toSet()
            val newlyAdded = remoteItems.filter { it.chip.id !in existingIds }.map { OverlayReorderEntry.Chip(it) }
            entries = updated + newlyAdded
        }
    }

    fun persistCurrentOrder() {
        val signalsIndex = entries.indexOfFirst { it is OverlayReorderEntry.Signals }
            .let { if (it < 0) entries.size else it }
        var orderCounter = 0
        val payload = mutableListOf<ChipOrderItem>()
        entries.forEachIndexed { idx, entry ->
            if (entry is OverlayReorderEntry.Chip) {
                val id = entry.item.chip.id ?: return@forEachIndexed
                payload += ChipOrderItem(
                    id = id,
                    order = orderCounter++,
                    isAbove = idx < signalsIndex,
                )
            }
        }
        scope.launch {
            chips.reorder(payload)
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (from.index in entries.indices && to.index in entries.indices) {
            entries = entries.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(enabled = false) {}
                    .padding(2.pad),
                verticalArrangement = Arrangement.spacedBy(1.5f.pad),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Chips",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "Drag up or down to rearrange",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, null)
                    }
                }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.pad),
                ) {
                    items(
                        items = entries,
                        key = { it.key },
                    ) { entry ->
                        ReorderableItem(
                            state = reorderState,
                            key = entry.key,
                        ) { isDragging ->
                            LaunchedEffect(isDragging) {
                                if (isDragging) {
                                    isDraggingAny = true
                                } else if (isDraggingAny) {
                                    isDraggingAny = false
                                    persistCurrentOrder()
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = if (isDragging) 6.dp else 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (isDragging) Modifier.shadow(6.dp, RoundedCornerShape(12.dp)) else Modifier),
                            ) {
                                when (entry) {
                                    is OverlayReorderEntry.Signals -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 1.pad, vertical = 1.pad),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DragIndicator,
                                                contentDescription = "Reorder Signals",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .draggableHandle()
                                                    .padding(end = 1.pad),
                                            )
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                SignalsButton(
                                                    onClick = {
                                                        onDismiss()
                                                        onSignalsClick()
                                                    },
                                                )
                                            }
                                            Spacer(Modifier.width(32.dp))
                                        }
                                    }

                                    is OverlayReorderEntry.Chip -> {
                                        val latestItem = remoteItems.find { it.chip.id == entry.item.chip.id } ?: entry.item
                                        ChipEditor(
                                            item = latestItem,
                                            initiallyEditing = latestItem.chip.id == newlyCreatedId,
                                            dragHandle = {
                                                Icon(
                                                    imageVector = Icons.Outlined.DragIndicator,
                                                    contentDescription = "Reorder Chip",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier
                                                        .draggableHandle()
                                                        .padding(end = 0.5f.pad),
                                                )
                                            },
                                            onCancelStandalone = {
                                                if (newlyCreatedId == latestItem.chip.id) {
                                                    newlyCreatedId = null
                                                }
                                            },
                                            onDelete = { id ->
                                                entries = entries.filterNot { it is OverlayReorderEntry.Chip && it.item.chip.id == id }
                                                if (newlyCreatedId == id) {
                                                    newlyCreatedId = null
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (isCreating) return@Button
                        isCreating = true
                        scope.launch {
                            val created = chips.create(
                                PersonChip(
                                    name = "",
                                    url = "",
                                    format = ChipFormatType.TEXT,
                                    prefix = "",
                                    color = defaultChipHexColor,
                                    jsonPath = "",
                                    isAbove = false,
                                ),
                            )
                            newlyCreatedId = created?.chip?.id
                            isCreating = false
                        }
                    },
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Chip")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChipEditor(
    item: PersonChipExtended,
    initiallyEditing: Boolean = false,
    dragHandle: (@Composable () -> Unit)? = null,
    onCancelStandalone: (() -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
) {
    val chip = item.chip
    val scope = rememberCoroutineScope()
    var isEditing by remember(initiallyEditing) { mutableStateOf(initiallyEditing) }
    var isSaving by remember { mutableStateOf(false) }

    var name by remember(isEditing, chip.id, chip.name) { mutableStateOf(chip.name.orEmpty()) }
    var url by remember(isEditing, chip.id, chip.url) { mutableStateOf(chip.url.orEmpty()) }
    var prefix by remember(isEditing, chip.id, chip.prefix) { mutableStateOf(chip.prefix.orEmpty()) }
    var format by remember(isEditing, chip.id, chip.format) { mutableStateOf(chip.format ?: ChipFormatType.TEXT) }
    var jsonPath by remember(isEditing, chip.id, chip.jsonPath) { mutableStateOf(chip.jsonPath.orEmpty()) }
    var colorHex by remember(isEditing, chip.id, chip.color) {
        mutableStateOf(parseChipHexColor(chip.color)?.toHex() ?: defaultChipHexColor)
    }
    var showCustomColorPicker by remember { mutableStateOf(false) }

    val value = item.value ?: "..."

    val liveChip = remember(name, url, prefix, format, jsonPath, colorHex, chip.isAbove, chip.order, chip.id) {
        PersonChip(
            person = chip.person,
            name = name,
            url = url,
            format = format,
            prefix = prefix,
            color = colorHex,
            jsonPath = jsonPath,
            isAbove = chip.isAbove,
            order = chip.order,
        ).apply {
            id = chip.id
            createdAt = chip.createdAt
        }
    }

    if (showCustomColorPicker) {
        ChipColorPickerDialog(
            initialColorHex = colorHex,
            previewChip = liveChip,
            previewValue = value.takeIf { it != "..." && it.isNotBlank() }
                ?: name.takeIf { it.isNotBlank() }
                ?: "...",
            onDismissRequest = { showCustomColorPicker = false },
            onColorSelected = { selectedHex ->
                colorHex = selectedHex
                showCustomColorPicker = false
            },
        )
    }

    if (!isEditing) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 1.pad, vertical = 0.5f.pad),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dragHandle?.invoke()
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                ChipItem(
                    chip = chip,
                    value = value,
                    onClick = { isEditing = true },
                    onLongClick = {},
                )
            }
            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Outlined.Edit, "Edit")
            }
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(1.5f.pad),
                verticalArrangement = Arrangement.spacedBy(1.pad),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        dragHandle?.invoke()
                        ChipItem(
                            chip = liveChip,
                            value = value,
                            onClick = {},
                            onLongClick = {},
                        )
                    }
                    IconButton(
                        onClick = {
                            val id = chip.id ?: return@IconButton
                            onDelete?.invoke(id)
                            scope.launch {
                                chips.remove(id)
                                onCancelStandalone?.invoke()
                            }
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Chip URL (GET)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(1.pad)) {
                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it },
                        label = { Text("Prefix (e.g. \$)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(2f),
                    ) {
                        OutlinedTextField(
                            value = format.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Format") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            ChipFormatType.entries.forEach { formatType ->
                                DropdownMenuItem(
                                    text = { Text(formatType.name) },
                                    onClick = {
                                        format = formatType
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                if (format == ChipFormatType.JSON) {
                    OutlinedTextField(
                        value = jsonPath,
                        onValueChange = { jsonPath = it },
                        label = { Text("JSON Path") },
                        placeholder = { Text("e.g. messages.0.text") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                val isPresetSelected = chipPastelHexColors.any { it.equals(colorHex, ignoreCase = true) }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    chipPastelHexColors.forEach { presetHex ->
                        val isSelected = presetHex.equals(colorHex, ignoreCase = true)
                        val swatchColor = parseChipHexColor(presetHex)?.let { Color(it.toArgbInt()) } ?: Color.LightGray
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                )
                                .clickable { colorHex = presetHex },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    // Custom color swatch using StatusColorPicker
                    val customSwatchBg = parseChipHexColor(colorHex)?.let { Color(it.toArgbInt()) }
                        ?: MaterialTheme.colorScheme.surfaceVariant
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(customSwatchBg)
                            .border(
                                width = if (!isPresetSelected) 3.dp else 1.dp,
                                color = if (!isPresetSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                            .clickable { showCustomColorPicker = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Palette,
                            contentDescription = "Custom color",
                            tint = if (!isPresetSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        onClick = {
                            isEditing = false
                            onCancelStandalone?.invoke()
                        },
                        enabled = !isSaving,
                    ) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                chips.update(liveChip)
                                isSaving = false
                                isEditing = false
                                onCancelStandalone?.invoke()
                            }
                        },
                        enabled = !isSaving,
                    ) {
                        Text(if (isSaving) "Saving..." else "Save changes")
                    }
                }
            }
        }
    }
}

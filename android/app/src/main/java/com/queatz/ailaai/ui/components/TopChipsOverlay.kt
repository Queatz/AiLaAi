package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.outlined.Check
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.queatz.ailaai.R
import com.queatz.ailaai.services.ChipConfig
import com.queatz.ailaai.services.ChipFormatType
import com.queatz.ailaai.services.chips
import com.queatz.ailaai.ui.theme.pad
import java.util.UUID

val pastelColors = listOf(
    0xFFFFB3BA.toInt(), // Pastel Pink
    0xFFFFDFBA.toInt(), // Pastel Orange
    0xFFFFFFBA.toInt(), // Pastel Yellow
    0xFFBAFFC9.toInt(), // Pastel Green
    0xFFBAE1FF.toInt(), // Pastel Blue
    0xFFE8BAFF.toInt(), // Pastel Purple
    0xFFE0E0E0.toInt()  // Grey
)

@Composable
fun TopChipsOverlay(
    onDismiss: () -> Unit,
    onSignalsClick: () -> Unit
) {
    val configs by chips.configs.collectAsState()

    val aboveConfigs = configs.filter { it.isAbove }
    val belowConfigs = configs.filter { !it.isAbove }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f)
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
                    Text("Chips Configuration", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, null)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.pad)
                ) {
                    item {
                        Text("Left Chips", style = MaterialTheme.typography.titleMedium)
                    }
                    items(aboveConfigs) { config ->
                        ChipEditor(config)
                    }
                    item {
                        Button(onClick = {
                            chips.addConfig(ChipConfig(id = UUID.randomUUID().toString(), isAbove = true, backgroundColor = pastelColors.last()))
                        }) {
                            Icon(Icons.Outlined.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Left Chip")
                        }

                        HorizontalDivider(Modifier.padding(vertical = 2.pad))

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            SignalsButton(onClick = {
                                onDismiss()
                                onSignalsClick()
                            })
                        }

                        HorizontalDivider(Modifier.padding(vertical = 2.pad))

                        Text("Right Chips", style = MaterialTheme.typography.titleMedium)
                    }
                    items(belowConfigs) { config ->
                        ChipEditor(config)
                    }
                    item {
                        Button(onClick = {
                            chips.addConfig(ChipConfig(id = UUID.randomUUID().toString(), isAbove = false, backgroundColor = pastelColors.last()))
                        }) {
                            Icon(Icons.Outlined.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Right Chip")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChipEditor(config: ChipConfig, initiallyEditing: Boolean = false, onCancelStandalone: (() -> Unit)? = null) {
    var isEditing by remember { mutableStateOf(initiallyEditing) }

    var name by remember(isEditing, config) { mutableStateOf(config.name) }
    var url by remember(isEditing, config) { mutableStateOf(config.url) }
    var prefix by remember(isEditing, config) { mutableStateOf(config.prefix) }
    var type by remember(isEditing, config) { mutableStateOf(config.type) }
    var jsonPath by remember(isEditing, config) { mutableStateOf(config.jsonPath) }
    var color by remember(isEditing, config) { mutableIntStateOf(config.backgroundColor) }

    val cache by chips.cache.collectAsState()
    val value = cache[config.id]?.value ?: "..."

    val liveConfig = config.copy(name = name, url = url, prefix = prefix, type = type, jsonPath = jsonPath, backgroundColor = color)

    if (!isEditing) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChipItem(config = config, value = value, onClick = { isEditing = true }, onLongClick = {})
            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Outlined.Edit, "Edit")
            }
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(1.pad), verticalArrangement = Arrangement.spacedBy(1.pad)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    ChipItem(config = liveConfig, value = value, onClick = {}, onLongClick = {})
                    IconButton(onClick = { 
                        chips.removeConfig(config.id) 
                        onCancelStandalone?.invoke()
                    }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Chip URL (GET)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(1.pad)) {
                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it },
                        label = { Text("Prefix (e.g. \$)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // Format dropdown
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(2f)
                    ) {
                        OutlinedTextField(
                            value = type.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Format") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ChipFormatType.entries.forEach { formatType ->
                                DropdownMenuItem(
                                    text = { Text(formatType.name) },
                                    onClick = {
                                        type = formatType
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (type == ChipFormatType.JSON) {
                    OutlinedTextField(
                        value = jsonPath,
                        onValueChange = { jsonPath = it },
                        label = { Text("JSON Path") },
                        placeholder = { Text("e.g. messages.0.text") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Colors
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    pastelColors.forEach { pastelColor ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(pastelColor))
                                .border(
                                    width = if (color == pastelColor) 3.dp else 1.dp,
                                    color = if (color == pastelColor) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable { color = pastelColor },
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == pastelColor) {
                                Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { 
                        isEditing = false 
                        onCancelStandalone?.invoke()
                    }) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        chips.updateConfig(liveConfig)
                        isEditing = false
                        onCancelStandalone?.invoke()
                    }) {
                        Text("Save changes")
                    }
                }
            }
        }
    }
}

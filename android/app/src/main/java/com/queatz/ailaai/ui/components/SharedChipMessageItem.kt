package com.queatz.ailaai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.services.ChipConfig
import com.queatz.ailaai.services.ChipFormatType
import com.queatz.ailaai.services.chips
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.ChipAttachment
import java.util.UUID

@Composable
fun ColumnScope.SharedChipMessageItem(
    chip: ChipAttachment,
    isMe: Boolean,
    isReply: Boolean
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var liveValue by remember(chip.url) { mutableStateOf<String?>(null) }
    val myConfigs by chips.configs.collectAsState()

    val chipConfig = remember(chip) {
        ChipConfig(
            id = "",
            name = chip.name ?: "",
            url = chip.url ?: "",
            type = try {
                ChipFormatType.valueOf(chip.format ?: "TEXT")
            } catch (_: Exception) {
                ChipFormatType.TEXT
            },
            prefix = chip.prefix ?: "",
            backgroundColor = chip.backgroundColor ?: 0,
            isAbove = chip.isAbove ?: true,
            jsonPath = chip.jsonPath ?: ""
        )
    }

    LaunchedEffect(chip.url) {
        if (!chip.url.isNullOrBlank()) {
            val fetched = chips.fetchUrl(chip.url!!)
            if (fetched != null) {
                liveValue = fetched
            }
        }
    }

    val isAlreadyAdded = myConfigs.any { it.url == chip.url && it.name == chip.name }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .padding(1.pad)
            .then(
                if (isReply) {
                    Modifier
                } else {
                    when (isMe) {
                        true -> Modifier
                            .padding(start = 8.pad)
                            .align(Alignment.End)

                        false -> Modifier
                            .padding(end = 8.pad)
                            .align(Alignment.Start)
                    }
                }
            )
    ) {
        Column(
            modifier = Modifier.padding(1.5f.pad),
            verticalArrangement = Arrangement.spacedBy(1.pad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.pad),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = chip.name?.takeIf { it.isNotBlank() } ?: "Shared Chip",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            ChipItem(
                config = chipConfig,
                value = liveValue ?: chip.name?.takeIf { it.isNotBlank() } ?: "...",
                onClick = { showAddDialog = true },
                onLongClick = {}
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(
                        if (isAlreadyAdded) Icons.Outlined.Check else Icons.Outlined.Add,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isAlreadyAdded) "Added" else "Add Chip")
                }
            }
        }
    }

    if (showAddDialog) {
        InstallChipDialog(
            chip = chip,
            onDismissRequest = { showAddDialog = false }
        )
    }
}

@Composable
fun InstallChipDialog(
    chip: ChipAttachment,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var isAbove by remember { mutableStateOf(chip.isAbove ?: true) }
    val chipConfig = remember(chip, isAbove) {
        ChipConfig(
            id = "",
            name = chip.name ?: "",
            url = chip.url ?: "",
            type = try {
                ChipFormatType.valueOf(chip.format ?: "TEXT")
            } catch (_: Exception) {
                ChipFormatType.TEXT
            },
            prefix = chip.prefix ?: "",
            backgroundColor = chip.backgroundColor ?: 0,
            isAbove = isAbove,
            jsonPath = chip.jsonPath ?: ""
        )
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(3.pad),
                verticalArrangement = Arrangement.spacedBy(2.pad)
            ) {
                Text("Add to Chips", style = MaterialTheme.typography.titleLarge)

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ChipItem(
                        config = chipConfig,
                        value = chip.name?.takeIf { it.isNotBlank() } ?: "...",
                        onClick = {},
                        onLongClick = {}
                    )
                }

                Text("Choose position:", style = MaterialTheme.typography.titleSmall)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isAbove = true }
                        .padding(horizontal = 1.pad, vertical = 0.5f.pad)
                ) {
                    RadioButton(
                        selected = isAbove,
                        onClick = { isAbove = true }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Left Chips", style = MaterialTheme.typography.bodyLarge)
                        Text("Displayed to the left of signals", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isAbove = false }
                        .padding(horizontal = 1.pad, vertical = 0.5f.pad)
                ) {
                    RadioButton(
                        selected = !isAbove,
                        onClick = { isAbove = false }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Right Chips", style = MaterialTheme.typography.bodyLarge)
                        Text("Displayed to the right of signals", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val newConfig = ChipConfig(
                            id = UUID.randomUUID().toString(),
                            name = chip.name ?: "",
                            url = chip.url ?: "",
                            type = try {
                                ChipFormatType.valueOf(chip.format ?: "TEXT")
                            } catch (_: Exception) {
                                ChipFormatType.TEXT
                            },
                            prefix = chip.prefix ?: "",
                            backgroundColor = chip.backgroundColor ?: 0,
                            isAbove = isAbove,
                            jsonPath = chip.jsonPath ?: ""
                        )
                        chips.addConfig(newConfig)
                        context.toast(if (isAbove) "Added to Left Chips" else "Added to Right Chips")
                        onDismissRequest()
                    }) {
                        Text("Add Chip")
                    }
                }
            }
        }
    }
}

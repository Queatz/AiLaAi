package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.services.chips
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.ChipAttachment
import com.queatz.db.ChipFormatType
import com.queatz.db.PersonChip
import com.queatz.db.defaultChipHexColor
import com.queatz.db.legacyColorIntToHex
import com.queatz.db.parseChipHexColor
import kotlinx.coroutines.launch

private fun ChipAttachment.toPersonChip(): PersonChip {
    val parsedFormat = try {
        ChipFormatType.valueOf(format ?: "TEXT")
    } catch (_: Exception) {
        ChipFormatType.TEXT
    }
    val resolvedColor = parseChipHexColor(color)?.toHex()
        ?: legacyColorIntToHex(backgroundColor)
        ?: defaultChipHexColor

    return PersonChip(
        name = name.orEmpty(),
        url = url.orEmpty(),
        format = parsedFormat,
        prefix = prefix.orEmpty(),
        color = resolvedColor,
        jsonPath = jsonPath.orEmpty(),
        isAbove = isAbove ?: false,
    )
}

@Composable
fun ColumnScope.SharedChipMessageItem(
    chip: ChipAttachment,
    isMe: Boolean,
    isReply: Boolean,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var liveValue by remember(chip.url) { mutableStateOf<String?>(null) }
    val myItems by chips.items.collectAsState()

    val previewChip = remember(chip) { chip.toPersonChip() }

    LaunchedEffect(chip.url) {
        val targetUrl = chip.url?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val preview = chips.previewUrl(targetUrl)
        if (!preview?.value.isNullOrBlank()) {
            liveValue = preview.value
        }
    }

    val isAlreadyAdded = myItems.any {
        it.chip.url == (chip.url ?: "") && it.chip.name == (chip.name ?: "")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
                },
            ),
    ) {
        Column(
            modifier = Modifier.padding(1.5f.pad),
            verticalArrangement = Arrangement.spacedBy(1.pad),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.pad),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = chip.name?.takeIf { it.isNotBlank() } ?: "Shared Chip",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            ChipItem(
                chip = previewChip,
                value = liveValue ?: chip.name?.takeIf { it.isNotBlank() } ?: "...",
                legacyBackgroundColor = chip.backgroundColor,
                onClick = { showAddDialog = true },
                onLongClick = {},
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = { showAddDialog = true },
                ) {
                    Icon(
                        if (isAlreadyAdded) Icons.Outlined.Check else Icons.Outlined.Add,
                        null,
                        modifier = Modifier.size(16.dp),
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
            liveValue = liveValue,
            onDismissRequest = { showAddDialog = false },
        )
    }
}

@Composable
fun InstallChipDialog(
    chip: ChipAttachment,
    liveValue: String?,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isInstalling by remember { mutableStateOf(false) }
    val previewChip = remember(chip) { chip.toPersonChip() }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(3.pad),
                verticalArrangement = Arrangement.spacedBy(2.pad),
            ) {
                Text("Add to Chips", style = MaterialTheme.typography.titleLarge)

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    ChipItem(
                        chip = previewChip,
                        value = liveValue ?: chip.name?.takeIf { it.isNotBlank() } ?: "...",
                        legacyBackgroundColor = chip.backgroundColor,
                        onClick = {},
                        onLongClick = {},
                    )
                }

                if (!chip.url.isNullOrBlank()) {
                    Text(
                        text = chip.url!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        enabled = !isInstalling,
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isInstalling) return@Button
                            isInstalling = true
                            scope.launch {
                                chips.create(previewChip)
                                isInstalling = false
                                context.toast("Added to Chips")
                                onDismissRequest()
                            }
                        },
                        enabled = !isInstalling,
                    ) {
                        Text(if (isInstalling) "Adding..." else "Add Chip")
                    }
                }
            }
        }
    }
}

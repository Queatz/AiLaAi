package app.chips

import Configuration
import Styles
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.sendMessage
import app.dialog.dialog
import app.dialog.selectGroupDialog
import application
import com.queatz.db.ChipAttachment
import com.queatz.db.ChipFormatType
import com.queatz.db.Message
import com.queatz.db.PersonChip
import com.queatz.db.parseChipHexColor
import components.Icon
import components.IconButton
import json
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import r
import time.formatDistanceToNow
import kotlin.js.Date
import kotlin.time.Duration.Companion.milliseconds

suspend fun chipDetailsDialog(
    chipId: String,
    configuration: Configuration,
) {
    var shouldOpenEdit = false
    var shareTargetChip: PersonChip? = null

    dialog(
        title = null,
        confirmButton = null,
        cancelButton = application.appString { close },
    ) { resolve ->
        val items by chips.items.collectAsState()
        val ext = items.find { it.chip.id == chipId }
        val scope = rememberCoroutineScope()
        var isSyncing by remember { mutableStateOf(false) }
        var syncStatus by remember { mutableStateOf("") }

        if (ext == null) {
            resolve(false)
            return@dialog
        }

        val chip = ext.chip

        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                alignItems(AlignItems.Center)
                gap(1.r)
                width(22.r)
                padding(.5.r, 0.r)
            }
        }) {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    justifyContent(JustifyContent.SpaceBetween)
                    width(100.percent)
                }
            }) {
                Span({
                    style {
                        fontSize(20.px)
                    }
                }) {
                    Text(chip.name?.takeIf { it.isNotBlank() } ?: "Unnamed Chip")
                }
                IconButton(
                    name = "edit",
                    title = "Edit Chip",
                ) {
                    shouldOpenEdit = true
                    resolve(true)
                }
            }

            ChipItem(
                chip = chip,
                value = ext.value ?: "...",
            )

            Div({
                style {
                    fontSize(13.px)
                    opacity(0.75)
                }
            }) {
                val syncedText = ext.syncedAt?.let {
                    "${formatDistanceToNow(Date(it.toEpochMilliseconds()))} ago"
                } ?: "Never"
                Text("Last synced: $syncedText")
            }

            if (syncStatus.isNotBlank()) {
                Div({
                    style {
                        fontSize(13.px)
                        color(Styles.colors.primary)
                    }
                }) {
                    Text(syncStatus)
                }
            }

            Button({
                classes(Styles.button)
                style {
                    width(100.percent)
                    justifyContent(JustifyContent.Center)
                }
                if (isSyncing) {
                    disabled()
                }
                onClick {
                    val id = chip.id ?: return@onClick
                    if (isSyncing) return@onClick
                    scope.launch {
                        isSyncing = true
                        syncStatus = "Syncing..."
                        val ok = chips.sync(id)
                        syncStatus = if (ok) "Sync successful" else "Sync failed"
                        isSyncing = false
                        delay(3000.milliseconds)
                        syncStatus = ""
                    }
                }
            }) {
                Icon("refresh")
                Span({ style { padding(0.r, .25.r) } }) {
                    Text(if (isSyncing) "Syncing..." else "Sync Now")
                }
            }

            Button({
                classes(Styles.outlineButton)
                style {
                    width(100.percent)
                    justifyContent(JustifyContent.Center)
                }
                onClick {
                    shareTargetChip = chip
                    resolve(true)
                }
            }) {
                Icon("share")
                Span({ style { padding(0.r, .25.r) } }) {
                    Text(application.appString { share })
                }
            }
        }
    }

    if (shouldOpenEdit) {
        editSingleChipDialog(chipId = chipId)
    } else if (shareTargetChip != null) {
        shareChipDialog(
            chip = shareTargetChip,
            configuration = configuration,
        )
    }
}

suspend fun editSingleChipDialog(
    chipId: String,
) {
    dialog(
        title = "Edit Chip",
        confirmButton = null,
        cancelButton = null,
    ) { resolve ->
        val items by chips.items.collectAsState()
        val ext = items.find { it.chip.id == chipId }
        if (ext == null) {
            resolve(false)
            return@dialog
        }
        Div({
            style {
                width(24.r)
            }
        }) {
            WebChipEditor(
                item = ext,
                initiallyEditing = true,
                onFinished = {
                    resolve(true)
                },
            )
        }
    }
}

suspend fun shareChipDialog(
    chip: PersonChip,
    configuration: Configuration,
) {
    var commentText = ""
    val proceed = dialog(
        title = "Share Chip",
        confirmButton = "Choose conversation",
        cancelButton = application.appString { cancel },
    ) {
        var comment by remember { mutableStateOf("") }
        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(1.r)
                width(22.r)
            }
        }) {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    justifyContent(JustifyContent.Center)
                    padding(.5.r)
                }
            }) {
                ChipItem(
                    chip = chip,
                    value = chip.name?.takeIf { it.isNotBlank() } ?: "...",
                )
            }
            TextArea(comment) {
                classes(Styles.textarea)
                placeholder("Say something about this chip... (optional)")
                style {
                    width(100.percent)
                }
                onInput {
                    comment = it.value
                    commentText = it.value
                }
            }
        }
    }

    if (proceed == true) {
        val groupExt = selectGroupDialog(
            configuration = configuration,
            title = application.appString { share },
        )
        val groupId = groupExt?.group?.id ?: return
        val attachment = ChipAttachment(
            name = chip.name,
            url = chip.url,
            format = (chip.format ?: ChipFormatType.TEXT).name,
            prefix = chip.prefix,
            color = chip.color,
            backgroundColor = parseChipHexColor(chip.color)?.toArgbInt(),
            isAbove = chip.isAbove,
            jsonPath = chip.jsonPath,
        )
        api.sendMessage(
            group = groupId,
            message = Message(
                text = commentText.trim().takeIf { it.isNotEmpty() },
                attachment = json.encodeToString(attachment),
            ),
        )
    }
}

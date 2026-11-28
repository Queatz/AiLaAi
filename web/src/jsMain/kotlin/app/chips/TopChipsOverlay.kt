package app.chips

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.web.events.SyntheticDragEvent
import app.dialog.dialog
import application
import com.queatz.db.ChipFormatType
import com.queatz.db.ChipOrderItem
import com.queatz.db.PersonChip
import com.queatz.db.PersonChipExtended
import com.queatz.db.chipPastelHexColors
import com.queatz.db.defaultChipHexColor
import com.queatz.db.parseChipHexColor
import components.DragOverPosition
import components.DraggableContentItem
import components.Icon
import components.IconButton
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.attributes.value
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.rgb
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

private sealed interface WebOverlayEntry {
    val key: String

    data class Chip(
        val item: PersonChipExtended,
    ) : WebOverlayEntry {
        override val key: String = "chip:${item.chip.id}"
    }

    data object Signals : WebOverlayEntry {
        override val key: String = "signals"
    }
}

private fun buildWebOverlayEntries(
    items: List<PersonChipExtended>,
): List<WebOverlayEntry> {
    val above = items.filter { it.chip.isAbove != false }.map { WebOverlayEntry.Chip(it) }
    val below = items.filter { it.chip.isAbove == false }.map { WebOverlayEntry.Chip(it) }
    return above + WebOverlayEntry.Signals + below
}

suspend fun topChipsOverlayDialog(
    onOpenSignals: () -> Unit,
) {
    var openSignalsRequested = false

    dialog(
        title = "Chips",
        confirmButton = null,
        cancelButton = application.appString { close },
        maxWidth = 32.r,
    ) { resolve ->
        val remoteItems by chips.items.collectAsState()
        val scope = rememberCoroutineScope()

        var entries by remember { mutableStateOf(buildWebOverlayEntries(remoteItems)) }
        var isDragging by remember { mutableStateOf(false) }
        var newlyCreatedId by remember { mutableStateOf<String?>(null) }
        var isCreating by remember { mutableStateOf(false) }

        LaunchedEffect(remoteItems, isDragging) {
            if (!isDragging) {
                val remoteMap = remoteItems.associateBy { it.chip.id }
                val updated = entries.mapNotNull { entry ->
                    when (entry) {
                        is WebOverlayEntry.Signals -> entry
                        is WebOverlayEntry.Chip -> {
                            val id = entry.item.chip.id
                            remoteMap[id]?.let { WebOverlayEntry.Chip(it) }
                        }
                    }
                }
                val existingIds = updated.mapNotNull { (it as? WebOverlayEntry.Chip)?.item?.chip?.id }.toSet()
                val newlyAdded = remoteItems.filter { it.chip.id !in existingIds }.map { WebOverlayEntry.Chip(it) }
                entries = updated + newlyAdded
            }
        }

        fun reorderEntries(
            fromIndex: Int,
            toIndex: Int,
        ) {
            if (fromIndex !in entries.indices) return
            val mutable = entries.toMutableList()
            val moved = mutable.removeAt(fromIndex)
            val adjustedTarget = (if (toIndex > fromIndex) toIndex - 1 else toIndex).coerceIn(0, mutable.size)
            mutable.add(adjustedTarget, moved)
            entries = mutable

            val signalsIdx = mutable.indexOfFirst { it is WebOverlayEntry.Signals }
                .let { if (it < 0) mutable.size else it }
            var orderCounter = 0
            val payload = mutableListOf<ChipOrderItem>()
            mutable.forEachIndexed { idx, entry ->
                if (entry is WebOverlayEntry.Chip) {
                    val id = entry.item.chip.id ?: return@forEachIndexed
                    payload += ChipOrderItem(
                        id = id,
                        order = orderCounter++,
                        isAbove = idx < signalsIdx,
                    )
                }
            }
            scope.launch {
                chips.reorder(payload)
            }
        }

        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(.75.r)
                width(26.r)
                maxHeight(68.vh)
                overflowY("auto")
                padding(.25.r)
            }
        }) {
            Div({
                style {
                    fontSize(12.px)
                    opacity(0.65)
                }
            }) {
                Text("Drag up or down to rearrange chips and Signals")
            }

            entries.forEachIndexed { index, entry ->
                key(entry.key) {
                    DraggableContentItem(
                        index = index,
                        editable = true,
                        onReorder = { from, to -> reorderEntries(from, to) },
                        onDragStateChange = { dragging -> isDragging = dragging },
                    ) {
                        Div({
                            classes(ChipStyles.overlayItemCard)
                            attr("draggable", "true")
                            onDragStart { event: SyntheticDragEvent ->
                                isDragging = true
                                event.dataTransfer?.setData("text/plain", index.toString())
                            }
                        }) {
                            when (entry) {
                                is WebOverlayEntry.Signals -> {
                                    Div({
                                        style {
                                            display(DisplayStyle.Flex)
                                            alignItems(AlignItems.Center)
                                            justifyContent(JustifyContent.SpaceBetween)
                                            gap(.5.r)
                                        }
                                    }) {
                                        Span({
                                            style {
                                                cursor("grab")
                                                opacity(0.65)
                                                display(DisplayStyle.Flex)
                                            }
                                            title("Drag to reorder Signals")
                                        }) {
                                            Icon("drag_indicator")
                                        }

                                        Div({
                                            style {
                                                flex(1)
                                                display(DisplayStyle.Flex)
                                                justifyContent(JustifyContent.Center)
                                            }
                                        }) {
                                            SignalsButton(
                                                onClick = {
                                                    openSignalsRequested = true
                                                    resolve(true)
                                                },
                                            )
                                        }

                                        IconButton(
                                            name = "open_in_new",
                                            title = "Open Signals",
                                        ) {
                                            openSignalsRequested = true
                                            resolve(true)
                                        }
                                    }
                                }

                                is WebOverlayEntry.Chip -> {
                                    val latest = remoteItems.find { it.chip.id == entry.item.chip.id } ?: entry.item
                                    WebChipEditor(
                                        item = latest,
                                        initiallyEditing = latest.chip.id == newlyCreatedId,
                                        showDragHandle = true,
                                        onFinished = {
                                            if (newlyCreatedId == latest.chip.id) {
                                                newlyCreatedId = null
                                            }
                                        },
                                        onDelete = { id ->
                                            entries = entries.filterNot { it is WebOverlayEntry.Chip && it.item.chip.id == id }
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

            Button({
                classes(Styles.button)
                style {
                    width(100.percent)
                    justifyContent(JustifyContent.Center)
                }
                if (isCreating) {
                    disabled()
                }
                onClick {
                    if (isCreating) return@onClick
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
                }
            }) {
                Icon("add")
                Span({ style { padding(0.r, .25.r) } }) {
                    Text(if (isCreating) "Adding..." else "Add Chip")
                }
            }
        }
    }

    if (openSignalsRequested) {
        onOpenSignals()
    }
}

@Composable
fun WebChipEditor(
    item: PersonChipExtended,
    initiallyEditing: Boolean = false,
    showDragHandle: Boolean = false,
    onFinished: () -> Unit = {},
    onDelete: (String) -> Unit = {},
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

    if (!isEditing) {
        Div({
            style {
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.SpaceBetween)
                gap(.5.r)
                width(100.percent)
            }
        }) {
            if (showDragHandle) {
                Span({
                    style {
                        cursor("grab")
                        opacity(0.65)
                        display(DisplayStyle.Flex)
                    }
                    title("Drag to reorder")
                }) {
                    Icon("drag_indicator")
                }
            }

            Div({
                style {
                    flex(1)
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                }
            }) {
                ChipItem(
                    chip = chip,
                    value = value,
                    onClick = { isEditing = true },
                )
            }

            IconButton(
                name = "edit",
                title = "Edit",
            ) {
                isEditing = true
            }
        }
    } else {
        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(.65.r)
                width(100.percent)
            }
            // Prevent drag while typing inside form inputs
            onDragStart {
                it.preventDefault()
                it.stopPropagation()
            }
        }) {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    justifyContent(JustifyContent.SpaceBetween)
                }
            }) {
                ChipItem(
                    chip = liveChip,
                    value = value.takeIf { it != "..." && it.isNotBlank() }
                        ?: name.takeIf { it.isNotBlank() }
                        ?: "...",
                )
                IconButton(
                    name = "delete",
                    title = "Delete chip",
                    styles = {
                        color(Styles.colors.red)
                    },
                ) {
                    val id = chip.id ?: return@IconButton
                    onDelete(id)
                    scope.launch {
                        chips.remove(id)
                        onFinished()
                    }
                }
            }

            Input(InputType.Text) {
                classes(Styles.textarea)
                value(name)
                placeholder("Title")
                style { width(100.percent) }
                onInput { name = it.value }
            }

            Input(InputType.Url) {
                classes(Styles.textarea)
                value(url)
                placeholder("Chip URL (GET)")
                style { width(100.percent) }
                onInput { url = it.value }
            }

            Div({
                style {
                    display(DisplayStyle.Flex)
                    gap(.5.r)
                    width(100.percent)
                }
            }) {
                Input(InputType.Text) {
                    classes(Styles.textarea)
                    value(prefix)
                    placeholder("Prefix (e.g. $)")
                    style { flex(1) }
                    onInput { prefix = it.value }
                }

                Select({
                    classes(Styles.textarea)
                    style { flex(2) }
                    onChange { event ->
                        event.value?.let { selectedVal ->
                            runCatching { ChipFormatType.valueOf(selectedVal) }.getOrNull()?.let {
                                format = it
                            }
                        }
                    }
                }) {
                    ChipFormatType.entries.forEach { ft ->
                        Option(
                            value = ft.name,
                            attrs = {
                                if (ft == format) {
                                    selected()
                                }
                            },
                        ) {
                            Text(ft.name)
                        }
                    }
                }
            }

            if (format == ChipFormatType.JSON) {
                Input(InputType.Text) {
                    classes(Styles.textarea)
                    value(jsonPath)
                    placeholder("JSON Path (e.g. messages.0.text)")
                    style { width(100.percent) }
                    onInput { jsonPath = it.value }
                }
            }

            // Preset swatches + Custom Status Color Picker (<input type="color">)
            val isPreset = chipPastelHexColors.any { it.equals(colorHex, ignoreCase = true) }

            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexWrap(FlexWrap.Wrap)
                    alignItems(AlignItems.Center)
                    gap(.5.r)
                }
            }) {
                chipPastelHexColors.forEach { presetHex ->
                    val selectedSwatch = presetHex.equals(colorHex, ignoreCase = true)
                    val parsed = parseChipHexColor(presetHex)
                    Div({
                        style {
                            width(28.px)
                            height(28.px)
                            borderRadius(999.px)
                            if (parsed != null) {
                                backgroundColor(rgb(parsed.r, parsed.g, parsed.b))
                            }
                            border(
                                if (selectedSwatch) 3.px else 1.px,
                                LineStyle.Solid,
                                if (selectedSwatch) Styles.colors.primary else Color("rgba(0,0,0,0.2)"),
                            )
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.Center)
                            cursor("pointer")
                        }
                        onClick {
                            colorHex = presetHex
                        }
                    }) {
                        if (selectedSwatch) {
                            Icon("check") {
                                fontSize(16.px)
                                color(Styles.colors.primary)
                            }
                        }
                    }
                }

                // Status Color Picker swatch
                val customParsed = parseChipHexColor(colorHex)
                Div({
                    style {
                        position(Position.Relative)
                        width(28.px)
                        height(28.px)
                        borderRadius(999.px)
                        if (customParsed != null) {
                            backgroundColor(rgb(customParsed.r, customParsed.g, customParsed.b))
                        }
                        border(
                            if (!isPreset) 3.px else 1.px,
                            LineStyle.Solid,
                            if (!isPreset) Styles.colors.primary else Styles.colors.outline,
                        )
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        justifyContent(JustifyContent.Center)
                        cursor("pointer")
                        property("overflow", "hidden")
                    }
                    title("Custom color")
                }) {
                    Icon("palette") {
                        fontSize(16.px)
                        color(if (!isPreset) Styles.colors.primary else Styles.colors.secondary)
                        property("pointer-events", "none")
                    }
                    Input(InputType.Color) {
                        value(colorHex.lowercase())
                        style {
                            position(Position.Absolute)
                            width(100.percent)
                            height(100.percent)
                            opacity(0)
                            cursor("pointer")
                        }
                        onInput {
                            colorHex = it.value.uppercase()
                        }
                    }
                }
            }

            Div({
                style {
                    display(DisplayStyle.Flex)
                    justifyContent(JustifyContent.FlexEnd)
                    gap(.5.r)
                }
            }) {
                Button({
                    classes(Styles.textButton)
                    if (isSaving) disabled()
                    onClick {
                        isEditing = false
                        onFinished()
                    }
                }) {
                    Text(application.appString { cancel })
                }
                Button({
                    classes(Styles.button)
                    if (isSaving) disabled()
                    onClick {
                        if (isSaving) return@onClick
                        isSaving = true
                        scope.launch {
                            chips.update(liveChip)
                            isSaving = false
                            isEditing = false
                            onFinished()
                        }
                    }
                }) {
                    Text(if (isSaving) "Saving..." else "Save changes")
                }
            }
        }
    }
}

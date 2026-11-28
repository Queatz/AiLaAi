package app.chips

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.compose.rememberDarkMode
import app.signals.signals
import com.queatz.db.ChipFormatType
import com.queatz.db.ChipValueKind
import com.queatz.db.PersonChip
import com.queatz.db.computeChipGradientPalette
import com.queatz.db.evaluateChipDisplay
import com.queatz.db.resolveChipBaseHex
import com.queatz.push.SignalPushData
import com.queatz.push.SignalReplyPushData
import components.Icon
import ellipsize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.rgb
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import push
import r
import kotlin.time.Duration.Companion.seconds

@Composable
fun ChipItem(
    chip: PersonChip,
    value: String,
    legacyBackgroundColor: Int? = null,
    onClick: () -> Unit = {},
    onContextMenuClick: (() -> Unit)? = null,
) {
    ensureChipGradientKeyframes()

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

    val fillGradient = remember(palette) {
        val s = palette.stops.map { "rgb(${it.r}, ${it.g}, ${it.b})" }
        "linear-gradient(135deg, ${s[0]}, ${s[1]}, ${s[2]}, ${s[3]})"
    }
    val borderGradient = remember(palette) {
        val b = palette.borderStops.map { "rgb(${it.r}, ${it.g}, ${it.b})" }
        "linear-gradient(135deg, ${b[0]}, ${b[1]}, ${b[2]}, ${b[3]})"
    }

    val fgCss = if (palette.useLightForeground) rgb(255, 255, 255) else rgb(26, 26, 26)

    val iconName: String? = when (evaluated.kind) {
        ChipValueKind.BooleanTrue -> "toggle_on"
        ChipValueKind.BooleanFalse -> "toggle_off"
        ChipValueKind.Null -> "remove_circle_outline"
        ChipValueKind.Undefined -> "help_outline"
        ChipValueKind.Normal -> null
    }

    val iconColorCss = when (evaluated.kind) {
        ChipValueKind.BooleanTrue -> if (palette.useLightForeground) rgb(185, 246, 202) else rgb(27, 94, 32)
        ChipValueKind.BooleanFalse -> if (palette.useLightForeground) rgb(224, 224, 224) else rgb(97, 97, 97)
        ChipValueKind.Null -> if (palette.useLightForeground) rgb(224, 224, 224) else rgb(97, 97, 97)
        ChipValueKind.Undefined -> if (palette.useLightForeground) rgb(255, 204, 128) else rgb(230, 81, 0)
        ChipValueKind.Normal -> fgCss
    }

    val prefixToDisplay = evaluated.prefix.take(100)
    val remainingChars = (100 - prefixToDisplay.length).coerceAtLeast(0)
    val textToDisplay = evaluated.text.take(remainingChars)

    Div({
        classes(ChipStyles.pill)
        style {
            property("background-image", "$fillGradient, $borderGradient")
            property("background-origin", "padding-box, border-box")
            property("background-clip", "padding-box, border-box")
            color(fgCss)
        }
        onClick {
            it.stopPropagation()
            onClick()
        }
        if (onContextMenuClick != null) {
            addEventListener("contextmenu") { event ->
                event.preventDefault()
                event.stopPropagation()
                onContextMenuClick()
            }
        }
    }) {
        if (iconName == null) {
            Span({
                style {
                    maxWidth(240.px)
                    ellipsize()
                }
            }) {
                Text("$prefixToDisplay$textToDisplay")
            }
        } else {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    gap(.25.r)
                }
            }) {
                if (prefixToDisplay.isNotEmpty()) {
                    Span { Text(prefixToDisplay) }
                }
                Icon(iconName) {
                    fontSize(16.px)
                    color(iconColorCss)
                }
                if (textToDisplay.isNotEmpty()) {
                    Span { Text(textToDisplay) }
                }
            }
        }
    }
}

@Composable
fun SignalsButton(
    onClick: () -> Unit,
    onContextMenuClick: (() -> Unit)? = null,
) {
    ensureChipGradientKeyframes()

    val activeSignals by signals.activeSignals.collectAsState()
    val count = activeSignals?.let { it.mine.size + it.others.size } ?: 0
    val isDark = rememberDarkMode()

    LaunchedEffect(Unit) {
        while (true) {
            signals.reloadActive()
            delay(30.seconds)
        }
    }

    LaunchedEffect(Unit) {
        push.events
            .filter { it.data is SignalPushData || it.data is SignalReplyPushData }
            .collect {
                signals.reloadActive()
            }
    }

    val fillGradient = if (isDark) {
        "linear-gradient(135deg, ${Styles.colors.primary}, ${Styles.colors.tertiary}, ${Styles.colors.secondary}, ${Styles.colors.primary})"
    } else {
        "linear-gradient(135deg, #d7e3ff, #f6d9ff, #d9f2ee, #d7e3ff)"
    }

    val borderGradient = if (isDark) {
        "linear-gradient(135deg, ${Styles.colors.secondary}, ${Styles.colors.primary}, ${Styles.colors.tertiary}, ${Styles.colors.secondary})"
    } else {
        "linear-gradient(135deg, #b8d8d3, #b8ccff, #eab8ff, #b8d8d3)"
    }

    val text = when (count) {
        0 -> "Signals"
        1 -> activeSignals?.mine?.firstOrNull()?.signal?.name
            ?: activeSignals?.others?.firstOrNull()?.signal?.name
            ?: "1 signal"
        else -> "$count signals"
    }

    Div({
        classes(ChipStyles.pill)
        style {
            property("background-image", "$fillGradient, $borderGradient")
            property("background-origin", "padding-box, border-box")
            property("background-clip", "padding-box, border-box")
            color(if (isDark) Styles.colors.white else rgb(24, 28, 36))
            maxWidth(200.px)
        }
        onClick {
            it.stopPropagation()
            onClick()
        }
        if (onContextMenuClick != null) {
            addEventListener("contextmenu") { event ->
                event.preventDefault()
                event.stopPropagation()
                onContextMenuClick()
            }
        }
    }) {
        Span({
            style {
                ellipsize()
            }
        }) {
            Text(text)
        }
    }
}

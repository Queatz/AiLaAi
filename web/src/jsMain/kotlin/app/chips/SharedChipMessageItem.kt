package app.chips

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.dialog.dialog
import application
import com.queatz.db.ChipAttachment
import com.queatz.db.ChipFormatType
import com.queatz.db.PersonChip
import com.queatz.db.defaultChipHexColor
import com.queatz.db.legacyColorIntToHex
import com.queatz.db.parseChipHexColor
import components.Icon
import ellipsize
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

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
fun SharedChipMessageItem(
    attachment: ChipAttachment,
) {
    val scope = rememberCoroutineScope()
    val myItems by chips.items.collectAsState()
    var liveValue by remember(attachment.url) { mutableStateOf<String?>(null) }

    val previewChip = remember(attachment) { attachment.toPersonChip() }

    LaunchedEffect(attachment.url) {
        val targetUrl = attachment.url?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val preview = chips.previewUrl(targetUrl)
        if (!preview?.value.isNullOrBlank()) {
            liveValue = preview.value
        }
    }

    val isAlreadyAdded = myItems.any {
        it.chip.url == (attachment.url ?: "") && it.chip.name == (attachment.name ?: "")
    }

    fun openInstall() {
        scope.launch {
            val confirmed = dialog(
                title = "Add to Chips",
                confirmButton = "Add Chip",
                cancelButton = application.appString { cancel },
            ) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        alignItems(AlignItems.Center)
                        gap(1.r)
                        padding(.5.r, 0.r)
                    }
                }) {
                    ChipItem(
                        chip = previewChip,
                        value = liveValue ?: attachment.name?.takeIf { it.isNotBlank() } ?: "...",
                        legacyBackgroundColor = attachment.backgroundColor,
                    )
                    if (!attachment.url.isNullOrBlank()) {
                        Div({
                            style {
                                fontSize(12.px)
                                opacity(0.7)
                                maxWidth(22.r)
                                ellipsize()
                            }
                        }) {
                            Text(attachment.url!!)
                        }
                    }
                }
            }
            if (confirmed == true) {
                chips.create(previewChip)
            }
        }
    }

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            gap(.75.r)
            padding(1.r)
            borderRadius(1.r)
            backgroundColor(Styles.colors.surface)
            border(1.px, LineStyle.Solid, Styles.colors.outline)
            maxWidth(20.r)
        }
    }) {
        Div({
            style {
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                gap(.5.r)
            }
        }) {
            Icon("tune") {
                color(Styles.colors.primary)
                fontSize(18.px)
            }
            Span({
                style {
                    fontWeight(600)
                    ellipsize()
                }
            }) {
                Text(attachment.name?.takeIf { it.isNotBlank() } ?: "Shared Chip")
            }
        }

        Div({
            style {
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.Center)
            }
        }) {
            ChipItem(
                chip = previewChip,
                value = liveValue ?: attachment.name?.takeIf { it.isNotBlank() } ?: "...",
                legacyBackgroundColor = attachment.backgroundColor,
                onClick = { openInstall() },
            )
        }

        Div({
            style {
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.FlexEnd)
            }
        }) {
            Button({
                classes(Styles.button)
                onClick { openInstall() }
            }) {
                Icon(if (isAlreadyAdded) "check" else "add") {
                    fontSize(16.px)
                }
                Span({ style { padding(0.r, .25.r) } }) {
                    Text(if (isAlreadyAdded) "Added" else "Add Chip")
                }
            }
        }
    }
}

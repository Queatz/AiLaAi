package app.chips

import LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import application
import com.queatz.push.ChipPushData
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement
import push
import kotlin.math.roundToInt

@Composable
fun TopChipsRow(
    onOpenSignals: () -> Unit,
) {
    val me by application.me.collectAsState()
    val items by chips.items.collectAsState()
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    val aboveItems = remember(items) { items.filter { it.chip.isAbove != false } }
    val belowItems = remember(items) { items.filter { it.chip.isAbove == false } }

    var viewportEl by remember { mutableStateOf<HTMLDivElement?>(null) }
    var signalsWrapperEl by remember { mutableStateOf<HTMLDivElement?>(null) }

    LaunchedEffect(me?.id) {
        if (me?.id != null) {
            chips.reload()
        } else {
            chips.clear()
        }
    }

    LaunchedEffect(Unit) {
        push.events
            .filter { it.data is ChipPushData }
            .collect {
                chips.reload()
            }
    }

    val centerKey = remember(items) {
        items.map { Triple(it.chip.id, it.chip.isAbove, it.value != null) }
    }

    LaunchedEffect(centerKey, viewportEl, signalsWrapperEl) {
        val vp = viewportEl ?: return@LaunchedEffect
        val sig = signalsWrapperEl ?: return@LaunchedEffect
        val maxScroll = vp.scrollWidth - vp.clientWidth
        if (maxScroll > 0) {
            val sigCenter = sig.offsetLeft + sig.offsetWidth / 2.0
            val desired = (sigCenter - vp.clientWidth / 2.0).roundToInt()
            vp.scrollLeft = desired.coerceIn(0, maxScroll).toDouble()
        }
    }

    fun openSettingsOverlay() {
        scope.launch {
            topChipsOverlayDialog(
                onOpenSignals = onOpenSignals,
            )
        }
    }

    if (me == null) return

    Div({
        classes(ChipStyles.rowViewport, "ailaai-no-scrollbar")
        ref { el ->
            viewportEl = el
            onDispose { viewportEl = null }
        }
        addEventListener("contextmenu") { event ->
            event.preventDefault()
            openSettingsOverlay()
        }
    }) {
        Div({
            classes(ChipStyles.rowContent)
        }) {
            aboveItems.forEach { ext ->
                ChipItem(
                    chip = ext.chip,
                    value = ext.value ?: "...",
                    onClick = {
                        val id = ext.chip.id ?: return@ChipItem
                        scope.launch {
                            chipDetailsDialog(
                                chipId = id,
                                configuration = configuration,
                            )
                        }
                    },
                    onContextMenuClick = { openSettingsOverlay() },
                )
            }

            Div({
                ref { el ->
                    signalsWrapperEl = el
                    onDispose { signalsWrapperEl = null }
                }
            }) {
                // Clicking Signals pill opens Chips Settings on Web (full parity),
                // and inside Chips Settings (or via Open Signals) navigates to Signals!
                SignalsButton(
                    onClick = { openSettingsOverlay() },
                    onContextMenuClick = { onOpenSignals() },
                )
            }

            belowItems.forEach { ext ->
                ChipItem(
                    chip = ext.chip,
                    value = ext.value ?: "...",
                    onClick = {
                        val id = ext.chip.id ?: return@ChipItem
                        scope.launch {
                            chipDetailsDialog(
                                chipId = id,
                                configuration = configuration,
                            )
                        }
                    },
                    onContextMenuClick = { openSettingsOverlay() },
                )
            }
        }
    }
}

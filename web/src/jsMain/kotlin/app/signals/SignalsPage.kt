package app.signals

import LocalConfiguration
import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.AppNavigation
import app.appNav
import app.chips.ensureChipGradientKeyframes
import application
import com.queatz.db.Signal
import com.queatz.db.SignalSendExtended
import com.queatz.push.SignalPushData
import com.queatz.push.SignalReplyPushData
import components.Icon
import components.Loading
import ellipsize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
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
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.gridTemplateColumns
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import push
import r
import kotlin.js.Date
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@Composable
fun SignalsPage() {
    ensureChipGradientKeyframes()

    val me by application.me.collectAsState()
    val activeSignals by signals.activeSignals.collectAsState()
    val personSignals by signals.personSignals.collectAsState()
    val catalog by signals.catalog.collectAsState()
    val navFilter by signals.navFilter.collectAsState()
    val selectedCategory by signals.selectedCategory.collectAsState()
    val searchText by signals.searchText.collectAsState()

    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    var isLoading by remember { mutableStateOf(catalog.isEmpty()) }
    var canLoadMore by remember { mutableStateOf(true) }
    var burstEmoji by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        signals.reloadActive(geo = me?.geo)
        signals.reloadMySignals()
        canLoadMore = signals.loadCatalog(offset = 0, reset = true)
        isLoading = false
    }

    LaunchedEffect(Unit) {
        push.events
            .filter { it.data is SignalPushData || it.data is SignalReplyPushData }
            .collect {
                signals.reloadActive(geo = me?.geo)
            }
    }

    val turnedOnIds = remember(personSignals) {
        personSignals.filter { it.turnedOn == true }.mapNotNull { it.signal }.toSet()
    }

    val filteredSignals = remember(catalog, turnedOnIds, navFilter, selectedCategory, searchText) {
        catalog.filter { sig ->
            val matchesFilter = when (navFilter) {
                SignalsNavFilter.All -> true
                SignalsNavFilter.MineOn -> sig.id in turnedOnIds
                SignalsNavFilter.Active -> false
            }
            val matchesSearch = searchText.isBlank() || sig.name?.contains(searchText.trim(), ignoreCase = true) == true
            val matchesCategory = selectedCategory == null || sig.categories?.contains(selectedCategory) == true
            matchesFilter && matchesSearch && matchesCategory
        }
    }

    val mineActive = remember(activeSignals, selectedCategory) {
        activeSignals?.mine.orEmpty().filter {
            selectedCategory == null || it.signal?.categories?.contains(selectedCategory) == true
        }
    }
    val othersActive = remember(activeSignals, selectedCategory) {
        activeSignals?.others.orEmpty().filter {
            selectedCategory == null || it.signal?.categories?.contains(selectedCategory) == true
        }
    }

    val greeting = remember {
        val hour = Date().getHours()
        when {
            hour < 5 -> "Good night"
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Div({
        style {
            position(Position.Relative)
            width(100.percent)
            height(100.percent)
            overflowY("auto")
            padding(2.r)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            alignItems(AlignItems.Center)
            property("box-sizing", "border-box")
        }
    }) {
        Div({
            style {
                width(100.percent)
                maxWidth(56.r)
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(1.5.r)
            }
        }) {
            // Soft animated gradient greeting matching Android SignalsGreeting
            Div({
                style {
                    fontSize(36.px)
                    fontWeight(300)
                    textAlign("center")
                    padding(1.r, 0.r)
                    property(
                        "background-image",
                        "linear-gradient(135deg, ${Styles.colors.primary}, ${Styles.colors.tertiary}, ${Styles.colors.secondary}, ${Styles.colors.primary})",
                    )
                    property("background-size", "220% 220%")
                    property("animation", "ailaaiSoftGradientShift 20s linear infinite")
                    property("-webkit-background-clip", "text")
                    property("background-clip", "text")
                    property("color", "transparent")
                }
            }) {
                Text(greeting)
            }

            if (isLoading && activeSignals == null) {
                Loading()
            }

            // Active Now section
            if (mineActive.isNotEmpty() || othersActive.isNotEmpty() || navFilter == SignalsNavFilter.Active) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        gap(.75.r)
                    }
                }) {
                    Div({
                        style {
                            fontSize(15.px)
                            fontWeight(600)
                        }
                    }) {
                        Text("Active Now")
                    }

                    if (mineActive.isEmpty() && othersActive.isEmpty()) {
                        Div({ style { opacity(0.65); fontSize(14.px) } }) {
                            Text("No active signals right now.")
                        }
                    } else {
                        Div({
                            style {
                                display(DisplayStyle.Grid)
                                gridTemplateColumns("repeat(auto-fill, minmax(260px, 1fr))")
                                gap(1.r)
                            }
                        }) {
                            mineActive.forEach { send ->
                                WebActiveSignalCard(
                                    send = send,
                                    isMine = true,
                                    onClick = {
                                        scope.launch {
                                            when (val res = signalRepliesDialog(send)) {
                                                is SignalRepliesResult.Cancelled -> signals.reloadActive(geo = me?.geo)
                                                is SignalRepliesResult.CreatedGroup -> {
                                                    signals.reloadActive(geo = me?.geo)
                                                    res.group.id?.let { gid ->
                                                        appNav.navigate(AppNavigation.Group(gid))
                                                    }
                                                }
                                                null -> {}
                                            }
                                        }
                                    },
                                )
                            }
                            othersActive.forEach { send ->
                                WebActiveSignalCard(
                                    send = send,
                                    isMine = false,
                                    onClick = {
                                        scope.launch {
                                            val ok = replySignalDialog(send)
                                            if (ok) {
                                                burstEmoji = send.signal?.emoji ?: "👋"
                                                signals.reloadActive(geo = me?.geo)
                                                delay(1.seconds)
                                                burstEmoji = null
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Signal Bubbles section
            if (navFilter != SignalsNavFilter.Active) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        justifyContent(JustifyContent.SpaceBetween)
                    }
                }) {
                    Span({ style { fontSize(15.px); fontWeight(600) } }) {
                        Text(
                            if (navFilter == SignalsNavFilter.MineOn) {
                                "My Signals (click to send, right-click to turn off)"
                            } else {
                                "Send a Signal (click to turn on / send, right-click to turn off)"
                            }
                        )
                    }
                }

                if (filteredSignals.isEmpty() && !isLoading) {
                    Div({ style { opacity(0.65); fontSize(14.px) } }) {
                        Text(
                            if (navFilter == SignalsNavFilter.MineOn) {
                                "You haven't turned on any signals yet — switch to All Signals to turn some on!"
                            } else {
                                "No signals found."
                            }
                        )
                    }
                } else {
                    Div({
                        style {
                            display(DisplayStyle.Grid)
                            gridTemplateColumns("repeat(auto-fill, minmax(116px, 1fr))")
                            gap(1.r)
                        }
                    }) {
                        filteredSignals.forEach { sig ->
                            val isOn = sig.id in turnedOnIds
                            WebSignalBubble(
                                signal = sig,
                                isOn = isOn,
                                onClick = {
                                    val sigId = sig.id ?: return@WebSignalBubble
                                    if (!isOn) {
                                        scope.launch {
                                            signals.toggle(sigId)
                                        }
                                    } else {
                                        scope.launch {
                                            val sent = sendSignalDialog(
                                                signal = sig,
                                                geo = me?.geo,
                                                configuration = configuration,
                                            )
                                            if (sent) {
                                                burstEmoji = sig.emoji ?: "👋"
                                                signals.reloadActive(geo = me?.geo)
                                                delay(1.seconds)
                                                burstEmoji = null
                                            }
                                        }
                                    }
                                },
                                onTurnOff = {
                                    val sigId = sig.id ?: return@WebSignalBubble
                                    if (isOn) {
                                        scope.launch {
                                            signals.toggle(sigId)
                                        }
                                    }
                                },
                            )
                        }
                    }

                    if (canLoadMore && navFilter == SignalsNavFilter.All && searchText.isBlank()) {
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                justifyContent(JustifyContent.Center)
                                padding(1.r)
                            }
                        }) {
                            Button({
                                classes(Styles.outlineButton)
                                onClick {
                                    scope.launch {
                                        canLoadMore = signals.loadCatalog(offset = catalog.size, reset = false)
                                    }
                                }
                            }) {
                                Text("Load more")
                            }
                        }
                    }
                }
            }
        }

        // Celebratory burst overlay
        burstEmoji?.let { emoji ->
            Div({
                style {
                    position(Position.Fixed)
                    top(0.px)
                    left(0.px)
                    right(0.px)
                    height(100.percent)
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    justifyContent(JustifyContent.Center)
                    property("pointer-events", "none")
                    property("z-index", "200")
                }
            }) {
                Div({
                    style {
                        fontSize(84.px)
                        property("animation", "ailaaiSignalBurst 1s ease-out forwards")
                    }
                }) {
                    Text(emoji)
                }
            }
        }
    }
}

@Composable
private fun WebActiveSignalCard(
    send: SignalSendExtended,
    isMine: Boolean,
    onClick: () -> Unit,
) {
    val now = Clock.System.now()
    val totalMs = if (send.signalSend.expiry != null && send.signalSend.createdAt != null) {
        (send.signalSend.expiry!! - send.signalSend.createdAt!!).inWholeMilliseconds
    } else {
        1L
    }
    val remMs = ((send.signalSend.expiry ?: now) - now).inWholeMilliseconds.coerceAtLeast(0L)
    val pct = if (totalMs > 0L) ((remMs.toDouble() / totalMs.toDouble()) * 100.0).coerceIn(0.0, 100.0) else 0.0

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            gap(.5.r)
            padding(1.r)
            borderRadius(1.r)
            backgroundColor(Styles.colors.surface)
            border(1.px, LineStyle.Solid, if (isMine) Styles.colors.primary else Styles.colors.outline)
            cursor("pointer")
        }
        onClick { onClick() }
    }) {
        Div({
            style {
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                gap(.5.r)
            }
        }) {
            Span({ style { fontSize(26.px) } }) {
                Text(send.signal?.emoji ?: "👋")
            }
            Span({
                style {
                    fontWeight(600)
                    fontSize(15.px)
                    ellipsize()
                }
            }) {
                Text(send.signal?.name.orEmpty())
            }
        }

        if (isMine) {
            val replyCount = send.replies?.size ?: 0
            Div({
                style {
                    fontSize(12.px)
                    color(Styles.colors.primary)
                    fontWeight(600)
                }
            }) {
                val suffix = if (replyCount > 0) {
                    " • $replyCount ${if (replyCount == 1) "reply" else "replies"}"
                } else {
                    ""
                }
                Text("Your signal$suffix")
            }
        } else {
            Div({
                style {
                    fontSize(12.px)
                    color(Styles.colors.secondary)
                }
            }) {
                Text(send.person?.name ?: "Someone")
            }
        }

        if (!send.signalSend.message.isNullOrBlank()) {
            Div({
                style {
                    fontSize(13.px)
                    ellipsize()
                }
            }) {
                Text(send.signalSend.message!!)
            }
        }

        WebSignalAttachments(
            photo = send.signalSend.photo,
            audio = send.signalSend.audio,
        )

        Div({
            style {
                width(100.percent)
                height(5.px)
                borderRadius(999.px)
                backgroundColor(Styles.colors.outline)
                property("overflow", "hidden")
            }
        }) {
            Div({
                style {
                    width(pct.percent)
                    height(100.percent)
                    backgroundColor(Styles.colors.primary)
                }
            }) {}
        }
    }
}

@Composable
private fun WebSignalBubble(
    signal: Signal,
    isOn: Boolean,
    onClick: () -> Unit,
    onTurnOff: () -> Unit,
) {
    Div({
        style {
            position(Position.Relative)
            property("aspect-ratio", "1 / 1")
            borderRadius(999.px)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            alignItems(AlignItems.Center)
            justifyContent(JustifyContent.Center)
            padding(.75.r)
            cursor("pointer")
            backgroundColor(Styles.colors.surface)
            border(
                if (isOn) 3.px else 1.px,
                LineStyle.Solid,
                if (isOn) Styles.colors.primary else Styles.colors.outline,
            )
            property("transition", "transform 0.15s ease, border-color 0.15s ease")
            property("box-sizing", "border-box")
            property("user-select", "none")
            if (!isOn) {
                opacity(0.82)
            }
        }
        onClick { onClick() }
        addEventListener("contextmenu") { event ->
            event.preventDefault()
            onTurnOff()
        }
    }) {
        if (isOn) {
            Span({
                style {
                    position(Position.Absolute)
                    top(6.px)
                    right(6.px)
                    width(20.px)
                    height(20.px)
                    borderRadius(999.px)
                    backgroundColor(Styles.colors.primary)
                    color(Styles.colors.white)
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    justifyContent(JustifyContent.Center)
                }
                title("Turn signal off")
                onClick {
                    it.stopPropagation()
                    onTurnOff()
                }
            }) {
                Icon("check") { fontSize(13.px) }
            }
        }

        Div({ style { fontSize(28.px) } }) {
            Text(signal.emoji ?: "❓")
        }
        Div({
            style {
                fontSize(12.px)
                fontWeight(600)
                textAlign("center")
                maxWidth(100.percent)
                ellipsize()
            }
        }) {
            Text(signal.name.orEmpty())
        }
        if (!signal.categories.isNullOrEmpty()) {
            Div({
                style {
                    fontSize(10.px)
                    opacity(0.65)
                    textAlign("center")
                    maxWidth(100.percent)
                    ellipsize()
                }
            }) {
                Text(signal.categories!!.joinToString(", "))
            }
        }
    }
}

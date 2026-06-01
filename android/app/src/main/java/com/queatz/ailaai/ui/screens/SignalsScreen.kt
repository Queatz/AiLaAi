package com.queatz.ailaai.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ailaai.api.*
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.push
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.PageInput
import com.queatz.ailaai.ui.components.SearchFieldAndAction
import com.queatz.ailaai.ui.dialogs.*
import com.queatz.ailaai.ui.state.latLngSaver
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.*
import com.queatz.push.SignalPushData
import com.queatz.push.SignalReplyPushData
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Clock

@Composable
fun SignalsScreen(id: String? = null) {
    val nav = nav
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var signals by remember { mutableStateOf(emptyList<Signal>()) }
    var personSignals by remember { mutableStateOf(emptyList<PersonSignal>()) }
    var activeSignals by remember { mutableStateOf<ActiveSignalsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var signalsOffset by remember { mutableIntStateOf(0) }
    var canLoadMore by remember { mutableStateOf(true) }

    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf(emptyList<String>()) }

    var showSendDialog by remember { mutableStateOf<Signal?>(null) }
    var showReplyDialog by remember { mutableStateOf<SignalSendExtended?>(null) }
    var showRepliesDialog by remember { mutableStateOf<SignalSendExtended?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var signalSentAnimation by remember { mutableStateOf<Signal?>(null) }
    var hasOpenedDeepLink by rememberSaveable { mutableStateOf(false) }

    var geo: LatLng? by rememberSaveable(stateSaver = latLngSaver()) { mutableStateOf(null) }
    val locationSelector = locationSelector(
        geo = geo,
        onGeoChange = { geo = it },
        activity = nav.context as Activity
    )

    var myId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        hasOpenedDeepLink = false
    }

    fun refreshActive() {
        scope.launch {
            api.activeSignals(geo?.toList(), onError = { isLoading = false }) {
                activeSignals = it
                isLoading = false
                
                // If we came from a deep link, open the correct dialog
                if (id != null && !hasOpenedDeepLink) {
                    val signalSend = it.mine.find { it.signalSend.id == id } ?: it.others.find { it.signalSend.id == id }
                    if (signalSend != null) {
                        hasOpenedDeepLink = true
                        if (signalSend.signalSend.person == myId) {
                            showRepliesDialog = signalSend
                        } else {
                            showReplyDialog = signalSend
                        }
                    }
                }
            }
        }
    }

    fun loadSignals(reset: Boolean = false) {
        if (reset) {
            signalsOffset = 0
            canLoadMore = true
        }

        if (!canLoadMore || (isLoading && !reset)) {
            return
        }

        isLoading = true
        scope.launch {
            api.signals(
                localHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
                offset = signalsOffset,
                onError = { isLoading = false }
            ) {
                signals = if (reset) it else signals + it
                signalsOffset = signals.size
                canLoadMore = it.size >= 20
                categories = signals.flatMap { it.categories ?: emptyList() }.distinct().sorted()
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        api.me { myId = it.id }
        loadSignals(true)
        api.mySignals { personSignals = it }
    }

    LaunchedEffect(geo) {
        refreshActive()
    }

    LaunchedEffect(push) {
        push.events
            .filter { it is SignalPushData || it is SignalReplyPushData }
            .collect {
                refreshActive()
            }
    }

    val filteredSignals = remember(signals, searchText, selectedCategory) {
        signals.filter { signal ->
            (searchText.isBlank() || signal.name?.contains(searchText, true) == true) &&
                    (selectedCategory == null || signal.categories?.contains(selectedCategory) == true)
        }
    }

    var h by rememberStateOf(80.dp.px)
    val gridState = rememberLazyGridState()

    val isMobile = LocalWindowInfo.current.containerDpSize.width < 600.dp

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(
            title = stringResource(R.string.signals),
            onTitleClick = {},
            navigationIcon = {
                BackButton()
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(100.dp),
                contentPadding = PaddingValues(
                    start = 1.pad,
                    top = 1.pad,
                    end = 1.pad,
                    bottom = 3.pad + h.inDp()
                ),
                horizontalArrangement = Arrangement.spacedBy(1.pad),
                verticalArrangement = Arrangement.spacedBy(1.pad),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SignalsGreeting()
                }

                activeSignals?.let { active ->
                    val mineFiltered = active.mine.filter { send ->
                        selectedCategory == null || send.signal?.categories?.contains(selectedCategory) == true
                    }
                    val othersFiltered = active.others.filter { send ->
                        selectedCategory == null || send.signal?.categories?.contains(selectedCategory) == true
                    }

                    if (mineFiltered.isNotEmpty() || othersFiltered.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = stringResource(R.string.active_now),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 1.pad, vertical = 1.pad)
                            )
                        }
                        items(mineFiltered, span = { GridItemSpan(if (isMobile) maxLineSpan else 1) }) { send ->
                            ActiveSignalCard(
                                send = send,
                                isMine = true,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    showRepliesDialog = send
                                }
                            )
                        }
                        items(othersFiltered, span = { GridItemSpan(if (isMobile) maxLineSpan else 1) }) { send ->
                            ActiveSignalCard(
                                send = send,
                                isMine = false,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    showReplyDialog = send
                                }
                            )
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.send_a_signal),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 1.pad, vertical = 1.pad)
                    )
                }

                if (filteredSignals.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.no_signals),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 1.pad, vertical = 1.pad)
                        )
                    }
                    return@LazyVerticalGrid
                }

                items(filteredSignals) { signal ->
                    val personSignal = personSignals.find { it.signal == signal.id }
                    val isOn = personSignal?.turnedOn == true

                    SignalBubble(signal, isOn = isOn, onLongClick = {
                        if (isOn) {
                            scope.launch {
                                api.toggleSignal(signal.id!!) {
                                    personSignals = personSignals.filter { it.signal != signal.id } + it
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(nav.context, nav.context.getString(R.string.signal_turned_off, signal.name), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        if (!isOn) {
                            scope.launch {
                                api.toggleSignal(signal.id!!) {
                                    personSignals = personSignals.filter { it.signal != signal.id } + it
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(nav.context, nav.context.getString(R.string.signal_turned_on, signal.name), Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            showSendDialog = signal
                        }
                    }
                }

                if (canLoadMore && signals.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(1.pad),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }

                        LaunchedEffect(Unit) {
                            loadSignals()
                        }
                    }
                }
            }

            if (isLoading && activeSignals == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }

            PageInput(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onPlaced {
                        if (!gridState.isScrollInProgress) {
                            h = it.size.height
                        }
                    }
            ) {
                SearchContent(
                    locationSelector = locationSelector,
                    isLoading = isLoading,
                    categories = categories,
                    category = selectedCategory
                ) {
                    selectedCategory = it
                }
                SearchFieldAndAction(
                    value = searchText,
                    valueChange = { searchText = it },
                    placeholder = stringResource(R.string.search),
                    action = {
                        Row {
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(Icons.Outlined.Settings, null)
                            }
                            IconButton(onClick = { showCreateDialog = true }) {
                                Icon(Icons.Outlined.Add, null)
                            }
                        }
                    }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateSignalDialog(
            onDismissRequest = { showCreateDialog = false },
            onSubmit = { signal ->
                scope.launch {
                    api.createSignal(signal) {
                        signals = (signals + it).sortedBy { it.name }
                        categories = signals.flatMap { it.categories ?: emptyList() }.distinct().sorted()
                        showCreateDialog = false
                    }
                }
            }
        )
    }

    if (showSettingsDialog) {
        SignalSettingsDialog(
            onDismissRequest = { showSettingsDialog = false }
        )
    }

    showSendDialog?.let { signal ->
        SendSignalDialog(
            signal = signal,
            geo = geo,
            onDismissRequest = { showSendDialog = null },
            onSubmit = { body ->
                scope.launch {
                    api.sendSignal(body, onError = { context.showDidntWork() }) {
                        showSendDialog = null
                        signalSentAnimation = signal
                        refreshActive()
                    }
                }
            }
        )
    }

    showReplyDialog?.let { send ->
        ReplySignalDialog(
            signalSend = send,
            onDismissRequest = {
                showReplyDialog = null
                if (id == send.signalSend.id) {
                    nav.popBackStack()
                }
            },
            onSubmit = { body ->
                scope.launch {
                    api.replySignal(send.signalSend.id!!, body, onError = { context.showDidntWork() }) {
                        showReplyDialog = null
                        if (id == send.signalSend.id) {
                            nav.popBackStack()
                        }
                        signalSentAnimation = send.signal
                        refreshActive()
                    }
                }
            }
        )
    }

    showRepliesDialog?.let { send ->
        SignalRepliesDialog(
            signalSend = send,
            onDismissRequest = {
                showRepliesDialog = null
                if (id == send.signalSend.id) {
                    nav.popBackStack()
                }
            },
            onCancelSignal = {
                scope.launch {
                    api.cancelSignal(send.signalSend.id!!) {
                        showRepliesDialog = null
                        if (id == send.signalSend.id) {
                            nav.popBackStack()
                        }
                        refreshActive()
                    }
                }
            },
            onCreateGroup = { people ->
                scope.launch {
                    api.createSignalGroup(send.signalSend.id!!, people) { group: Group ->
                        showRepliesDialog = null
                        if (id == send.signalSend.id) {
                            nav.popBackStack()
                        }
                        nav.appNavigate(AppNav.Group(group.id!!))
                    }
                }
            }
        )
    }

    signalSentAnimation?.let { signal ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val animatable = remember { Animatable(1f) }

            LaunchedEffect(Unit) {
                animatable.animateTo(5f, animationSpec = tween(durationMillis = 1000))
                signalSentAnimation = null
            }

            val alpha = (1f - (animatable.value - 1f) / 4f).coerceIn(0f, 1f)
            val emoji = signal.emoji ?: "👋"

            Text(
                text = emoji,
                modifier = Modifier
                    .scale(animatable.value)
                    .alpha(alpha),
                style = MaterialTheme.typography.displayLarge
            )

            val starScale = animatable.value * 0.125f
            val baseDistance = 50.dp.px

            repeat(8) { i ->
                val angle = i * (360f / 8f)
                val angleRad = Math.toRadians(angle.toDouble())

                Text(
                    text = emoji,
                    modifier = Modifier
                        .offset {
                            val distance = (animatable.value - 1f) * baseDistance
                            IntOffset(
                                (cos(angleRad) * distance).roundToInt(),
                                (sin(angleRad) * distance).roundToInt()
                            )
                        }
                        .scale(starScale)
                        .alpha(alpha),
                    style = MaterialTheme.typography.displayLarge
                )
            }
        }
    }
}

@Composable
fun SignalsGreeting() {
    var size by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primary
    )

    val brush = rememberAnimatedGradientBrush(
        colors = colors,
        size = size,
        label = "Greeting"
    )

    Text(
        stringResource(greetingRes),
        style = MaterialTheme.typography.displaySmall.copy(
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            brush = brush
        ),
        modifier = Modifier
            .onSizeChanged { size = it.toSize() }
            .fillMaxWidth()
            .padding(horizontal = 1.pad, vertical = 2.pad)
    )
}

@Composable
fun ActiveSignalCard(
    send: SignalSendExtended,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val now = Clock.System.now()
    val totalDuration = if (send.signalSend.expiry != null && send.signalSend.createdAt != null) (send.signalSend.expiry!! - send.signalSend.createdAt!!).inWholeMilliseconds else 0L
    val remaining = ((send.signalSend.expiry?.let { it - now } ?: kotlin.time.Duration.ZERO)).inWholeMilliseconds
    val progress = if (totalDuration > 0) (remaining.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(1.5f.pad)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(send.signal?.emoji ?: "👋", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(0.5f.pad))
                Text(
                    send.signal?.name ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
            }
            if (!isMine) {
                Text(
                    send.person?.name ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.your_signal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val replies = send.replies
                    if (!replies.isNullOrEmpty()) {
                        Text(
                            " • ${replies.size} ${if (replies.size == 1) stringResource(R.string.reply_title) else stringResource(R.string.replies)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (!send.signalSend.message.isNullOrBlank()) {
                Text(
                    send.signalSend.message!!,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 0.5f.pad)
                )
            }
            Spacer(Modifier.height(0.5f.pad))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().clip(CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SignalBubble(
    signal: Signal,
    isOn: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    Surface(
        shape = CircleShape,
        color = if (isOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        border = if (isOn) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.aspectRatio(1f).scale(scale)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current
                )
                .padding(1.pad)
        ) {
            Text(signal.emoji ?: "❓", style = MaterialTheme.typography.headlineMedium)
            Text(
                signal.name ?: "",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            if (!signal.categories.isNullOrEmpty()) {
                Text(
                    signal.categories!!.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

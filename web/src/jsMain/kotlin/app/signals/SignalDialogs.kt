package app.signals

import Configuration
import Styles
import aiJson
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.cancelSignal
import app.ailaai.api.createSignal
import app.ailaai.api.createSignalGroup
import app.ailaai.api.replySignal
import app.ailaai.api.sendSignal
import app.dialog.categoryDialog
import app.dialog.dialog
import app.dialog.photoDialog
import app.dialog.rememberChoosePhotoDialog
import app.dialog.selectGroupDialog
import application
import baseUrl
import com.queatz.db.AiJsonRequest
import com.queatz.db.Group
import com.queatz.db.SendSignalBody
import com.queatz.db.Signal
import com.queatz.db.SignalAudience
import com.queatz.db.SignalReplyBody
import com.queatz.db.SignalSendExtended
import components.Icon
import components.IconButton
import components.ProfilePhoto
import json
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.max
import org.jetbrains.compose.web.attributes.min
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.step
import org.jetbrains.compose.web.attributes.value
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.LineStyle
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
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Audio
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.CheckboxInput
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.RangeInput
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import r
import kotlin.js.Date
import kotlin.math.roundToInt
import kotlin.time.Clock

suspend fun createSignalDialog(
    existingCategories: List<String>,
): Signal? {
    var finalName = ""
    var finalEmoji = ""
    var finalCategories = emptyList<String>()

    val confirmed = dialog(
        title = "Create Signal",
        confirmButton = application.appString { create },
        cancelButton = application.appString { cancel },
        enableConfirm = { finalName.isNotBlank() && finalEmoji.isNotBlank() },
    ) {
        val scope = rememberCoroutineScope()
        var name by remember { mutableStateOf("") }
        var emoji by remember { mutableStateOf("") }
        var isSuggested by remember { mutableStateOf(false) }
        var isSuggesting by remember { mutableStateOf(false) }
        var selectedCat by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(name) {
            finalName = name
            if (name.isBlank()) {
                isSuggesting = false
                return@LaunchedEffect
            }
            delay(500)
            if (emoji.isNotBlank() && !isSuggested) return@LaunchedEffect
            isSuggesting = true
            api.aiJson(
                request = AiJsonRequest(
                    prompt = "What is the best single emoji for '$name'? Return only the emoji in a json field named 'emoji'.",
                    schema = buildJsonObject {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("emoji") {
                                put("type", "string")
                            }
                        }
                        putJsonArray("required") {
                            add("emoji")
                        }
                        put("additionalProperties", false)
                    },
                ),
                onError = {
                    isSuggesting = false
                },
            ) { response ->
                isSuggesting = false
                val suggested = runCatching {
                    json.decodeFromString<JsonObject>(response.json)["emoji"]?.jsonPrimitive?.content
                }.getOrNull()
                if (!suggested.isNullOrBlank() && (emoji.isBlank() || isSuggested)) {
                    emoji = suggested
                    finalEmoji = suggested
                    isSuggested = true
                }
            }
        }

        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(.75.r)
                width(22.r)
            }
        }) {
            Input(InputType.Text) {
                classes(Styles.textarea)
                value(name)
                placeholder("Signal name")
                style { width(100.percent) }
                onInput {
                    name = it.value
                    finalName = it.value
                }
            }

            Div({
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    gap(.5.r)
                }
            }) {
                Input(InputType.Text) {
                    classes(Styles.textarea)
                    value(emoji)
                    placeholder(if (isSuggesting) "Suggesting emoji..." else "Emoji (e.g. ☕)")
                    style { flex(1) }
                    onInput {
                        emoji = it.value
                        finalEmoji = it.value
                        isSuggested = false
                    }
                }
            }

            Button({
                classes(Styles.outlineButton)
                style {
                    width(100.percent)
                    justifyContent(JustifyContent.FlexStart)
                }
                onClick {
                    scope.launch {
                        val cat = categoryDialog(
                            categories = existingCategories,
                            category = selectedCat,
                        )
                        if (cat != null) {
                            selectedCat = cat.takeIf { it.isNotBlank() }
                            finalCategories = selectedCat?.let(::listOf) ?: emptyList()
                        }
                    }
                }
            }) {
                Icon("category")
                Span({ style { padding(0.r, .35.r) } }) {
                    Text(selectedCat ?: "Set category (optional)")
                }
            }
        }
    }

    if (confirmed == true && finalName.isNotBlank() && finalEmoji.isNotBlank()) {
        var created: Signal? = null
        api.createSignal(
            signal = Signal(
                name = finalName.trim(),
                emoji = finalEmoji.trim(),
                categories = finalCategories,
            ),
        ) {
            created = it
        }
        return created
    }
    return null
}

suspend fun sendSignalDialog(
    signal: Signal,
    geo: List<Double>?,
    configuration: Configuration,
): Boolean {
    val durations = listOf(
        15 * 60 * 1000L to "15m",
        30 * 60 * 1000L to "30m",
        45 * 60 * 1000L to "45m",
        60 * 60 * 1000L to "1h",
        2 * 60 * 60 * 1000L to "2h",
        3 * 60 * 60 * 1000L to "3h",
        4 * 60 * 60 * 1000L to "4h",
    )
    val radii = listOf(100.0, 250.0, 500.0, 1000.0, 2500.0, 5000.0)

    var outMessage = ""
    var outAudience = SignalAudience.Friends
    var outRadius = 1000.0
    var outDuration = 60 * 60 * 1000L
    var outGroups = emptyList<Group>()
    var outPhoto: String? = null

    val confirmed = dialog(
        title = null,
        confirmButton = application.appString { send },
        cancelButton = application.appString { cancel },
        enableConfirm = { outAudience != SignalAudience.Groups || outGroups.isNotEmpty() },
    ) {
        val scope = rememberCoroutineScope()
        val choosePhoto = rememberChoosePhotoDialog(showUpload = true)
        val isGenerating by choosePhoto.isGenerating.collectAsState()

        var message by remember { mutableStateOf("") }
        var audience by remember { mutableStateOf(if (geo != null) SignalAudience.Nearby else SignalAudience.Friends) }
        var radiusIdx by remember { mutableStateOf(3) }
        var duration by remember { mutableStateOf(60 * 60 * 1000L) }
        var selectedGroups by remember { mutableStateOf(emptyList<Group>()) }
        var photo by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(audience) {
            outAudience = audience
        }

        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(.85.r)
                width(24.r)
            }
        }) {
            Div({
                style {
                    fontSize(56.px)
                    textAlign("center")
                }
            }) {
                Text(signal.emoji ?: "👋")
            }
            Div({
                style {
                    fontSize(20.px)
                    fontWeight(600)
                    textAlign("center")
                }
            }) {
                Text(signal.name.orEmpty())
            }

            TextArea(message) {
                classes(Styles.textarea)
                placeholder("Message (optional)")
                style { width(100.percent) }
                onInput {
                    message = it.value
                    outMessage = it.value
                }
            }

            Div({ style { fontSize(12.px); opacity(0.75) } }) {
                Text("Audience")
            }
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexWrap(FlexWrap.Wrap)
                    gap(.5.r)
                }
            }) {
                SignalAudience.entries.forEach { itemAudience ->
                    val selected = audience == itemAudience
                    Button({
                        classes(if (selected) Styles.button else Styles.outlineButton)
                        onClick {
                            audience = itemAudience
                            outAudience = itemAudience
                        }
                    }) {
                        Icon(
                            when (itemAudience) {
                                SignalAudience.Nearby -> "place"
                                SignalAudience.Friends -> "group"
                                SignalAudience.Groups -> "forum"
                            }
                        ) { fontSize(16.px) }
                        Span({ style { padding(0.r, .25.r) } }) {
                            Text(itemAudience.name)
                        }
                    }
                }
            }

            if (audience == SignalAudience.Nearby) {
                val radiusMeters = radii[radiusIdx]
                val label = if (radiusMeters >= 1000) "${radiusMeters / 1000.0} km" else "${radiusMeters.toInt()} m"
                Div({ style { fontSize(12.px); opacity(0.8) } }) {
                    Text("Radius: $label")
                }
                RangeInput(
                    value = radiusIdx,
                    min = 0,
                    max = radii.size - 1,
                    step = 1,
                ) {
                    style { width(100.percent) }
                    onInput { event ->
                        val idx = (event.value?.toInt() ?: 3).coerceIn(radii.indices)
                        radiusIdx = idx
                        outRadius = radii[idx]
                    }
                }
            }

            if (audience == SignalAudience.Groups) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        gap(.5.r)
                    }
                }) {
                    if (selectedGroups.isNotEmpty()) {
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                flexWrap(FlexWrap.Wrap)
                                gap(.35.r)
                            }
                        }) {
                            selectedGroups.forEach { g ->
                                Span({
                                    style {
                                        padding(.25.r, .6.r)
                                        borderRadius(999.px)
                                        backgroundColor(Styles.colors.surface)
                                        border(1.px, LineStyle.Solid, Styles.colors.outline)
                                        fontSize(12.px)
                                        cursor("pointer")
                                    }
                                    title("Click to remove")
                                    onClick {
                                        selectedGroups = selectedGroups.filter { it.id != g.id }
                                        outGroups = selectedGroups
                                    }
                                }) {
                                    Text("${g.name ?: "Group"} ×")
                                }
                            }
                        }
                    }
                    Button({
                        classes(Styles.outlineButton)
                        onClick {
                            scope.launch {
                                val chosen = selectGroupDialog(configuration = configuration)
                                chosen?.group?.let { grp ->
                                    if (selectedGroups.none { it.id == grp.id }) {
                                        selectedGroups = selectedGroups + grp
                                        outGroups = selectedGroups
                                    }
                                }
                            }
                        }
                    }) {
                        Icon("add")
                        Span({ style { padding(0.r, .25.r) } }) {
                            Text("Add group")
                        }
                    }
                }
            }

            Div({ style { fontSize(12.px); opacity(0.75) } }) {
                Text("Duration")
            }
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexWrap(FlexWrap.Wrap)
                    gap(.35.r)
                }
            }) {
                durations.forEach { (ms, label) ->
                    Button({
                        classes(if (duration == ms) Styles.button else Styles.outlineButton)
                        onClick {
                            duration = ms
                            outDuration = ms
                        }
                    }) {
                        Text(label)
                    }
                }
            }

            Div({
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    gap(.5.r)
                }
            }) {
                IconButton(
                    name = "photo",
                    title = "Attach photo",
                    isLoading = isGenerating,
                    enabled = !isGenerating,
                ) {
                    scope.launch {
                        choosePhoto.launch { uploadedUrl, _, _ ->
                            photo = uploadedUrl
                            outPhoto = uploadedUrl
                        }
                    }
                }
                if (photo != null) {
                    Img(src = "$baseUrl$photo") {
                        style {
                            height(48.px)
                            borderRadius(.5.r)
                        }
                    }
                    IconButton(
                        name = "delete",
                        title = "Remove photo",
                    ) {
                        photo = null
                        outPhoto = null
                    }
                }
            }
        }
    }

    if (confirmed == true) {
        var sentOk = false
        api.sendSignal(
            body = SendSignalBody(
                signal = signal.id!!,
                message = outMessage.trim().takeIf { it.isNotEmpty() },
                photo = outPhoto,
                geo = if (outAudience == SignalAudience.Nearby) geo else null,
                radius = if (outAudience == SignalAudience.Nearby) outRadius / 1000.0 else null,
                audience = outAudience,
                groups = if (outAudience == SignalAudience.Groups) outGroups.mapNotNull { it.id } else null,
                duration = outDuration,
                localHour = Date().getHours(),
            ),
        ) {
            sentOk = true
        }
        return sentOk
    }
    return false
}

suspend fun replySignalDialog(
    send: SignalSendExtended,
): Boolean {
    val now = Clock.System.now()
    val isExpired = send.signalSend.expiry?.let { it <= now } == true
    val alreadyReplied = !send.replies.isNullOrEmpty()

    var outMessage = ""
    var outPhoto: String? = null

    val confirmed = dialog(
        title = "Reply to ${send.person?.name ?: "Someone"} ${send.signal?.emoji.orEmpty()} ${send.signal?.name.orEmpty()}",
        confirmButton = if (!isExpired && !alreadyReplied) "Reply" else null,
        cancelButton = application.appString { close },
    ) {
        val scope = rememberCoroutineScope()
        val choosePhoto = rememberChoosePhotoDialog(showUpload = true)
        val isGenerating by choosePhoto.isGenerating.collectAsState()
        var message by remember { mutableStateOf("") }
        var photo by remember { mutableStateOf<String?>(null) }

        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(.75.r)
                width(22.r)
            }
        }) {
            if (!send.signalSend.message.isNullOrBlank()) {
                Div({ style { fontSize(14.px) } }) {
                    Text(send.signalSend.message!!)
                }
            }

            WebSignalAttachments(
                photo = send.signalSend.photo,
                audio = send.signalSend.audio,
            )

            if (isExpired) {
                Div({ style { color(Styles.colors.red); fontSize(13.px) } }) {
                    Text("This signal has expired.")
                }
            } else if (alreadyReplied) {
                Div({ style { color(Styles.colors.primary); fontSize(13.px) } }) {
                    Text("You have already replied to this signal.")
                }
            } else {
                TextArea(message) {
                    classes(Styles.textarea)
                    placeholder("Your reply...")
                    style { width(100.percent) }
                    onInput {
                        message = it.value
                        outMessage = it.value
                    }
                }

                Div({
                    style {
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        gap(.5.r)
                    }
                }) {
                    IconButton(
                        name = "photo",
                        title = "Attach photo",
                        isLoading = isGenerating,
                        enabled = !isGenerating,
                    ) {
                        scope.launch {
                            choosePhoto.launch { uploaded, _, _ ->
                                photo = uploaded
                                outPhoto = uploaded
                            }
                        }
                    }
                    if (photo != null) {
                        Img(src = "$baseUrl$photo") {
                            style {
                                height(48.px)
                                borderRadius(.5.r)
                            }
                        }
                        IconButton("delete", "Remove photo") {
                            photo = null
                            outPhoto = null
                        }
                    }
                }
            }
        }
    }

    if (confirmed == true && !isExpired && !alreadyReplied) {
        var replied = false
        api.replySignal(
            id = send.signalSend.id!!,
            body = SignalReplyBody(
                message = outMessage.trim().takeIf { it.isNotEmpty() },
                photo = outPhoto,
            ),
        ) {
            replied = true
        }
        return replied
    }
    return false
}

sealed interface SignalRepliesResult {
    data object Cancelled : SignalRepliesResult
    data class CreatedGroup(
        val group: Group,
    ) : SignalRepliesResult
}

suspend fun signalRepliesDialog(
    send: SignalSendExtended,
): SignalRepliesResult? {
    var actionRequested: String? = null
    var chosenPeople: List<String> = emptyList()

    dialog(
        title = null,
        confirmButton = null,
        cancelButton = application.appString { close },
        maxWidth = 30.r,
        extraButtons = { resolve ->
            Button({
                classes(Styles.textButton)
                style { color(Styles.colors.red) }
                onClick {
                    actionRequested = "cancel"
                    resolve(true)
                }
            }) {
                Text("Cancel Signal")
            }
        },
    ) { resolve ->
        val selectedPeople = remember { mutableStateListOf<String>() }

        val now = Clock.System.now()
        val totalMs = if (send.signalSend.expiry != null && send.signalSend.createdAt != null) {
            (send.signalSend.expiry!! - send.signalSend.createdAt!!).inWholeMilliseconds
        } else {
            1L
        }
        val remDuration = (send.signalSend.expiry ?: now) - now
        val remMs = remDuration.inWholeMilliseconds.coerceAtLeast(0L)
        val pct = if (totalMs > 0L) ((remMs.toDouble() / totalMs.toDouble()) * 100.0).coerceIn(0.0, 100.0) else 0.0
        val hours = remDuration.inWholeHours.coerceAtLeast(0L)
        val minutes = (remDuration.inWholeMinutes % 60L).coerceAtLeast(0L)

        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(.75.r)
                width(25.r)
                maxHeight(68.vh)
                overflowY("auto")
            }
        }) {
            Div({ style { fontSize(52.px); textAlign("center") } }) {
                Text(send.signal?.emoji ?: "👋")
            }
            Div({ style { fontSize(20.px); fontWeight(600); textAlign("center") } }) {
                Text(send.signal?.name.orEmpty())
            }
            if (!send.signalSend.message.isNullOrBlank()) {
                Div({ style { textAlign("center"); fontSize(14.px) } }) {
                    Text(send.signalSend.message!!)
                }
            }

            WebSignalAttachments(
                photo = send.signalSend.photo,
                audio = send.signalSend.audio,
            )

            // Progress bar
            Div({
                style {
                    width(100.percent)
                    height(6.px)
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

            Div({
                style {
                    fontSize(12.px)
                    opacity(0.7)
                    textAlign("center")
                }
            }) {
                Text(if (hours > 0) "${hours}h ${minutes}m remaining" else "${minutes}m remaining")
            }

            Div({ style { fontWeight(600); fontSize(15.px); padding(.25.r, 0.r) } }) {
                Text("Replies")
            }

            val replies = send.replies.orEmpty()
            if (replies.isEmpty()) {
                Div({ style { opacity(0.65); fontSize(13.px) } }) {
                    Text("Waiting for replies...")
                }
            } else {
                replies.forEach { reply ->
                    val pid = reply.person?.id ?: reply.signalReply.person
                    val isChecked = pid != null && selectedPeople.contains(pid)

                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            gap(.75.r)
                            padding(.65.r)
                            borderRadius(.75.r)
                            backgroundColor(Styles.colors.surface)
                            border(
                                1.px,
                                LineStyle.Solid,
                                if (isChecked) Styles.colors.primary else Styles.colors.outline,
                            )
                            cursor("pointer")
                        }
                        onClick {
                            if (pid != null) {
                                if (selectedPeople.contains(pid)) selectedPeople.remove(pid)
                                else selectedPeople.add(pid)
                            }
                        }
                    }) {
                        CheckboxInput(checked = isChecked) {}
                        reply.person?.let { person ->
                            ProfilePhoto(person, size = 36.px)
                        }
                        Div({
                            style {
                                flex(1)
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Column)
                                gap(.2.r)
                            }
                        }) {
                            Div({ style { fontWeight(600); fontSize(14.px) } }) {
                                Text(reply.person?.name ?: "Someone")
                            }
                            if (!reply.affinitySignals.isNullOrEmpty()) {
                                Div({ style { fontSize(12.px); opacity(0.8) } }) {
                                    Text(reply.affinitySignals!!.joinToString(" ") { it.emoji.orEmpty() })
                                }
                            }
                            if (!reply.signalReply.message.isNullOrBlank()) {
                                Div({ style { fontSize(13.px) } }) {
                                    Text(reply.signalReply.message!!)
                                }
                            }
                            WebSignalAttachments(
                                photo = reply.signalReply.photo,
                                audio = reply.signalReply.audio,
                            )
                        }
                    }
                }

                if (selectedPeople.isNotEmpty()) {
                    Button({
                        classes(Styles.button)
                        style {
                            width(100.percent)
                            justifyContent(JustifyContent.Center)
                        }
                        onClick {
                            chosenPeople = selectedPeople.toList()
                            actionRequested = "group"
                            resolve(true)
                        }
                    }) {
                        Icon("group_add")
                        Span({ style { padding(0.r, .25.r) } }) {
                            Text("Create group (${selectedPeople.size})")
                        }
                    }
                }
            }
        }
    }

    if (actionRequested == "cancel") {
        val confirmCancel = dialog(
            title = "Cancel signal?",
            confirmButton = "Yes, cancel",
            cancelButton = application.appString { cancel },
        ) {
            Div { Text("Your active signal will end immediately.") }
        }
        if (confirmCancel == true) {
            var done = false
            api.cancelSignal(id = send.signalSend.id!!) {
                done = true
            }
            if (done) return SignalRepliesResult.Cancelled
        }
    } else if (actionRequested == "group" && chosenPeople.isNotEmpty()) {
        var createdGroup: Group? = null
        api.createSignalGroup(
            id = send.signalSend.id!!,
            people = chosenPeople,
        ) {
            createdGroup = it
        }
        if (createdGroup != null) {
            return SignalRepliesResult.CreatedGroup(group = createdGroup)
        }
    }

    return null
}

@androidx.compose.runtime.Composable
fun WebSignalAttachments(
    photo: String?,
    audio: String?,
) {
    val scope = rememberCoroutineScope()
    if (photo.isNullOrBlank() && audio.isNullOrBlank()) return

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            gap(.5.r)
        }
    }) {
        if (!photo.isNullOrBlank()) {
            Img(src = "$baseUrl$photo") {
                style {
                    maxWidth(100.percent)
                    maxHeight(160.px)
                    borderRadius(.65.r)
                    cursor("pointer")
                    property("object-fit", "cover")
                }
                onClick {
                    it.stopPropagation()
                    scope.launch {
                        photoDialog("$baseUrl$photo")
                    }
                }
            }
        }
        if (!audio.isNullOrBlank()) {
            Audio({
                attr("controls", "true")
                attr("src", "$baseUrl$audio")
                style {
                    width(100.percent)
                }
                onClick { it.stopPropagation() }
            }) {}
        }
    }
}

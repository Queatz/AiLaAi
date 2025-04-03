package app.group

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.PageTopBar
import app.ailaai.api.createMember
import app.ailaai.api.newCard
import app.ailaai.api.pinGroup
import app.ailaai.api.removeMember
import app.ailaai.api.unpinGroup
import app.ailaai.api.updateGroup
import app.ailaai.api.updateMember
import app.bots.addBotDialog
import app.bots.botDialog
import app.bots.botHowToDialog
import app.bots.createBotDialog
import app.bots.createGroupBotDialog
import app.bots.groupBotDialog
import app.bots.groupBotsDialog
import app.bots.updateGroupBotDialog
import app.dialog.dialog
import app.dialog.inputDialog
import app.invites.createInviteDialog
import app.menu.InlineMenu
import app.menu.Menu
import app.messaages.inList
import app.nav.name
import appString
import appText
import application
import call
import com.queatz.db.Bot
import com.queatz.db.Card
import com.queatz.db.Effect
import com.queatz.db.Group
import com.queatz.db.GroupBot
import com.queatz.db.GroupConfig
import com.queatz.db.GroupEditsConfig
import com.queatz.db.GroupExtended
import com.queatz.db.GroupMessagesConfig
import com.queatz.db.JoinRequestAndPerson
import com.queatz.db.Member
import com.queatz.db.Person
import com.queatz.db.RainEffect
import components.IconButton
import components.LinkifyText
import joins
import json
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import lib.formatDistanceToNow
import notBlank
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.RangeInput
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import qr
import r
import webBaseUrl
import kotlin.js.Date

@Composable
fun GroupTopBar(
    group: GroupExtended,
    onGroupUpdated: () -> Unit,
    onGroupGone: () -> Unit,
    showCards: Boolean,
    onShowCards: () -> Unit,
    onSearch: () -> Unit
) {
    val me by application.me.collectAsState()
    val myMember = group.members?.find { it.person?.id == me?.id }
    val scope = rememberCoroutineScope()
    val calls by call.calls.collectAsState()
    val callParticipants = calls.firstOrNull { it.group == group.group!!.id }?.participants ?: 0

    val closeStr = appString { close }

    var menuTarget by remember {
        mutableStateOf<DOMRect?>(null)
    }

    var showDescription by remember(group) {
        mutableStateOf(true)
    }

    fun showBot(bot: Bot, onBotDeleted: () -> Unit) {
        scope.launch {
            val reload = MutableSharedFlow<Unit>()

            botDialog(
                scope = scope,
                reload = reload,
                bot = bot,
                onBotUpdated = {
                    scope.launch {
                        reload.emit(Unit)
                    }
                },
                onBotDeleted = {
                    onBotDeleted()
                    onGroupUpdated()
                }
            )
        }
    }

    fun createBot(onCreated: (Bot) -> Unit) {
        scope.launch {
            createBotDialog(
                this,
                {
                    scope.launch {
                        botHowToDialog()
                    }
                }
            ) {
                onCreated(it)
                showBot(it) {}
            }
        }
    }

    fun editGroupBot(bot: Bot, groupBot: GroupBot, onUpdated: () -> Unit) {
        scope.launch {
            updateGroupBotDialog(bot, groupBot) {
                onUpdated()
            }
        }
    }

    fun showGroupBot(bot: Bot, groupBot: GroupBot, onUpdated: () -> Unit) {
        scope.launch {
            val reload = MutableSharedFlow<Unit>()
            groupBotDialog(
                scope = scope,
                reload = reload,
                bot = bot,
                groupBot = groupBot,
                myMember = myMember,
                onGroupBotUpdated = {
                    onUpdated()
                },
                onEditBot = { bot, groupBot ->
                    editGroupBot(bot, groupBot) {
                        scope.launch {
                            reload.emit(Unit)
                        }
                        onUpdated()
                    }
                },
                onBotRemoved = {
                    onUpdated()
                },
                onOpenBot = {
                    showBot(it) {
                        scope.launch {
                            reload.emit(Unit)
                        }
                        onUpdated()
                    }
                }
            )
        }
    }

    fun createGroupBot(bot: Bot, onCreated: () -> Unit) {
        scope.launch {
            val reload = MutableSharedFlow<Unit>()
            createGroupBotDialog(
                reload = reload,
                group = group.group!!.id!!,
                bot = bot,
                onOpenBot = {
                    showBot(it) {
                        scope.launch {
                            reload.emit(Unit)
                        }
                    }
                }
            ) {
                onCreated()
            }
        }
    }

    fun addABot(onAdded: () -> Unit) {
        scope.launch {
            val reload = MutableSharedFlow<Unit>()
            addBotDialog(
                reload = reload,
                group = group.group!!.id!!,
                onCreateBot = {
                    createBot {
                        scope.launch {
                            reload.emit(Unit)
                        }
                    }
                },
                onAddBot = {
                    createGroupBot(it) {
                        onAdded()
                        onGroupUpdated()
                    }
                }
            )
        }
    }

    fun showBots() {
        scope.launch {
            val reload = MutableSharedFlow<Unit>()
            groupBotsDialog(
                reload = reload,
                group = group.group!!.id!!,
                myMember = myMember,
                onAddBot = {
                    addABot {
                        scope.launch {
                            reload.emit(Unit)
                        }
                    }
                },
                onBot = { bot, groupBot ->
                    showGroupBot(bot, groupBot) {
                        onGroupUpdated()
                        scope.launch {
                            reload.emit(Unit)
                        }
                    }
                }
            )
        }
    }

    fun renameGroup() {
        scope.launch {
            val name = inputDialog(
                title = application.appString { groupName },
                placeholder = "",
                confirmButton = application.appString { rename },
                defaultValue = group.group?.name ?: ""
            )

            if (name == null) return@launch

            api.updateGroup(group.group!!.id!!, Group(name = name)) {
                onGroupUpdated()
            }
        }
    }

    fun makeOpen(open: Boolean) {
        scope.launch {
            val result = dialog(
                application.appString { if (open) actionOpenGroup else actionCloseGroup },
                application.appString { if (open) makeOpenGroup else makeCloseGroup },
            ) {
                appText { if (open) actionOpenGroupDescription else actionCloseGroupDescription }
            }

            if (result != true) return@launch

            api.updateGroup(group.group!!.id!!, Group(open = open)) {
                onGroupUpdated()
            }
        }
    }

    fun showEffects() {
        scope.launch {
            val groupConfig = group.group?.config ?: GroupConfig()

            val result = dialog(
                application.appString { effects },
                application.appString { update },
                closeStr
            ) {
                var effectsConfig by remember {
                    mutableStateOf<List<Effect>?>(group.group?.config?.effects?.let { json.decodeFromString(it) })
                }

                LaunchedEffect(effectsConfig) {
                    groupConfig.effects = json.encodeToString(effectsConfig)
                }

                InlineMenu({}) {
                    item(appString { none }, selected = effectsConfig.isNullOrEmpty()) {
                        effectsConfig = emptyList()
                    }

                    val selected = effectsConfig?.any { it is RainEffect } == true
                    item(
                        appString { rain },
                        selected = selected,
                        icon = "settings".takeIf { selected },
                        onIconClick = if (selected) {
                            {
                                scope.launch {
                                    dialog(application.appString { settings }) {
                                        var rainAmount by remember {
                                            mutableStateOf(
                                                (effectsConfig?.firstOrNull() as? RainEffect)?.amount ?: 0.1
                                            )
                                        }

                                        LaunchedEffect(rainAmount) {
                                            effectsConfig = RainEffect(rainAmount).inList()
                                        }

                                        RangeInput(
                                            rainAmount * 100.0,
                                            min = 5,
                                            max = 100
                                        ) {
                                            onInput {
                                                rainAmount = it.value!!.toDouble() / 100.0
                                            }
                                        }
                                    }
                                }
                            }
                        } else null
                    ) {
                        effectsConfig = RainEffect(.1).inList()
                    }
                }
            }

            if (result == true) {
                api.updateGroup(group.group!!.id!!, Group(config = groupConfig)) {
                    onGroupUpdated()
                }
            }
        }
    }

    fun showSettings() {
        scope.launch {
            val groupConfig = group.group?.config ?: GroupConfig()

            val result = dialog(
                application.appString { settings },
                application.appString { update },
                closeStr
            ) {
                var messagesConfig by remember { mutableStateOf(group.group?.config?.messages) }
                var editsConfig by remember { mutableStateOf(group.group?.config?.edits) }

                LaunchedEffect(messagesConfig) {
                    groupConfig.messages = messagesConfig
                }

                LaunchedEffect(editsConfig) {
                    groupConfig.edits = editsConfig
                }

                Div(
                    {
                        style {
                            fontSize(18.px)
                            fontWeight("bold")
                            marginBottom(1.r)
                        }
                    }
                ) {
                    // todo translate
                    Text("Who sends messages to this group?")
                }
                InlineMenu({}) {
                    // todo translate
                    item("Hosts", selected = messagesConfig == GroupMessagesConfig.Hosts) {
                        messagesConfig = GroupMessagesConfig.Hosts
                    }
                    // todo translate
                    item("Everyone", selected = messagesConfig == null) {
                        messagesConfig = null
                    }
                }
                Div(
                    {
                        style {
                            fontSize(18.px)
                            fontWeight("bold")
                            marginTop(1.r)
                        }
                    }
                ) {
                    // todo translate
                    Text("Who edits this group?")
                }
                Div(
                    {
                        style {
                            fontSize(14.px)
                            opacity(.5f)
                            marginBottom(1.r)
                        }
                    }
                ) {
                    // todo translate
                    Text("Name, introduction, photo")
                }
                InlineMenu({}) {
                    // todo translate
                    item("Hosts", selected = editsConfig == GroupEditsConfig.Hosts) {
                        editsConfig = GroupEditsConfig.Hosts

                    }
                    // todo translate
                    item("Everyone", selected = editsConfig == null) {
                        editsConfig = null
                    }
                }
            }

            if (result == true) {
                api.updateGroup(group.group!!.id!!, Group(config = groupConfig)) {
                    onGroupUpdated()
                }
            }
        }
    }

    fun updateIntroduction() {
        scope.launch {
            val introduction = inputDialog(
                title = application.appString { introduction },
                placeholder = "",
                singleLine = false,
                confirmButton = application.appString { update },
                defaultValue = group.group?.description ?: ""
            )

            if (introduction == null) return@launch

            api.updateGroup(group.group!!.id!!, Group(description = introduction)) {
                onGroupUpdated()
            }
        }
    }

    fun createCard() {
        scope.launch {
            val result = inputDialog(
                application.appString { createCard },
                application.appString { title },
                application.appString { create }
            )

            if (result == null) return@launch

            api.newCard(Card(name = result, group = group.group!!.id!!)) {
                onGroupUpdated()
            }
        }
    }

    fun addMember(person: Person) {
        scope.launch {
            api.createMember(
                Member().apply {
                    from = person.id!!
                    to = group.group!!.id!!
                }
            ) {
                onGroupUpdated()
            }
        }
    }

    if (menuTarget != null) {
        Menu({ menuTarget = null }, menuTarget!!) {
            item(appString { openInNewTab }, icon = "open_in_new") {
                window.open("/group/${group.group!!.id}", target = "_blank")
            }
            item(appString { search }, icon = "search") {
                onSearch()
            }
            if (myMember != null) {
                item(appString { invite }) {
                    scope.launch {
                        friendsDialog(
                            omit = group.members?.mapNotNull { it.person?.id }.orEmpty(),
                            actions = { resolve ->
                                if (myMember.member?.host != true) {
                                    return@friendsDialog
                                }
                                IconButton(
                                    name = "add_link",
                                    // todo: translate
                                    title = "Create an invite link"
                                ) {
                                    resolve(null)
                                    scope.launch {
                                        createInviteDialog(
                                            group = group.group!!.id!!,
                                            onError = {
                                                scope.launch {
                                                    dialog(
                                                        // todo: translate
                                                        title = "The invite link could not be created",
                                                        cancelButton = null
                                                    )
                                                }
                                            }
                                        ) { invite ->
                                            scope.launch {
                                                val result = dialog(
                                                    // todo: translate
                                                    title = "Invite link is active",
                                                    cancelButton = application.appString { close },
                                                    // todo: translate
                                                    confirmButton = "Copy link",
                                                    content = {
                                                        Div({
                                                            style {
                                                                padding(1.r)
                                                                border(1.px, LineStyle.Solid, Styles.colors.primary)
                                                                borderRadius(1.r)
                                                                property("word-break", "break-word")
                                                            }
                                                        }) {
                                                            A(
                                                                href = "$webBaseUrl/invite/${invite.code!!}",
                                                                attrs = {
                                                                    target(ATarget.Blank)
                                                                }
                                                            ) {
                                                                Text("$webBaseUrl/invite/${invite.code!!}")
                                                            }
                                                        }
                                                    }
                                                )
                                                
                                                if (result == true) {
                                                    window.navigator.clipboard.writeText(
                                                        "$webBaseUrl/invite/${invite.code!!}"
                                                    )
                                                }
                                            }
                                            onGroupUpdated()
                                        }
                                    }
                                }
                            }
                        ) {
                            it.forEach { addMember(it) }
                        }
                    }
                }
            }
            item(appString { members }) {
                scope.launch {
                    groupMembersDialog(group)
                }
            }
            item(appString { bots }) {
                showBots()
            }
            item(appString { this.cards }) {
                onShowCards()
            }
            if (myMember != null) {
                if (group.group?.config?.edits == null || myMember.member?.host == true) {
                    item(appString { rename }) {
                        renameGroup()
                    }
                    item(appString { introduction }) {
                        updateIntroduction()
                    }
                }
                if (myMember.member?.host == true) {
                    item(appString { manage }) {
                        scope.launch {
                            dialog(
                                null,
                                closeStr,
                                null
                            ) {
                                InlineMenu({
                                    it(true)
                                }) {
                                    if (group.group?.open == true) {
                                        item(appString { makeCloseGroup }) {
                                            makeOpen(false)
                                        }
                                    } else {
                                        item(appString { makeOpenGroup }) {
                                            makeOpen(true)
                                        }
                                    }
                                    item(appString { effects }) {
                                        showEffects()
                                    }
                                    item(appString { settings }) {
                                        showSettings()
                                    }
                                }
                            }
                        }
                    }
                }
                item(appString { if (group.pin == true) unpin else pin }) {
                    scope.launch {
                        if (group.pin == true) {
                            api.unpinGroup(group.group!!.id!!) {
                                onGroupUpdated()
                            }
                        } else {
                            api.pinGroup(group.group!!.id!!) {
                                onGroupUpdated()
                            }
                        }
                    }
                }
                item(appString { hide }) {
                    scope.launch {
                        api.updateMember(
                            myMember.member!!.id!!,
                            Member(hide = true)
                        ) {
                            onGroupGone()
                        }
                    }
                }
                item(appString { leave }) {
                    scope.launch {
                        val result = dialog(application.appString { leaveGroup }, application.appString { leave })

                        if (result == true) {
                            api.removeMember(
                                myMember.member!!.id!!
                            ) {
                                onGroupGone()
                            }
                        }
                    }
                }
            }

            item(appString { qrCode }) {
                scope.launch {
                    dialog("", cancelButton = null) {
                        val qrCode = remember {
                            "$webBaseUrl/group/${group.group!!.id!!}".qr
                        }
                        Img(src = qrCode) {
                            style {
                                borderRadius(1.r)
                            }
                        }
                    }
                }
            }
        }
    }

    val allJoinRequests by joins.joins.collectAsState()
    var joinRequests by remember {
        mutableStateOf(emptyList<JoinRequestAndPerson>())
    }

    LaunchedEffect(allJoinRequests) {
        joinRequests = allJoinRequests.filter { it.joinRequest?.group == group.group?.id }
    }

    if (joinRequests.isNotEmpty()) {
        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                overflowY("auto")
                overflowX("hidden")
                maxHeight(25.vh)
            }
        }) {
            joinRequests.forEach {
                GroupJoinRequest(it) {
                    onGroupUpdated()
                }
            }
        }
    } else if (showDescription && !showCards) {
        group.group?.description?.notBlank?.let { description ->
            Div({
                classes(AppStyles.groupDescription)

                onClick {
                    showDescription = false
                }

                title(application.appString { clickToHide })
            }) {
                LinkifyText(description)
            }
        }
    }

    // todo: apps
    if (false && !showCards) {
        Div({
            classes(AppStyles.groupAppsBar)
        }) {
            Button({
                classes(Styles.button)
            }) {
                Text("\uD83C\uDF75 Free Matcha 7am - 8am")
            }
        }
    }

    val active = group.members?.filter { it != myMember }?.maxByOrNull {
        it.person?.seen?.toEpochMilliseconds() ?: 0
    }?.person?.seen?.let { Date(it.toEpochMilliseconds()) }?.let {
        "${appString { active }} ${formatDistanceToNow(it, js("{ addSuffix: true }"))}"
    }

    PageTopBar(
        title = group.name(appString { someone }, appString { newGroup }, listOf(me!!.id!!)),
        description = if (group.group?.open == true) {
            listOfNotNull(appString { openGroup }, active).joinToString(" • ")
        } else {
            active
        },
        actions = {
            if (!showCards) {
                IconButton(
                    name = "call",
                    title = appString { call },
                    // todo translate
                    text = if (callParticipants > 0) "$callParticipants in call" else null,
                    backgroundColor = if (callParticipants > 0) Styles.colors.tertiary else null,
                    styles = {
                        marginRight(.5.r)
                    }
                ) {
                    if (call.active.value?.group?.group?.id == group.group?.id) {
                        call.end()
                    } else {
                        call.join(me ?: return@IconButton, group)
                    }
                }
            }

            if (showCards) {
                if (myMember != null) {
                    IconButton(
                        name = "add",
                        title = appString { createCard },
                        styles = {
                            marginRight(.5.r)
                        }
                    ) {
                        createCard()
                    }
                }
                IconButton(
                    name = "forum",
                    title = appString { goBack },
                    styles = {
                        marginRight(.5.r)
                    }
                ) {
                    onShowCards()
                }
            } else {
                group.cardCount?.takeIf { it > 0 }?.let {
                    IconButton(
                        name = "map",
                        title = appString { this.cards },
                        count = it,
                        styles = {
                            marginRight(.5.r)
                        }
                    ) {
                        onShowCards()
                    }
                }
                group.botCount?.takeIf { it > 0 }?.let {
                    IconButton(
                        name = "smart_toy",
                        title = appString { bots },
                        count = it,
                        styles = {
                            marginRight(.5.r)
                        }
                    ) {
                        showBots()
                    }
                }
            }
        }
    ) {
        menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
    }
}

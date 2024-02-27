package app.group

import Styles
import androidx.compose.runtime.*
import api
import app.AppStyles
import app.PageTopBar
import app.ailaai.api.*
import app.dialog.dialog
import app.dialog.inputDialog
import app.menu.InlineMenu
import app.menu.Menu
import app.nav.CardNav
import app.nav.name
import appString
import appText
import application
import call
import com.queatz.db.*
import components.IconButton
import components.LinkifyText
import joins
import kotlinx.browser.window
import kotlinx.coroutines.launch
import lib.formatDistanceToNow
import notBlank
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
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
    onShowCards: () -> Unit
) {
    val me by application.me.collectAsState()
    val myMember = group.members?.find { it.person?.id == me?.id }
    val scope = rememberCoroutineScope()
    val calls by call.calls.collectAsState()
    val callParticipants = calls.firstOrNull { it.group == group.group!!.id }?.participants ?: 0

    var menuTarget by remember {
        mutableStateOf<DOMRect?>(null)
    }

    var showDescription by remember(group) {
        mutableStateOf(true)
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
//            item("Pin") {
//
//            }
            if (myMember != null) {
                item(appString { invite }) {
                    scope.launch {
                        friendsDialog(group.members?.mapNotNull { it.person?.id } ?: emptyList()) {
                            addMember(it)
                        }
                    }
                }
            }
            item(appString { members }) {
                scope.launch {
                    groupMembersDialog(group)
                }
            }
            item(appString { this.cards }) {
                onShowCards()
            }
            if (myMember != null) {
                item(appString { rename }) {
                    renameGroup()
                }
                item(appString { introduction }) {
                    updateIntroduction()
                }
                if (myMember.member?.host == true) {
                    val closeStr = appString { close }
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
                                }
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
    }

    val allJoinRequests by joins.joins.collectAsState()
    var joinRequests by remember {
        mutableStateOf(emptyList<JoinRequestAndPerson>())
    }

    LaunchedEffect(allJoinRequests) {
        joinRequests = allJoinRequests.filter { it.joinRequest?.group == group.group?.id}
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
            }
        }
    ) {
        menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
    }
}

package app.group

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.AppStyles
import app.messaages.preview
import app.nav.isUnread
import app.nav.name
import appString
import appText
import application
import baseUrl
import call
import com.queatz.db.*
import components.GroupPhoto
import components.Icon
import focusable
import joins
import kotlinx.coroutines.flow.map
import lib.formatDistanceToNow
import lib.toLocaleString
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r
import kotlin.js.Date

enum class GroupInfo {
    LatestMessage,
    Members
}

@Composable
fun GroupItem(
    group: GroupExtended,
    selectable: Boolean = true,
    selected: Boolean = false,
    onSurface: Boolean = false,
    onBackground: Boolean = false,
    coverPhoto: Boolean = false,
    showInCall: Boolean = false,
    shadow: Boolean = false,
    maxWidth: CSSSizeValue<CSSUnit.rem>? = null,
    onSelected: () -> Unit,
    info: GroupInfo = GroupInfo.LatestMessage,
    styles: StyleScope.() -> Unit = {}
) {
    val me by application.me.collectAsState()
    val calls by call.calls.collectAsState()
    val myMember = group.members?.takeIf { me != null }?.find { it.person?.id == me?.id }
    val inCall = if (showInCall) (calls.firstOrNull { it.group == group.group!!.id }?.participants ?: 0) else 0

    val joinRequestCount by joins.joins
        .map { it.count { it.joinRequest?.group == group.group?.id } }
        .collectAsState(0)

    val hasCover = coverPhoto && group.group?.photo != null

    Div({
        classes(AppStyles.groupItemCard)

        if (shadow) {
            classes(AppStyles.groupItemCardShadow)
        }

        style {
            maxWidth?.let {
                maxWidth(it)
            }
            styles()
        }
    }) {
        if (hasCover) {
            Div({
                style {
                    width(100.percent)
                    height(16.r)
                    background("url($baseUrl${group.group?.photo})")
                    backgroundSize("cover")
                    backgroundPosition("center")
                    borderRadius(1.r, 1.r, 0.r, 0.r)
                    cursor("pointer")
                }

                onClick {
                    onSelected()
                }
            }) {}
        }

        Div({
            classes(
                buildList {
                    add(AppStyles.groupItem)
                    if (selected) {
                        add(AppStyles.groupItemSelected)
                    }
                    if (!selectable) {
                        add(AppStyles.groupItemDefault)
                    }
                    if (onBackground) {
                        add(AppStyles.groupItemOnBackground)
                    }
                    if (onSurface) {
                        add(AppStyles.groupItemOnSurface)
                    }
                }
            )
            style {
                if (hasCover) {
                    borderRadius(0.r, 0.r, 1.r, 1.r)
                }
            }
            onClick {
                onSelected()
            }

            focusable()
        }) {
            GroupPhoto(group, me)
            Div({
                style {
                    width(0.px)
                    flexGrow(1)
                }
            }) {
                Div({
                    classes(AppStyles.groupItemName)

                    style {
                        if (group.isUnread(myMember?.member)) {
                            fontWeight("bold")
                        }
                    }
                }) {
                    Text(group.name(appString { someone }, appString { newGroup }, listOfNotNull(me?.id)))
                }
                Div({
                    classes(AppStyles.groupItemMessage)
                }) {
                    when (info) {
                        GroupInfo.LatestMessage -> {
                            if (group.latestMessage?.member == myMember?.member?.id) {
                                Text("${appString { you }}: ")
                            } else if (group.members!!.size > 2 && group.latestMessage != null) {
                                Text("${group.latestMessagePersonOrBotName}: ")
                            }
                            Text(
                                group.latestMessage?.preview() ?: "${appString { created }} ${
                                    formatDistanceToNow(
                                        Date(group.group!!.createdAt!!.toEpochMilliseconds()),
                                        js("{ addSuffix: true }")
                                    )
                                }"
                            )
                        }

                        GroupInfo.Members -> {
                            Text(
                                buildString {
                                    append(group.members!!.size.toLocaleString())
                                    append(" ")
                                    if (group.members!!.size == 1) {
                                        append(appString { inlineMember })
                                    } else {
                                        append(appString { inlineMembers })
                                    }
                                    if (group.group?.description.isNullOrBlank().not()) {
                                        append(" • ")
                                        append(group.group!!.description)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            when {
                inCall > 0 -> {
                    Div({
                        style {
                            marginLeft(.5.r)
                            flexShrink(0)
                        }
                    }) {
                        Span({
                            style {
                                color(Styles.colors.tertiary)
                                fontWeight("bold")
                                fontSize(14.px)
                            }
                        }) {
                            // todo translate
                            Text("$inCall in call")
                        }
                    }
                }

                info == GroupInfo.Members && myMember != null -> {
                    Div({
                        style {
                            marginLeft(.5.r)
                            flexShrink(0)
                            color(Styles.colors.primary)
                            fontWeight("bold")
                            fontSize(14.px)
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                        }
                    }) {
                        Span { appText { joined } }
                        Icon("check") {
                            marginLeft(.5.r)
                        }
                    }
                }

                joinRequestCount > 0 -> {
                    Div({
                        style {
                            marginLeft(.5.r)
                            flexShrink(0)
                        }
                    }) {
                        Span({
                            style {
                                color(Styles.colors.primary)
                                fontWeight("bold")
                                fontSize(14.px)
                            }
                        }) {
                            if (joinRequestCount == 1) {
                                Text("$joinRequestCount ${appString { inlinePersonWaiting }}")
                            } else {
                                Text("$joinRequestCount ${appString { inlinePeopleWaiting }}")
                            }
                        }
                    }
                }

                group.latestMessage != null -> {
                    Div({
                        style {
                            marginLeft(.5.r)
                            flexShrink(0)
                        }
                    }) {
                        Span({
                            style {
                                if (group.isUnread(myMember?.member)) {
                                    color(Styles.colors.primary)
                                    fontWeight("bold")
                                } else {
                                    color(Styles.colors.secondary)
                                    opacity(.5)
                                }
                                fontSize(14.px)
                            }
                        }) {
                            Text(
                                " ${
                                    group.group?.seen?.let {
                                        formatDistanceToNow(
                                            Date(it.toEpochMilliseconds()),
                                            js("{ addSuffix: true }")
                                        )
                                    }
                                }"
                            )
                        }
                    }
                }
            }
            if (group.pin == true) {
                Icon("keep") {
                    marginLeft(.5.r)
                    fontSize(16.px)
                    opacity(.25f)
                }
            }
        }
    }
}

val GroupExtended.latestMessagePersonOrBotName
    @Composable
    get() =
        (members?.find { it.member?.id == latestMessage!!.member }?.person?.name)
            ?: (bots?.find { it.id == latestMessage!!.bot }?.name)
            ?: appString { someone }

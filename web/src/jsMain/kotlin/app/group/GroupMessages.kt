import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.AppNavigation
import app.AppStyles
import app.ailaai.api.groupTopReactions
import app.ailaai.api.messages
import app.ailaai.api.messagesBefore
import app.ailaai.api.myTopReactions
import app.ailaai.api.reactToMessage
import app.ailaai.api.setMessageRating
import app.appNav
import app.components.LoadMore
import app.components.LoadMoreState
import app.dialog.replyInNewGroupDialog
import app.group.GroupMessageBar
import app.group.JoinGroupLayout
import app.menu.Menu
import app.messaages.MessageItem
import app.messaages.preview
import app.nav.NavSearchInput
import app.rating.setRatingDialog
import app.reaction.addReactionDialog
import com.queatz.db.GroupExtended
import com.queatz.db.GroupMessagesConfig
import com.queatz.db.MemberAndPerson
import com.queatz.db.Message
import com.queatz.db.Rating
import com.queatz.db.ReactBody
import com.queatz.db.Reaction
import components.IconButton
import components.Loading
import components.ProfilePhoto
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement

@Composable
fun GroupMessages(
    group: GroupExtended,
    showSearch: Boolean,
    onShowSearch: (Boolean) -> Unit,
) {
    val nav = appNav
    val scope = rememberCoroutineScope()
    val me by application.me.collectAsState()
    val myMember = group.members?.find { it.person?.id == me!!.id }

    var myTopReactions by remember { mutableStateOf(emptyList<String>()) }
    var topGroupReactions by remember { mutableStateOf(emptyList<String>()) }
    var search by remember(group.group?.id) { mutableStateOf("") }
    var searchByReaction by remember(group.group?.id) { mutableStateOf("") }
    var searchByRating by remember(group.group?.id) { mutableStateOf("") }

    LaunchedEffect(Unit) {
        api.myTopReactions {
            myTopReactions = it.take(5).map { it.reaction }
        }
    }

    LaunchedEffect(group) {
        topGroupReactions = emptyList()
        api.groupTopReactions(group.group!!.id!!) {
            topGroupReactions = it.take(5).map { it.reaction }
        }
    }

    var isLoading by remember(group.group?.id) {
        mutableStateOf(true)
    }

    var messages by remember(group.group?.id) {
        mutableStateOf(emptyList<Message>())
    }

    var hasMore by remember(group.group?.id) {
        mutableStateOf(true)
    }

    val state = remember(group.group?.id) {
        LoadMoreState()
    }

    var replyMessage by remember(group.group?.id) {
        mutableStateOf<Message?>(null)
    }

    suspend fun reloadMessages() {
        api.messages(
            group = group.group!!.id!!,
            search = search.notBlank,
            reaction = searchByReaction.notBlank,
            rating = searchByRating.notBlank
        ) {
            messages = it
        }
        isLoading = false
    }

    suspend fun loadMore() {
        if (!hasMore || messages.isEmpty()) {
            return
        }
        api.messagesBefore(
            group = group.group!!.id!!,
            before = messages.lastOrNull()?.createdAt ?: return,
            search = search.notBlank,
            reaction = searchByReaction.notBlank,
            rating = searchByRating.notBlank
        ) {
            val messagesCount = messages.size
            messages = (messages + it).distinctBy { it.id }

            if (messages.size == messagesCount) {
                hasMore = false
            }
        }
    }

    LaunchedEffect(
        group.group?.id,
        search,
        searchByReaction,
        searchByRating
    ) {
        isLoading = search.isBlank() && searchByReaction.isBlank() && searchByRating.isBlank()
        reloadMessages()
    }

    LaunchedEffect(group.group?.id) {
        push.events.collectLatest {
            reloadMessages()
        }
    }

    LaunchedEffect(group.group?.id) {
        push.reconnect.collectLatest {
            reloadMessages()
        }
    }

    LaunchedEffect(messages.firstOrNull()?.id) {
        state.scrollToBottom()
    }

    if (isLoading) {
        Loading()
    } else {
        if (showSearch) {
            var menuTarget by remember {
                mutableStateOf<DOMRect?>(null)
            }

            if (menuTarget != null) {
                Menu({ menuTarget = null }, menuTarget!!) {
                    // todo translate
                    item("Reaction") {
                        scope.launch {
                            val reaction = addReactionDialog(
                                quickReactions = topGroupReactions + myTopReactions,
                                confirmButton = application.appString { this.search }
                            )

                            if (reaction != null) {
                                searchByReaction = reaction
                            }
                        }
                    }
                    // todo translate
                    item("Rating") {
                        scope.launch {
                            val rating = setRatingDialog(
                                confirmButton = application.appString { this.search },
                                onRemoveRating = {
                                    searchByRating = ""
                                }
                            )

                            if (rating != null) {
                                searchByRating = rating
                            }
                        }
                    }
                }
            }

            Div({
                classes(AppStyles.messageBar)
            }) {
                IconButton(
                    name = "filter_list",
                    title = appString { filter },
                    styles = { marginLeft(1.r) },
                    count = listOf(searchByReaction, searchByRating).count { it.isNotBlank() }
                ) {
                    menuTarget = if (menuTarget == null) {
                        (it.target as HTMLElement).getBoundingClientRect()
                    } else {
                        null
                    }
                }
                NavSearchInput(
                    value = search,
                    onChange = {
                        search = it
                    },
                    onDismissRequest = {
                        onShowSearch(false)
                    },
                    styles = {
                        width(100.percent)
                        height(3.5.r)
                    },
                    defaultMargins = false
                )
            }
        } else if (myMember != null) {
            if (group.group?.config?.messages == null || myMember.member?.host == true) {
                GroupMessageBar(group, replyMessage, { replyMessage = null }) {
                    reloadMessages()
                }
            } else {
                Div({
                    style {
                        height(1.r)
                    }
                }) { }
            }
        } else {
            JoinGroupLayout(group)
        }
        LoadMore(
            state,
            hasMore && messages.isNotEmpty(),
            attrs = {
                classes(AppStyles.messages)
            },
            onLoadMore = {
                scope.launch {
                    loadMore()
                }
            }
        ) {
            messages.forEachIndexed { index, message ->
                val title = (message.text?.notBlank ?: message.preview())?.let { "\"$it\"" } ?: appString { reply }

                val members = group.members ?: emptyList()
                val nextMessage = if (index > 0) messages[index - 1] else null

                val seenUntilHere = members.filter {
                    it.member?.id != myMember?.member?.id && it.hasSeen(message) && (nextMessage == null || !it.hasSeen(
                        nextMessage
                    ))
                }

                seenUntilHere.notEmpty?.let { members ->
                    Div({
                        classes(AppStyles.seenUntilLayout)
                    }) {
                        members.forEach {
                            ProfilePhoto(it.person!!, size = 18.px, fontSize = 12.px)
                        }
                    }
                }

                MessageItem(
                    message = message,
                    previousMessage = if (index < messages.lastIndex) messages[index + 1] else null,
                    member = group.members?.find { member -> member.member?.id == message.member },
                    bot = group.bots?.find { bot -> bot.id == message.bot },
                    myMember = myMember,
                    bots = group.bots ?: emptyList(),
                    canReply = group.group?.config?.messages != GroupMessagesConfig.Hosts,
                    canReact = myMember != null,
                    onReply = {
                        replyMessage = message
                    },
                    onReact = {
                        scope.launch {
                            val reaction = addReactionDialog(topGroupReactions + myTopReactions)?.notBlank

                            if (reaction != null) {
                                api.reactToMessage(message.id!!, ReactBody(reaction = Reaction(reaction = reaction))) {
                                    reloadMessages()
                                }
                            }
                        }
                    },
                    onRate = { rating ->
                        scope.launch {
                            val result = setRatingDialog(rating) {
                                scope.launch {
                                    api.setMessageRating(
                                        id = message.id!!,
                                        rating = Rating(rating = null)
                                    ) {
                                        reloadMessages()
                                    }
                                }
                            }?.trimStart('+')?.toIntOrNull()

                            if (result != null) {
                                api.setMessageRating(
                                    id = message.id!!,
                                    rating = Rating(rating = result)
                                ) {
                                    reloadMessages()
                                }
                            }
                        }
                    },
                    onReplyInNewGroup = {
                        scope.launch {
                            replyInNewGroupDialog(title, scope, group, message) {
                                nav.navigate(AppNavigation.Group(it.id!!))
                            }
                        }
                    }
                ) {
                    scope.launch {
                        reloadMessages()
                    }
                }
            }
        }
    }
}

private fun MemberAndPerson.hasSeen(message: Message) = (member?.seen ?: Instant.DISTANT_PAST) >= message.createdAt!!

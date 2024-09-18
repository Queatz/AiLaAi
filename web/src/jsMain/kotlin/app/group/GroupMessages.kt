import androidx.compose.runtime.*
import app.AppNavigation
import app.AppStyles
import app.ailaai.api.messages
import app.ailaai.api.messagesBefore
import app.appNav
import app.components.LoadMore
import app.components.LoadMoreState
import app.dialog.replyInNewGroupDialog
import app.group.GroupMessageBar
import app.group.JoinGroupLayout
import app.messaages.MessageItem
import app.messaages.preview
import com.queatz.db.GroupExtended
import com.queatz.db.Message
import components.Loading
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.dom.Div

@Composable
fun GroupMessages(group: GroupExtended) {
    val nav = appNav
    val scope = rememberCoroutineScope()
    val me by application.me.collectAsState()
    val myMember = group.members?.find { it.person?.id == me!!.id }

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

    var replyMessage by remember {
        mutableStateOf<Message?>(null)
    }

    suspend fun reloadMessages() {
        api.messages(group.group!!.id!!) {
            messages = it
        }
        isLoading = false
    }

    suspend fun loadMore() {
        if (!hasMore || messages.isEmpty()) {
            return
        }
        api.messagesBefore(
            group.group!!.id!!,
            before = messages.lastOrNull()?.createdAt ?: return
        ) {
            val messagesCount = messages.size
            messages = (messages + it).distinctBy { it.id }

            if (messages.size == messagesCount) {
                hasMore = false
            }
        }
    }

    LaunchedEffect(group.group?.id) {
        isLoading = true
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
        if (myMember != null) {
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
                MessageItem(
                    message = message,
                    previousMessage = if (index < messages.lastIndex) messages[index + 1] else null,
                    member = group.members?.find { member -> member.member?.id == message.member },
                    bot = group.bots?.find { bot -> bot.id == message.bot },
                    myMember = myMember,
                    bots = group.bots ?: emptyList(),
                    onReply = {
                        replyMessage = message
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

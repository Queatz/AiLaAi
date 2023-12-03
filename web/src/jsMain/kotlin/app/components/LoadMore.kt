package app.components

import androidx.compose.runtime.*
import components.Loading
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.*
import org.w3c.dom.events.Event
import r

class LoadMoreState {
    val onScrollToBottom = MutableSharedFlow<Unit>()

    suspend fun scrollToBottom() {
        onScrollToBottom.emit(Unit)
    }
}

@Composable
fun LoadMore(
    state: LoadMoreState,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var messagesDiv by remember {
        mutableStateOf<HTMLDivElement?>(null)
    }

    var loadingDiv by remember {
        mutableStateOf<HTMLDivElement?>(null)
    }

    var isLoadMoreVisible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(state) {
        state.onScrollToBottom.collectLatest {
            delay(100)
            messagesDiv?.scroll(
                ScrollToOptions(
                    top = messagesDiv!!.scrollHeight.toDouble(),
                    behavior = ScrollBehavior.SMOOTH
                )
            )
        }
    }

    DisposableEffect(messagesDiv, loadingDiv) {
        val eventListener = { event: Event ->
            if (messagesDiv != null && loadingDiv != null) {
                val loadingRect = loadingDiv!!.getBoundingClientRect()
                val messagesRect = messagesDiv!!.getBoundingClientRect()

                isLoadMoreVisible = loadingRect.overlaps(messagesRect)
            }
        }

        eventListener(Event("scroll"))

        messagesDiv?.addEventListener("scroll", eventListener)

        onDispose {
            messagesDiv?.removeEventListener("scroll", eventListener)
        }
    }

    LaunchedEffect(isLoadMoreVisible) {
        if (isLoadMoreVisible) {
            onLoadMore()
        }
    }

    Div({
        attrs?.invoke(this)
        ref {
            messagesDiv = it

            onDispose {
                messagesDiv = null
            }
        }
    }) {
        content()

        if (hasMore) {
            Loading {
                style {
                    padding(1.r)
                }
                ref { element ->
                    loadingDiv = element

                    onDispose {
                        loadingDiv = null
                    }
                }
            }
        }
    }
}

private fun DOMRect.overlaps(other: DOMRect) = (other.bottom > top && other.top < bottom && other.right > left && other.left < right)

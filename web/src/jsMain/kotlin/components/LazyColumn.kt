package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

class LazyDsl internal constructor() {
    var all by mutableStateOf(listOf<@Composable () -> Unit>())

    fun item(block: @Composable () -> Unit) {
        all = all + @Composable {
            block()
        }
    }

    fun <T> items(items: List<T>, block: @Composable (item: T) -> Unit) {
        all = all + items.map {
            @Composable {
                block(it)
            }
        }
    }
}

/**
 * Only works when the immediate parent is scrollable
 */
@Composable
fun LazyColumn(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    block: LazyDsl.() -> Unit,
) {
    LazyList(
        attrs = attrs,
        horizontal = false,
        block = block
    )
}

/**
 * Only works when the immediate parent is scrollable
 */
@Composable
fun LazyRow(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    block: LazyDsl.() -> Unit,
) {
    LazyList(
        attrs = attrs,
        horizontal = true,
        block = block
    )
}

@Composable
fun LazyList(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
//    itemAttrs: AttrBuilderContext<HTMLDivElement>? = null,
    horizontal: Boolean,
    pageSize: Int = 20,
    block: LazyDsl.() -> Unit,
) {
    val dsl = remember(block) { LazyDsl().apply { block() } }
    var shownCount by remember { mutableStateOf(pageSize) }
    var element by remember { mutableStateOf<HTMLDivElement?>(null) }

    fun calc() {
        if (dsl.all.isEmpty()) return
        if (shownCount >= dsl.all.size) return

        element?.let { ref ->
            var scrollableParent = ref.parentElement
            while (scrollableParent != null) {
                val style = window.getComputedStyle(scrollableParent)
                val overflowY = style.getPropertyValue("overflow-y")
                val overflow = style.getPropertyValue("overflow")
                if (overflowY == "auto" || overflowY == "scroll" || overflow == "auto" || overflow == "scroll") {
                    break
                }
                scrollableParent = scrollableParent.parentElement
            }

            val parent = scrollableParent ?: ref.parentElement ?: return@let

            val elementRect = ref.getBoundingClientRect()
            val parentRect = parent.getBoundingClientRect()

            val reached = if (horizontal) {
                elementRect.right <= parentRect.right + 100
            } else {
                elementRect.bottom <= parentRect.bottom + 100
            }

            if (reached) {
                shownCount += pageSize
            }
        }
    }

    LaunchedEffect(element, dsl.all.size) {
        while (shownCount < dsl.all.size) {
            delay(50)
            calc()
        }
    }

    Div({
        style {
            display(DisplayStyle.Flex)

            if (!horizontal) {
                flexDirection(FlexDirection.Column)
            }
        }

        ref {
            element = it

            onDispose {
                element = null
            }
        }

        attrs?.invoke(this)
    }) {
        dsl.all.take(shownCount).forEach {
            Div {
                it()
            }
        }
    }
}

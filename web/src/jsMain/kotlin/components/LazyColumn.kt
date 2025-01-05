package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

class LazyDsl internal constructor() {
    val all = mutableListOf<@Composable () -> Unit>()

    fun item(block: @Composable () -> Unit) {
        all += @Composable {
            block()
        }
    }

    fun <T : Any> items(items: List<T>, block: @Composable (item: T) -> Unit) {
        all += items.map {
            @Composable {
                block(it)
            }
        }
    }
}

/**
 * Only works when the immediate parent is scrolled
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
 * Only works when the immediate parent is scrolled
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

    var isBottomReached by remember { mutableStateOf(false) }

    fun calc() {
        isBottomReached = if (dsl.all.isEmpty()) {
            false
        } else {
            element?.let { ref ->
                val parent = ref.parentElement ?: return@let false

                val elementRect = ref.getBoundingClientRect()
                val parentRect = parent.getBoundingClientRect()

                if (horizontal) {
                    false//
                } else {
                    elementRect.bottom <= parentRect.bottom
                }
            } == true
        }
    }

    LaunchedEffect(isBottomReached) {
        if (isBottomReached) {
            shownCount += pageSize
        }
    }

    // todo
    LaunchedEffect(element) {
        while (true) {
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

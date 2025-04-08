package app.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.AppStyles
import components.Icon
import focusable
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.MouseEvent
import parents
import r

private val onMenuOpened by lazy {
    MutableSharedFlow<Unit>()
}

class MenuScope(val onDismissRequest: () -> Unit) {
    @Composable
    fun item(
        title: String,
        selected: Boolean = false,
        icon: String? = null,
        textIcon: String? = null,
        iconTitle: String? = null,
        onIconClick: (() -> Unit)? = null,
        onClick: () -> Unit
    ) {
        Div({
            classes(
                if (selected) {
                    listOf(AppStyles.menuItem, AppStyles.menuItemSelected)
                } else {
                    listOf(AppStyles.menuItem)
                }
            )

            focusable()

            onClick {
                it.stopPropagation()
                onDismissRequest()
                onClick()
            }
        }) {
            Span {
                Text(title)
            }
            if (textIcon != null) {
                Div(
                    attrs = {
                        style {
                            opacity(0.5)
                            fontWeight("bold")
                            display(DisplayStyle.Flex)
                            width(24.px)
                            height(24.px)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.Center)
                        }
                    }
                ) {
                    Text(textIcon)
                }
            }
            if (icon != null) {
                Icon(
                    name = icon,
                    title = iconTitle,
                    onClick = if (onIconClick == null) {
                        null
                    } else {
                        {
                            onDismissRequest()
                            onIconClick()
                        }
                    }) {
                    flexShrink(0)
                    marginLeft(1.r)
                    opacity(.5)
                }
            }
        }
    }
}

/**
 * @param target The rect to position the menu around, in screen space.
 *      You must calculate this based on where the rect
 *      is considered to be in the DOM, and where you
 *      place this Menu in the DOM.
 */
@Composable
fun Menu(
    onDismissRequest: () -> Unit,
    target: DOMRect,
    above: Boolean = false,
    useOffsetParent: Boolean = false,
    content: @Composable MenuScope.() -> Unit
) {
    var y by remember { mutableStateOf(0) }
    var measured by remember { mutableStateOf(false) }

    fun keepInFrame(element: HTMLElement): Int {
        val distance = element.getBoundingClientRect().bottom - window.innerHeight
        return distance.toInt().coerceAtLeast(0)
    }

    // Close when other menus are opened
    LaunchedEffect(Unit) {
        onMenuOpened.emit(Unit)
        delay(100)
        onMenuOpened.collect {
            onDismissRequest()
        }
    }

    Div({
        classes(AppStyles.menu)

        style {
            if (!measured) {
                opacity(0)
            }

            target.let { it.left + it.width to it.top + (if (above) 0.0 else it.height) }
                .let { (left, top) ->
                    top((top - y).px)
                    left(left.px)
                }
        }

        onClick {
            it.stopPropagation()
            onDismissRequest()
        }

        ref { menuElement ->
            val windowClickListener = EventListener {
                val parents = ((it as? MouseEvent)?.target as? HTMLElement)?.parents
                if (parents?.none { it == menuElement } == true) {
                    onDismissRequest()
                }
            }

            if (above) {
                y = menuElement.clientHeight
            } else {
                y = keepInFrame(menuElement)
            }

            if (useOffsetParent) {
                menuElement.offsetParent?.let {
                    val parentRect = it.getBoundingClientRect()
                    if (parentRect.top != 0.0) {
                        y += parentRect.top.toInt()
                    }
                }
            }

            menuElement.onresize = {
                if (above) {
                    y = menuElement.clientHeight
                } else {
                    y = keepInFrame(menuElement)
                }
            }

            measured = true

            val windowResizeListener = EventListener { onDismissRequest() }
            document.addEventListener("click", windowClickListener)
            window.addEventListener("resize", windowResizeListener)

            onDispose {
                document.removeEventListener("click", windowClickListener)
                window.removeEventListener("resize", windowResizeListener)
            }
        }
    }) {
        MenuScope(onDismissRequest = onDismissRequest).content()
    }
}

@Composable
fun InlineMenu(
    onDismissRequest: () -> Unit,
    content: @Composable MenuScope.() -> Unit
) {
    Div({
        classes(AppStyles.menuInline)

        onClick {
            onDismissRequest()
        }
    }) {
        MenuScope(onDismissRequest = onDismissRequest).content()
    }
}


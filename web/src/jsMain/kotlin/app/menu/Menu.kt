package app.menu

import androidx.compose.runtime.Composable
import app.AppStyles
import components.Icon
import focusable
import kotlinx.browser.document
import kotlinx.browser.window
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

class MenuScope {
    @Composable
    fun item(title: String, selected: Boolean = false, icon: String? = null, onClick: () -> Unit) {
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
                onClick()
            }
        }) {
            Span {
                Text(title)
            }
            if (icon != null) {
                Icon(icon) {
                    flexShrink(0)
                    marginLeft(1.r)
                    opacity(.5)
                }
            }
        }
    }
}

@Composable
fun Menu(
    onDismissRequest: () -> Unit,
    target: DOMRect,
    content: @Composable MenuScope.() -> Unit
) {
    Div({
        classes(AppStyles.menu)

        style {
            target.let { it.left + it.width to it.top + it.height }
                .let { (left, top) ->
                    top(top.px)
                    left(left.px)
                }
        }

        onClick {
            onDismissRequest()
        }

        ref { menuElement ->
            val clickListener = EventListener {
                val parents = ((it as? MouseEvent)?.target as? HTMLElement)?.parents
                if (parents?.none { it == menuElement } == true) {
                    onDismissRequest()
                }
            }
            val resizeListener = EventListener { onDismissRequest() }
            document.addEventListener("click", clickListener)
            window.addEventListener("resize", resizeListener)

            onDispose {
                document.removeEventListener("click", clickListener)
                window.removeEventListener("resize", resizeListener)
            }
        }
    }) {
        MenuScope().content()
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
        MenuScope().content()
    }
}


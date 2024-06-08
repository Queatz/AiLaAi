package app

import androidx.compose.runtime.Composable
import androidx.compose.web.events.SyntheticMouseEvent
import appString
import components.IconButton
import ellipsize
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement
import r

@Composable
fun PageTopBar(
    title: String,
    description: String? = null,
    actions: @Composable ElementScope<HTMLDivElement>.() -> Unit = {},
    navActions: @Composable ElementScope<HTMLDivElement>.() -> Unit = {},
    onTitleClick: (() -> Unit)? = null,
    onDescriptionClick: (() -> Unit)? = null,
    onMenu: ((SyntheticMouseEvent) -> Unit)? = null
) {
    Div({
        style {
            display(DisplayStyle.Flex)
            padding(.5.r)
            margin(1.r, .5.r, .5.r, .5.r)
            alignItems(AlignItems.Center)
            overflow("hidden")
            flexShrink(0)
        }
    }) {
        navActions()
        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                flex(1)
                overflow("hidden")
            }
        }) {
            Div({
                style {
                    fontSize(24.px)
                    ellipsize()
                }

                onTitleClick?.let {
                    onClick {
                        it()
                    }
                }
            }) {
                Text(title)
            }

            if (!description.isNullOrBlank()) {
                Div({
                    classes(AppStyles.groupItemMessage)

                    style {
                        ellipsize()
                    }

                    onDescriptionClick?.let {
                        onClick {
                            it()
                        }
                    }
                }) {
                    Text(description)
                }
            }
        }
        actions()
        IconButton("more_vert", appString { options }, styles = {
            flexShrink(0)
            fontWeight("bold")
        }) {
            onMenu?.invoke(it)
        }
    }
}

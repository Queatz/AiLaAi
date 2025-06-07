package app.widget

import StyleManager
import Styles
import app.dark
import app.mobile
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position.Companion.Absolute
import org.jetbrains.compose.web.css.Position.Companion.Relative
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.ms
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.rgba
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.textDecoration
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.transitions
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.css.width
import r
import shadow

val WidgetStyles get() = StyleManager.style(WidgetStyleSheet::class)

@OptIn(ExperimentalComposeWebApi::class)
class WidgetStyleSheet : StyleSheet() {
    val spaceContainer by style {
        position(Relative)
        width(100.percent)
        property("aspect-ratio", "2")
        borderRadius(1.r)

        mobile(self) {
            property("aspect-ratio", ".5")
        }
    }

    val space by style {
        width(100.percent)
        height(100.percent)
        property("aspect-ratio", "2")
        borderRadius(1.r)
        shadow()
        backgroundColor(Styles.colors.white)

        dark(self) {
            backgroundColor(Styles.colors.dark.surface)
        }
    }

    val spacePathToolbar by style {
        position(Absolute)
        bottom(0.r)
        left(0.r)
        textAlign("center")
        padding(1.r)
        property("z-index", 1)
        display(DisplayStyle.Flex)

        transitions {
            "color" {
                duration = 100.ms
            }
            "background-color" {
                duration = 100.ms
            }
        }
    }

    val spaceSidePanel by style {
        position(Absolute)
        top(0.r)
        right(0.r)
        padding(1.r)
        property("z-index", 1)
        display(DisplayStyle.Flex)
        property("flex-direction", "column")
        property("gap", "0.5rem")

        transitions {
            "color" {
                duration = 100.ms
            }
            "background-color" {
                duration = 100.ms
            }
        }
    }

    val spacePathWidget by style {
        position(Absolute)
        top(0.r)
        left(0.r)
        right(0.r)
        textAlign("center")
        padding(1.r)
        property("z-index", 1)
        property("pointer-events", "none")
    }

    val spacePathItem by style {
        cursor("pointer")
        color(Styles.colors.black)
        property("pointer-events", "auto")

        hover(self) style {
            textDecoration("underline")
        }

        dark(self) {
            color(Styles.colors.white)
        }
    }

    val tableCenter by style {
    }

    val columnSelected by style {
    }

    val pageTreeItem by style {
        display(DisplayStyle.Flex)

        child(self, selector("div")) style {
            padding(1.r)
            borderRadius(.5.r)
            border(2.px, LineStyle.Solid, Styles.colors.background)
            whiteSpace("pre-wrap")
        }

        dark(self) {
            child(self, selector("div")) style {
                border(2.px, LineStyle.Solid, Styles.colors.black)
            }
        }
    }

    val pageTree by style {
        width(100.percent)

        child(self, not(lastChild)) style {
            marginBottom(1.r)
        }
    }

    val table by style {
        property("border-spacing", "${.5.r}")
        property("border", "none")
        width(100.percent)

        desc(self, selector("th")) style {
            padding(.5.r, 1.r)
            textAlign("start")
            cursor("pointer")
            opacity(.5f)

            self + className(columnSelected) style {
                opacity(1f)
            }

            self + className(tableCenter) style {
                textAlign("center")
            }
        }
        desc(self, selector("td")) style {
            textAlign("start")
            padding(1.r)
            borderRadius(.5.r)
            border(2.px, LineStyle.Solid, Styles.colors.background)
            whiteSpace("pre-wrap")

            self + className(tableCenter) style {
                textAlign("center")
            }
        }

        dark(self) {
            desc(self, selector("td")) style {
                border(2.px, LineStyle.Solid, Styles.colors.black)
            }
        }
    }

    // Slide list panel (thumbnails)
    val slideListPanel by style {
        position(Absolute)
        top(4.r)
        right(1.r)
        width(12.r)
        property("max-height", "calc(100% - 6rem)")
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        gap(0.5.r)
        padding(0.5.r)
        backgroundColor(Styles.colors.background)
        borderRadius(1.r)
        overflow("auto")
        property("pointer-events", "initial")
        property("z-index", "10")
        property("box-shadow", "0 4px 8px rgba(0, 0, 0, 0.1)")
        dark(self) {
            backgroundColor(Styles.colors.dark.surface)
        }
    }

    val slideListPanelHeader by style {
        padding(0.5.r)
        fontSize(16.px)
        fontWeight("bold")
        border(0.px, LineStyle.Solid, Styles.colors.outline)
        property("border-bottom-width", "1px")
        marginBottom(0.5.r)
    }

    val slideListPanelEmpty by style {
        padding(1.r)
        color(Styles.colors.secondary)
        textAlign("center")
    }

    val slideListPanelItem by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.SpaceBetween)
        padding(0.5.r)
        borderRadius(0.5.r)
        cursor("pointer")

        property("transition", "background-color 0.1s")

        hover(self) style {
            backgroundColor(Styles.colors.white)
        }

        dark(self) {
            hover(self) style {
                backgroundColor(Styles.colors.black)
            }
        }
    }

    val slideListPanelItemSelected by style {
        backgroundColor(Styles.colors.primary)
        color(Styles.colors.white)

        hover(self) style {
            backgroundColor(Styles.colors.primary)
            color(Styles.colors.white)
        }

        dark(self) {
            hover(self) style {
                backgroundColor(Styles.colors.primary)
                color(Styles.colors.white)
            }
        }
    }

    // Controls overlay for slideshow navigation
    val slideshowControls by style {
        position(Absolute)
        bottom(4.r)
        borderRadius(1.r)
        property("left", "50%")
        property("transform", "translateX(-50%)")
        display(DisplayStyle.Flex)
        justifyContent(JustifyContent.Center)
        padding(.5.r)
        backgroundColor(rgba(0, 0, 0, 0.5))

        dark(self) {
            backgroundColor(rgba(255, 255, 255, 0.1))
        }
    }

    val slideshowControlsCounter by style {
        color(Styles.colors.white)
        margin(0.r, 0.5.r)
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
    }
}

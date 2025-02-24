package app.widget

import app.dark
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.Position.Companion.Absolute
import r

@OptIn(ExperimentalComposeWebApi::class)
object WidgetStyles : StyleSheet() {
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

    val spacePathWidget by style {
        position(Absolute)
        top(0.r)
        left(0.r)
        right(0.r)
        textAlign("center")
        padding(1.r)
        property("z-index", 1)
    }

    val spacePathItem by style {
        cursor("pointer")
        color(Color.black)

        hover(self) style {
            textDecoration("underline")
        }

        dark(self) {
            color(Color.white)
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
                border(2.px, LineStyle.Solid, Color.black)
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
                border(2.px, LineStyle.Solid, Color.black)
            }
        }
    }
}

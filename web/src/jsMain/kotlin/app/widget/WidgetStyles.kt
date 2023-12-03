package app.widget

import app.dark
import org.jetbrains.compose.web.css.*
import r

object WidgetStyles : StyleSheet() {
    val tableCenter by style {
    }

    val columnSelected by style {
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

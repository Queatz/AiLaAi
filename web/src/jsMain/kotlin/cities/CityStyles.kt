package cities
import app.dark
import org.jetbrains.compose.web.css.*
import r

object CityStyles : StyleSheet() {
    val cities by style {
        display(DisplayStyle.Flex)
        flexWrap(FlexWrap.Wrap)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Center)
    }

    val gradient by style {
        position(Position.Absolute)
        background("linear-gradient(to bottom, transparent 20%, rgba(255, 255, 255, 0.95) 80%)")
        property("inset", "0")

        media(mediaMaxWidth(640.px)) {
            self style {
                background("linear-gradient(to bottom, transparent 00%, rgba(255, 255, 255, 0.95) 60%)")
            }
        }

        dark(self) {
            background("linear-gradient(to bottom, transparent 20%, rgba(0, 0, 0, 0.65) 80%)")
        }
    }

    val city by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Stretch)
        padding(1.r)
        backgroundPosition("center")
        backgroundSize("cover")
        borderRadius(2.r)
        property("aspect-ratio", "1.5")
        property("box-shadow", "2px 2px 8px rgba(0, 0, 0, .25)")
        backgroundColor(Styles.colors.background)
        overflow("hidden")
        cursor("pointer")
        marginBottom(1.r)
        width(100.percent)
        boxSizing("border-box")
        justifyContent(JustifyContent.End)
        fontSize(42.px)
        property("text-shadow", "rgba(0, 0, 0, .25) 1px 1px 1px")

        media(mediaMaxWidth(640.px)) {
            self style {
                fontSize(32.px)
            }
        }
    }

    val cityAbout by style {
        media(mediaMaxWidth(640.px)) {
            self style {
                display(DisplayStyle.None)
            }
        }
    }
}

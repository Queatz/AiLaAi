package app.cards

import Styles
import org.jetbrains.compose.web.css.*
import r

val CardsPageStyles get() = StyleManager.style(CardsPageStyleSheet::class)

class CardsPageStyleSheet : StyleSheet() {
    val layout by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        flexWrap(FlexWrap.Wrap)
        position(Position.Relative)

        child(self, className(Styles.card)) style {
            self style {
                width(320.px)
                marginTop(1.r)
                marginLeft(1.r)
            }
        }
    }
}

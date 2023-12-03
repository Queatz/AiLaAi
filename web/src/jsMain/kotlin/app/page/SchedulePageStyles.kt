package app.page

import Styles
import Styles.elevated
import app.dark
import org.jetbrains.compose.web.css.*
import r

object SchedulePageStyles : StyleSheet() {
    val title by style {
        marginBottom(.5.r)
        fontSize(18.px)
        display(DisplayStyle.Flex)
    }

    val section by style {
        borderRadius(1.r)
        padding(.5.r)
        marginBottom(1.r)
        backgroundColor(Color.white)
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        overflow("hidden")
        elevated()

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
            property("border", "none")
        }
    }

    val rowActions by style {
        opacity(0)
        lineHeight("0")
        color(Styles.colors.primary)
        display(DisplayStyle.Flex)
    }

    val rowText by style {

    }

    val row by style {
        display(DisplayStyle.Flex)
        padding(.5.r)
        borderRadius(.5.r)
        cursor("pointer")
        alignItems(AlignItems.Center)
        opacity(1)

        self + hover style {
            backgroundColor(Styles.colors.background)

            child(self, className(rowActions)) style {
                opacity(1)
            }
        }

        self + focus style {
            backgroundColor(Styles.colors.background)

            child(self, className(rowActions)) style {
                opacity(1)
            }
        }

        dark(self) {
            self + hover style {
                backgroundColor(Color.black)
            }

            self + focus style {
                backgroundColor(Color.black)
            }

            child(self, className(rowActions)) style {
                color(Color.white)
            }
        }
    }
}

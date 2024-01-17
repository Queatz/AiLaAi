package app.call

import app.AppStyles
import app.dark
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.*
import r
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalComposeWebApi::class)
object CallStyles : StyleSheet() {
    val participantControls by style {
        position(Position.Absolute)
        display(DisplayStyle.Flex)
        justifyContent(JustifyContent.Center)
        alignItems(AlignItems.Center)
        property("inset", "0")

        child(self, className(AppStyles.iconButton)) style {
            opacity(0)
            backgroundColor(Color("rgba(255 255 255 / 50%)"))

            transitions {
                "opacity" {
                    duration = .5.s
                }
            }
        }

        dark(self) {
            child(self, className(AppStyles.iconButton)) style {
                backgroundColor(Color("rgba(0 0 0 / 50%)"))
            }
        }

        self + hover style {
            child(self, className(AppStyles.iconButton)) style {
                opacity(1)
            }
        }
    }

    val callRootFullscreen by style { }

    val callRoot by style {
        position(Position.Fixed)
        display(DisplayStyle.Flex)
        borderRadius(1.r)
        backgroundColor(Color.black)
        cursor("pointer")
        property("z-index", "100")
        property("box-shadow", "2px 2px 8px rgba(0, 0, 0, .25)")
        top(5.r)
        right(1.r)
        overflow("hidden")
        transform {
            translateZ(1.px)
        }

        self + not(className(callRootFullscreen)) style {
            width(18.r)
            height(12.r)
        }
        self + className(callRootFullscreen) style {
            top(1.r)
            right(1.r)
            bottom(1.r)
            left(1.r)
        }
    }
}

import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.AnimationTimingFunction.Companion.Linear
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.animation
import org.jetbrains.compose.web.css.background
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.duration
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.iterationCount
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.s
import org.jetbrains.compose.web.css.timingFunction
import org.jetbrains.compose.web.css.transform
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.width

@OptIn(ExperimentalComposeWebApi::class)
object EffectStyles : StyleSheet() {
    val container by style {
        position(Position.Fixed)
        property("inset", "0")
        property("z-index", 10)
        property("pointer-events", "none")
    }

    val dropKeyframes by keyframes {
        from {
            transform {
                translateY(0.vh)
            }
        }

        to {
            transform {
                translateY(100.vh)
            }
        }
    }

    val drop by style {
        position(Position.Fixed)
        width(1.px)
        height(10.vh)
        bottom(100.percent)
        background("linear-gradient(to bottom, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.25)")
        animation(dropKeyframes) {
            timingFunction(Linear)
            duration(1.s)
            iterationCount(null)
        }
    }
}

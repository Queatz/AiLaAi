package app.game.editor

import app.dark
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.boxSizing
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.transform
import org.jetbrains.compose.web.css.unaryMinus
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.css.width
import r

/**
 * Styles for the Seekbar component
 */
@OptIn(ExperimentalComposeWebApi::class)
object SeekbarStyles : StyleSheet() {
    val seekbarContainer by style {
        position(Position.Relative)
        width(100.percent)
        padding(2.r)
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        boxSizing("border-box")
    }

    val seekbarBar by style {
        position(Position.Relative)
        width(100.percent)
        height(0.75.r)
        backgroundColor(Color("#e0e0e0"))
        borderRadius(0.5.r)
        cursor("pointer")
        property("user-select", "none")

        dark(self) {
            backgroundColor(Color("#333333"))
        }
    }

    val seekbarCurrentFrame by style {
        position(Position.Absolute)
        width(3.px)
        height(1.r)
        backgroundColor(Styles.colors.primary)
        borderRadius(1.5.px)
        top(50.percent)
        transform {
            translateY(-50.percent)
        }
        property("z-index", "2")
    }

    val seekbarMarker by style {
        position(Position.Absolute)
        width(1.px)
        height(0.75.r)
        backgroundColor(Color("#555"))
        top(50.percent)
        transform {
            translateY(-50.percent)
        }
    }

    val seekbarMarkerLabel by style {
        position(Position.Absolute)
        fontSize(12.px)
        fontWeight("bold")
        color(Color("#555"))
        bottom(0.75.r)
        transform {
            translateX(-50.percent)
        }
        whiteSpace("nowrap")
    }

    val seekbarKeyframe by style {
        position(Position.Absolute)
        width(8.px)
        height(8.px)
        borderRadius(4.px)
        backgroundColor(Color("#ffcc00"))
        top(50.percent)
        transform {
            translate(-50.percent, -50.percent)
        }
        property("z-index", "1")
    }

    val seekbarKeyframeDuration by style {
        position(Position.Absolute)
        height(4.px)
        backgroundColor(Color("#ffcc0066"))
        borderRadius(2.px)
        top(50.percent)
        transform {
            translateY(-50.percent)
        }
    }

    val seekbarMarkersContainer by style {
        position(Position.Relative)
        width(100.percent)
        height(1.5.r)
    }
}

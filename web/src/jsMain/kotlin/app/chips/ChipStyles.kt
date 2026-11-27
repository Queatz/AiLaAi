package app.chips

import StyleManager
import Styles
import app.dark
import kotlinx.browser.document
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.boxSizing
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.css.width
import r

val ChipStyles get() = StyleManager.style(ChipStyleSheet::class)

private var keyframesInjected = false

fun ensureChipGradientKeyframes() {
    if (keyframesInjected) return
    keyframesInjected = true
    val existing = document.getElementById("ailaai-chip-gradient-keyframes")
    if (existing != null) return
    val styleEl = document.createElement("style")
    styleEl.id = "ailaai-chip-gradient-keyframes"
    styleEl.textContent = """
        @keyframes ailaaiSoftGradientShift {
            0% { background-position: 0% 50%; }
            50% { background-position: 100% 50%; }
            100% { background-position: 0% 50%; }
        }
        @keyframes ailaaiSignalBurst {
            0% { transform: scale(0.8); opacity: 1; }
            100% { transform: scale(4.2); opacity: 0; }
        }
        .ailaai-no-scrollbar::-webkit-scrollbar {
            display: none;
        }
        .ailaai-no-scrollbar {
            -ms-overflow-style: none;
            scrollbar-width: none;
        }
    """.trimIndent()
    document.head?.appendChild(styleEl)
}

class ChipStyleSheet : StyleSheet() {
    val rowViewport by style {
        width(100.percent)
        overflowX("auto")
        overflowY("hidden")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        padding(.5.r, .75.r, .25.r, .75.r)
        boxSizing("border-box")
        flexShrink(0)
        property("user-select", "none")
    }

    val rowContent by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        gap(.5.r)
        property("margin", "0 auto")
    }

    val pill by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        borderRadius(999.px)
        padding(.35.r, .95.r)
        fontSize(13.px)
        fontWeight(600)
        whiteSpace("nowrap")
        cursor("pointer")
        flexShrink(0)
        border(2.px, LineStyle.Solid, org.jetbrains.compose.web.css.Color.transparent)
        property("background-size", "220% 220%")
        property("animation", "ailaaiSoftGradientShift 20s linear infinite")
        property("transition", "transform 0.12s ease, box-shadow 0.12s ease")
        property("user-select", "none")

        self + hover style {
            property("transform", "translateY(-1px)")
            property("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.12)")
        }
    }

    val overlayItemCard by style {
        borderRadius(.75.r)
        padding(.5.r, .75.r)
        backgroundColor(Styles.colors.background)
        border(1.px, LineStyle.Solid, Styles.colors.outline)

        dark(self) {
            backgroundColor(Styles.colors.dark.surface)
            border(1.px, LineStyle.Solid, Styles.colors.dark.outline)
        }
    }
}

package app

import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.CSSSelector

fun <TBuilder> GenericStyleSheetBuilder<TBuilder>.dark(self: CSSSelector, block: TBuilder.() -> Unit) {
    media("(prefers-color-scheme: dark)") {
        self style {
            block()
        }
    }
}

fun <TBuilder> GenericStyleSheetBuilder<TBuilder>.mobile(self: CSSSelector, block: TBuilder.() -> Unit) {
    media(mediaMaxWidth(640.px)) {
        self style {
            block()
        }
    }
}

fun <TBuilder> GenericStyleSheetBuilder<TBuilder>.desktop(self: CSSSelector, block: TBuilder.() -> Unit) {
    media(mediaMinWidth(641.px)) {
        self style {
            block()
        }
    }
}

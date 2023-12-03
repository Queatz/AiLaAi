import app.dark
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.*

object Styles : StyleSheet() {
    object colors {
        val background = Color("#E0F3FF")
        val primary = Color("#006689")
        val secondary = Color("#767676")
        val tertiary = Color("#2e8900")

        object dark {
            val background = Color("#18191a")
        }
    }

    fun CSSBuilder.cardStyle() {
        borderRadius(2.r)
        backgroundColor(colors.background)
        property("box-shadow", "2px 2px 8px rgba(0, 0, 0, .25)")

        dark(self) {
            backgroundColor(colors.dark.background)
        }
    }

    fun CSSBuilder.elevated() {
        property("box-shadow", "1px 1px 4px rgba(0, 0, 0, 0.125)")
        backgroundColor(Color.white)
        borderRadius(1.r)

        dark(self) {
            backgroundColor(colors.dark.background)
        }
    }

    init {
        "a" style {
            color(colors.primary)
            fontWeight("bold")
            textDecoration("none")
        }
    }

    val mobileRow by style {
        display(DisplayStyle.Flex)

        media(mediaMaxWidth(640.px)) {
            self style {
                flexDirection(FlexDirection.Column)
            }
        }
    }

    val textIcon by style {
        fontSize(24.px)
    }
    val modal by style {
        borderRadius(2.r)
        padding(1.5.r)
        boxSizing("border-box")
        backgroundColor(colors.background)
        // todo these mke the body scroll to bottom when the dialog is opened?
        // display(DisplayStyle.Flex)
        // flexDirection(FlexDirection.Column)
        property("max-height", "calc(100vh - 2rem)")
        property("max-width", "calc(100vw - 2rem)")
        property("border", "none")
        property("box-shadow", "2px 2px 8px rgba(0, 0, 0, .25)")
        property("scrollbar-width", "none")

        self + selector("::backdrop") style {
            backgroundColor(colors.primary)
            opacity(.5)
        }

        dark(self) {
            backgroundColor(colors.dark.background)
            color(Color.white)
        }

        child(self, selector("header")) style {
            fontSize(24.px)
            marginBottom(1.r)
        }

        child(self, selector("section")) style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            marginBottom(1.r)
        }

        child(self, selector("footer")) style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.RowReverse)

            child(self, selector("button")) style {
                marginLeft(1.r)
            }
        }
    }

    val switchSlider by style {
        position(Position.Absolute)
        cursor("pointer")
        property("inset", "0")
        borderRadius(2.r)
        backgroundColor(Color("#e4e4e4"))
        property("transition", ".5s")

        dark(self) {
            backgroundColor(colors.dark.background)
        }

        self + before style {
            position(Position.Absolute)
            property("content", "\"\"")
            height(1.5.r)
            width(1.5.r)
            left(.25.r)
            bottom(.25.r)
            borderRadius(1.5.r)
            backgroundColor(Color.white)
            property("transition", ".5s")
        }
    }

    @OptIn(ExperimentalComposeWebApi::class)
    val switch by style {
        position(Position.Relative)
        display(DisplayStyle.InlineBlock)
        width(3.5.r)
        height(2.r)
        minWidth(3.5.r)
        minHeight(2.r)
        borderRadius(2.r)

        child(self, selector("input")) style {
            opacity(0)
            width(0.r)
            height(0.r)
            display(DisplayStyle.None)
        }

        sibling(child(self, selector("input") + checked), className(switchSlider)) style {
            backgroundColor(colors.primary)
        }

        sibling(child(self, selector("input") + checked), (className(switchSlider) + before)) style {
            transform {
                translateX(1.5.r)
            }
        }
    }

    val dateTimeInput by style {
        borderRadius(1.r)
        property("color", "inherit")
        property("font", "inherit")
        border(1.px, LineStyle.Solid, Color("#444444"))

        dark(self) {
            backgroundColor(colors.dark.background)
        }

        child(self, selector("option")) style {
            borderRadius(.5.r)
            margin(.5.r)
            padding(.5.r, 1.r)
        }
    }

    val menuButton by style {
        cursor("pointer")

        self + hover style {
            textDecoration("underline")
        }
    }

    val appHeader by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        padding(1.r)
        margin(1.r)
        elevated()
    }

    val appFooter by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        padding(1.r)
        marginTop(1.r)
        backgroundColor(Color("#f7f7f7"))

        dark(self) {
            backgroundColor(colors.dark.background)
        }

        media(mediaMaxWidth(640.px)) {
            self style {
                flexDirection(FlexDirection.Column)
            }
        }
    }

    val mainHeader by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        padding(4.r, 2.r)
        backgroundColor(Color("#2f0729"))
        backgroundImage("url(/saigonnight-mobile.jpg)")
        backgroundPosition("center")
        textAlign("center")
        backgroundSize("cover")
        margin(1.r, 0.r)
        fontSize(32.px)
        color(Color.white)
        borderRadius(2.r)
        whiteSpace("pre-wrap")
        fontFamily("Estonia")
        lineHeight("1.25")
        property("aspect-ratio", "6/1")
        property("text-shadow", "#fff 0px 0px .5rem")

        media(mediaMinWidth(641.px)) {
            self style {
                fontSize(42.px)
                padding(4.r)
                textAlign("end")
                justifyContent(JustifyContent.FlexEnd)
                backgroundImage("url(/saigonnight.jpg)")
            }
        }
    }

    val mainContent by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        minHeight(100.vh)
        alignItems(AlignItems.Stretch)
        justifyContent(JustifyContent.Stretch)
    }

    val navContainer by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Stretch)
//        padding(1.r)
        overflowX("hidden")
        property("box-shadow", "2px 2px 16px rgba(0, 0, 0, 0.125)")
        backgroundColor(Color.white)
        borderRadius(1.r)
        marginLeft(1.r)
        marginRight(1.r)
        property("max-width", "calc(100vw - ${2.r})")

        dark(self) {
            backgroundColor(colors.dark.background)
        }
    }

    val navContent by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Stretch)
        boxSizing("border-box")
    }

    val cardContent by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.FlexStart)
        padding(1.r)

        child(self, not(lastChild)) style {
            marginBottom(1.r)
        }

        media(mediaMinWidth(641.px)) {
            self style {
                padding(1.r * 1.5f)
            }
        }
    }

    val content by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Row)
        flexWrap(FlexWrap.Wrap)
        flexGrow(1)
        padding(1.r)
        overflow("auto")
        padding(1.r / 2)
        justifyContent(JustifyContent.Center)
        alignContent(AlignContent.FlexStart)

        media(mediaMinWidth(641.px)) {
            self style {
                padding(1.r)
            }
        }
    }

    @OptIn(ExperimentalComposeWebApi::class)
    val card by style {
        cardStyle()
        position(Position.Relative)
        display(DisplayStyle.Flex)
        width(640.px)
        overflow("hidden")
        flexDirection(FlexDirection.ColumnReverse)
        cursor("pointer")
        property("aspect-ratio", ".75")
        property("will-change", "transform")
        property("transform-style", "preserve-3d")

        transitions {
            "transform" {
                duration = 500.ms
            }
        }

        transform {
            perspective(100.vw)
        }

//        self + hover style {
//            self style {
//                transform {
//                    perspective(100.vw)
//                    rotate3d(1f, 0, .5f, 6.deg)
//                    translate3d(0.r, -1.r, 0.r)
//                }
//            }
//        }

        media(mediaMaxWidth(640.px)) {
            self style {
                width(100.percent)
                margin(1.r / 2)
            }
        }
    }

    val category by style {
        borderRadius(.5.r)
        border(1.px, LineStyle.Solid, colors.primary)
        color(colors.primary)
        property("width", "fit-content")
        marginTop(.5.r)
        padding(.5.r, 1.r)
    }

    val cardPost by style {
        backgroundColor(rgba(255, 255, 255, .92))
        padding(1.r)
        margin(1.r)
        color(Color.black)
        borderRadius(1.r)
        maxHeight(50.percent)
        boxSizing("border-box")
        overflowY("auto")
        fontSize(18.px)

        dark(self) {
            backgroundColor(rgba(0, 0, 0, .92))
            color(Color.white)
        }
    }

    val cardButton by style {
        backgroundColor(rgba(255, 255, 255, .92))
        borderRadius(2.r)
        padding(1.r / 2, 1.r)
        color(Color.black)
        property("z-index", "1")

        dark(self) {
            backgroundColor(rgba(0, 0, 0, .92))
            color(Color.white)
        }
    }

    val button by style {
        borderRadius(2.r)
        border(0.px)
        padding(0.r, 2.r)
        height(3.r)
        backgroundColor(colors.primary)
        color(Color.white)
        cursor("pointer")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        fontWeight("bold")

        selector(".material-symbols-outlined") style {
            marginRight(.5.r)
        }

        self + disabled style {
            opacity(.5)
        }
    }

    val outlineButton by style {
        borderRadius(2.r)
        border(1.px, LineStyle.Solid, colors.primary)
        padding(0.r, 2.r)
        height(3.r)
        backgroundColor(Color.transparent)
        color(colors.primary)
        cursor("pointer")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        fontWeight("bold")

        selector(".material-symbols-outlined") style {
            marginRight(.5.r)
        }

        self + disabled style {
            opacity(.5)
        }
    }

    val textButton by style {
        property("border", "none")
        borderRadius(2.r)
        height(3.r)
        backgroundColor(Color.transparent)
        color(colors.primary)
        cursor("pointer")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        fontWeight("bold")
        property("font-size", "inherit")
        selector(".material-symbols-outlined") style {
            marginRight(.5.r)
        }

        self + disabled style {
            opacity(.5)
        }
    }

    val mainContentCards by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        flexWrap(FlexWrap.Wrap)
        position(Position.Relative)

        child(self, className(card)) style {
            self style {
                width(320.px)
                marginTop(0.r)
                marginLeft(0.r)
            }
        }
    }

    val textarea by style {
        borderRadius(1.r)
        border(1.px, LineStyle.Solid, colors.background)
        property("resize", "none")
        padding(1.r)
        property("font-size", "inherit")
        fontFamily("inherit")
        boxSizing("border-box")
        backgroundColor(Color.white)

        dark(self) {
            backgroundColor(colors.dark.background)
            color(Color.white)
            border(1.px, LineStyle.Solid, Color("#444444"))

            self + selector("::placeholder") style {
                dark(self) {
                    color(Color.white)
                    opacity(.5)
                }
            }
        }
    }

    val profilePhotoText by style {
        borderRadius(100.percent)
        backgroundColor(colors.background)
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        flexShrink(0)

        dark(self) {
            backgroundColor(Color.black)
        }
    }

    val profilePhotoPhoto by style {
        borderRadius(100.percent)
        backgroundColor(colors.background)
        backgroundPosition("center")
        backgroundSize("cover")
        flexShrink(0)

        dark(self) {
            backgroundColor(Color.black)
        }
    }

    val profilePhotoBorder by style {
        border(3.px, LineStyle.Solid, Color.white)

        dark(self) {
            border(3.px, LineStyle.Solid, Color.black)
        }
    }
}

fun CSSBuilder.ellipsize() {
    overflow("hidden")
    property("text-overflow", "ellipsis")
    whiteSpace("nowrap")
}

fun StyleScope.ellipsize() {
    overflow("hidden")
    property("text-overflow", "ellipsis")
    whiteSpace("nowrap")
}

import app.dark
import app.desktop
import app.mobile
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.AlignContent
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.AlignSelf
import org.jetbrains.compose.web.css.AnimationTimingFunction
import org.jetbrains.compose.web.css.CSSBuilder
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.Position.Companion.Absolute
import org.jetbrains.compose.web.css.Position.Companion.Relative
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.alignContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.alignSelf
import org.jetbrains.compose.web.css.backgroundAttachment
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.backgroundPosition
import org.jetbrains.compose.web.css.backgroundSize
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.boxSizing
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.div
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.fontFamily
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.lineHeight
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.media
import org.jetbrains.compose.web.css.mediaMaxWidth
import org.jetbrains.compose.web.css.mediaMinWidth
import org.jetbrains.compose.web.css.minHeight
import org.jetbrains.compose.web.css.minWidth
import org.jetbrains.compose.web.css.ms
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.outline
import org.jetbrains.compose.web.css.outlineColor
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.paddingRight
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.rgba
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.textDecoration
import org.jetbrains.compose.web.css.times
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.transform
import org.jetbrains.compose.web.css.transitions
import org.jetbrains.compose.web.css.unaryMinus
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.vw
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.css.width
import org.w3c.dom.HTMLDivElement

@OptIn(ExperimentalComposeWebApi::class)
object Styles : StyleSheet() {
    object colors {
        val background = Color("#E0F3FF")
        val primary = Color("#006689")
        val secondary = Color("#767676")
        val tertiary = Color("#2e8900")
        val red = Color("#761c1c")
        val green = Color("#1c7626")
        val outline = Color("#fff6")

        object dark {
            val background = Color("#18191a")
            val surface = Color("#232526")
            val outline = Color("#0006")
        }
    }

    val fullscreenContainer by style {
        backgroundColor(Color.white)

        dark(self) {
            backgroundColor(Color.black)
        }
    }

    val scriptCoverContainer by style {
        backgroundColor(rgba(255, 255, 255, .92))
        borderRadius(2.r)

        dark(self) {
            backgroundColor(rgba(0, 0, 0, .92))
        }
    }

    val featureButton by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        padding(1.r)
        elevated(hoverElevation = 2.0)
        cursor("pointer")

        hover(self) style {
            property("scale", 1.05)
        }

        transitions {
            "transform" {
                duration = 200.ms
            }
            "scale" {
                duration = 200.ms
            }
        }
    }

    val video by style {
        width(100.percent)
        property("z-index", "0")
        backgroundColor(Styles.colors.background)
        property("object-fit", "cover")
        borderRadius(1.r)
        overflow("hidden")
        transform {
            translateZ(1.px)
        }

        dark(self) {
            backgroundColor(colors.dark.background)
        }
    }

    val markdown by style {
        whiteSpace("initial")
        property("word-break", "break-word")

        desc(self, selector(":first-child")) style {
            property("margin-top", "0")
        }
        desc(self, selector(":last-child")) style {
            property("margin-bottom", "0")
        }
        
        desc(self, selector("blockquote")) style {
            property("border-left", "${2.px} ${LineStyle.Solid} ${colors.primary}")
            paddingLeft(.5.r)
            marginLeft(0.px)
        }

        desc(self, selector("li")) style {
            marginBottom(1.r)
        }
    }

    val reactionsLayout by style {
        display(DisplayStyle.Flex)
        flexWrap(FlexWrap.Wrap)
        gap(.5f.r)
    }

    val calendarLineNow by style { }

    val calendarLineDrop by style { }

    val calendarLine by style {
        position(Absolute)
        left(0.r)
        right(0.r)
        height(1.px)
        backgroundColor(rgba(0, 0, 0, .125))

        self + className(calendarLineNow) style {
            backgroundColor(colors.red)
        }

        self + className(calendarLineDrop) style {
            backgroundColor(colors.primary)
        }

        dark(self) {
            backgroundColor(rgba(255, 255, 255, .125))
        }
    }

    val calendarColumnTitleHidden by style { }

    @OptIn(ExperimentalComposeWebApi::class)
    val calendarColumnTitle by style {
        fontSize(12.px)
        opacity(.8f)
        textAlign("center")
        padding(.25f.r, .5f.r)
        alignSelf(AlignSelf.Center)
        elevated()

        self + className(calendarColumnTitleHidden) style {
            opacity(0)
        }

        transitions {
            "opacity" {
                duration = 200.ms
            }
        }
    }

    val calendarEvent by style {
        boxSizing("border-box")
        padding(0.r, .125.r)
        fontSize(12.px)
        overflow("hidden")
        elevated()
        borderRadius(.25.r)
        cursor("pointer")
    }

    val formContent by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Stretch)
        width(100.percent)
        boxSizing("border-box")
        maxWidth(800.px)
        elevated()
        padding(0.r, 1.r, 1.r, 1.r)

        dark(self) {
            border(1.px, LineStyle.Solid, Color.black)
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

    fun CSSBuilder.elevated(elevation: Double = 1.0, hoverElevation: Double? = null) {
        property("box-shadow", "${elevation}px ${elevation}px ${elevation * 4.0}px rgba(0, 0, 0, 0.125)")
        backgroundColor(Color.white)
        borderRadius(1.r)

        hoverElevation?.let { hoverElevation ->
            property("box-shadow", "${hoverElevation}px ${hoverElevation}px ${hoverElevation * 4.0}px rgba(0, 0, 0, 0.125)")
        }

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

    val desktopOnly by style {
        display(DisplayStyle.Flex)

        mobile(self) {
            display(DisplayStyle.None)
        }
    }

    val mobileOnly by style {
        display(DisplayStyle.Flex)

        desktop(self) {
            display(DisplayStyle.None)
        }
    }

    val mainContainer by style {
        position(Position.Relative)
        width(100.vw)
        height(100.vh)
        overflow("hidden")
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)

        dark(self) {
            backgroundColor(Color.black)
            color(Color.white)
        }
    }

    val background by style {
        position(Position.Relative)
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
    }

    val backgroundPhotoLoaded by style {}

    @OptIn(ExperimentalComposeWebApi::class)
    val backgroundPhoto by style {
        position(Position.Absolute)
        backgroundPosition("center")
        backgroundSize("cover")
        backgroundAttachment("fixed")
        property("inset", "0")
        opacity(0)

        self + className(backgroundPhotoLoaded) style {
            opacity(1)
        }

        transitions {
            "opacity" {
                duration = 200.ms
            }
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

    val switchBordered by style {
        border(1.px, LineStyle.Solid, Color.white)

        dark(self) {
            border(1.px, LineStyle.Solid, Color.black)
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
        property("z-index", "1")
    }

    val appFooter by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        padding(1.r)
        backgroundColor(Color("#f7f7f7"))
        property("z-index", "1")
        whiteSpace("nowrap")
        overflowX("auto")
        width(100.vw)

        dark(self) {
            backgroundColor(colors.dark.background)
        }

        mobile(self) {
            justifyContent(JustifyContent.Start)
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
        property("z-index", "1")

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
        property("z-index", "1")
    }

    val mainContentKiosk by style {
        width(40.vw)
        property("margin-left", "auto")
    }

    @OptIn(ExperimentalComposeWebApi::class)
    val navContainer by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Stretch)
        overflowX("hidden")
        overflowY("auto")
        property("box-shadow", "2px 2px 16px rgba(0, 0, 0, 0.125)")
        backgroundColor(Color.white)
        borderRadius(1.r)
        marginLeft(1.r)
        marginRight(1.r)
        transform {
            translateZ(1.px)
        }
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

    val profileCard by style {
        elevated()
        display(DisplayStyle.Flex)
        overflow("hidden")
        flexDirection(FlexDirection.Column)
        cursor("pointer")
        property("aspect-ratio", ".75")
        alignItems(AlignItems.Center)

        media(mediaMaxWidth(640.px)) {
            self style {
                width(100.percent)
                margin(1.r / 2)
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
        minHeight(3.r)
        backgroundColor(colors.primary)
        color(Color.white)
        cursor("pointer")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        fontWeight("bold")
        overflow("hidden")
        backgroundImage("linear-gradient(to bottom, #ffffff36, #ffffff00)")
        property("box-shadow", "0 0 4px 2px #ffffff36 inset")

        selector(".material-symbols-outlined") style {
            marginRight(.5.r)
        }

        self + disabled style {
            opacity(.5)
        }
    }

    val outlineButtonAlt by style {}
    val outlineButtonSmall by style {}
    val outlineButtonTonal by style {}

    val outlineButton by style {
        borderRadius(2.r)
        border(1.px, LineStyle.Solid, colors.primary)
        padding(0.r, 2.r)
        minHeight(3.r)
        backgroundColor(Color.transparent)
        color(colors.primary)
        cursor("pointer")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        fontWeight("bold")

        selector(".material-symbols-outlined") style {
            marginRight(.5.r)
        }

        self + className(outlineButtonAlt) style {
            color(Color.black)
        }

        self + className(outlineButtonTonal) style {
            backgroundColor(colors.background)
        }

        self + className(outlineButtonSmall) style {
            padding(0.r, 1.r)
            minHeight(2.r)
        }

        self + disabled style {
            opacity(.5)
        }

        dark(self) {
            self + className(outlineButtonAlt) style {
                color(Color.white)
            }

            self + className(outlineButtonTonal) style {
                backgroundColor(colors.dark.background)
            }
        }
    }

    val floatingButtonSelected by style {}

    val floatingButton by style {
        borderRadius(2.r)
        property("border", "none")
        padding(0.r, 2.r)
        minHeight(3.r)
        backgroundColor(colors.background)
        color(colors.primary)
        cursor("pointer")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        fontWeight("bold")
        property("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.125)")

        selector(".material-symbols-outlined") style {
            marginRight(.5.r)
        }

        self + disabled style {
            opacity(.5)
        }

        self + className(floatingButtonSelected) style {
            backgroundColor(colors.primary)
            color(Color.white)
        }

        dark(self) {
            backgroundColor(colors.dark.background)
            color(Color.white)
        }
    }

    val textButton by style {
        property("border", "none")
        borderRadius(2.r)
        minHeight(3.r)
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

    val inlineButton by style {
        color(colors.primary)
        cursor("pointer")
        fontWeight("bold")

        self + disabled style {
            opacity(.5)
        }
    }

    val buttonSelected by style {
        outline(colors.secondary, LineStyle.Solid.toString(), 2.px)

        dark(self) {
            outlineColor(Color.white)
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

    val textareaInline by style { }

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

        self + className(textareaInline) style {
            property("border", "none")
            padding(.5.r, 0.r)
            borderRadius(.5.r)
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

    val mapContainer by style {
        height(0.r)
        flexGrow(1)
        display(DisplayStyle.Flex)
        alignItems(AlignItems.FlexStart)

        mobile(self) {
            flexDirection(FlexDirection.Column)
        }
    }

    val mapUi by style {
        flexGrow(1)
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Center)

        mobile(self) {
            width(100.percent)
        }

        desktop(self) {
            maxWidth(50.percent)
        }
    }

    val mapListAutoHide by style {  }

    val mapList by style {
        marginBottom(1.r)

        // Show above map controls
        property("z-index", "10")

        self + className(mapListAutoHide) style {
            transform {
                translateX(-85.percent)
            }

            transitions {
                "transform" {
                    duration = 350.ms
                    timingFunction = AnimationTimingFunction.EaseInOut
                }
            }

            self + hover style {
                transform {
                    translateX(0.percent)
                }
            }
        }

        desktop(self) {
            width(24.r)
            property("max-height", "calc(${100.percent} - ${1.r})")
        }

        mobile(self) {
            display(DisplayStyle.None)
        }
    }

    val mapPanelContainer by style {
    }

    val mapPanel by style {
        position(Relative)
        marginBottom(1.r)

        // Show above map controls
        property("z-index", "10")

        desktop(self) {
            marginLeft(0.r)
            width(36.r)
            property("max-height", "calc(${100.percent} - ${1.r})")
        }

        mobile(self) {
            property("width", "calc(${100.percent} - ${2.r})")
            property("max-height", "calc(${50.percent} - ${1.r})")
        }
    }

    val mapMarker by style {
    }

    val mapMarkerContent by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Center)
        gap(2.r)
        cursor("pointer")
        property("pointer-events", "auto")
        property("will-change", "transform")
        property("transform-origin", "bottom center")

        transitions {
            "transform" {
                duration = 200.ms
            }
        }
    }

    val personList by style {
        property("width", "calc(${100.percent} - ${1.r})")
        overflowX("auto")
        property("scrollbar-width", "none")
        paddingLeft(.5.r)
        paddingRight(1.r)
        gap(1.r)
    }

    val personItem by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Center)
        gap(.5.r)
        textAlign("center")
        cursor("pointer")
    }

    val personItemStatus by style {
        display(DisplayStyle.Flex)
        gap(.25.r)
        alignItems(AlignItems.Center)
        position(Absolute)
        top(0.r)
        borderRadius(1.r)
        padding(.25.r, .5.r)
        maxWidth(100.percent)
        left(50.percent)
        property("transform", "translateX(-50%)")
        backgroundColor(Color.white)
        ellipsize()
        fontSize(10.px)
        boxSizing("border-box")
        property("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.125)")

        dark(self) {
            backgroundColor(Color.black)
        }
    }

    val personItemStatusIndicatorText by style {
        position(Absolute)
        top(-11.px)
        right(-1.px)
        color(Color.white)
        property("text-shadow", "1px 0px 0px black, -1px 0px 0px black, 0px 1px 0px black, 0px -1px 0px black")

        dark(self) {
            color(Color.black)
            property("text-shadow", "1px 0px 0px white, -1px 0px 0px white, 0px 1px 0px white, 0px -1px 0px white")
        }
    }
    val personItemStatusIndicator by style {
        width(12.px)
        height(12.px)
        borderRadius(6.px)
        property("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.125), rgba(255, 255, 255, 0.5) 0px 2px 4px inset")
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

fun AttrsScope<HTMLDivElement>.mainContent(layout: AppLayout = AppLayout.Default) {
    if (layout == AppLayout.Kiosk) {
        classes(Styles.mainContent, Styles.mainContentKiosk)
    } else {
        classes(Styles.mainContent)
    }
}

package app

import Styles
import Styles.elevated
import Styles.textIcon
import ellipsize
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.*
import r


object AppStyles : StyleSheet() {

    val seenUntilLayout by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.RowReverse)
        gap(.5.r)
        padding(.5.r, 1.r, 0.r, 1.r)
        overflowX("hidden")
        flexWrap(FlexWrap.Wrap)
    }

    val notificationsLayout by style {
        position(Position.Fixed)
        left(0.r)
        top(0.r)
        right(0.r)
        maxHeight(50.vh)
        overflowY("auto")
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Center)
        maxWidth(34.r)
        padding(1.r)
        property("margin", "auto")
        property("z-index", "101")
    }

    val notification by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        padding(1.r)
        marginBottom(1.r)
        backgroundColor(Color.white)
        borderRadius(1.r)
        cursor("pointer")
        property("box-shadow", "2px 2px 16px rgba(0, 0, 0, 0.125)")

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
        }
    }

    val notificationBody by style {
        flex(1)
        padding(0.r, .5.r)
    }

    val notificationActions by style {
        flexShrink(0)
    }

    val notificationIcon by style {
        flexShrink(0)
        color(Styles.colors.tertiary)
    }

    val notificationTitle by style {
        fontWeight("bold")
        property("word-break", "break-word")
    }

    val notificationMessage by style {
        opacity(.5)
        property("word-break", "break-word")
    }

    val groupAppsBar by style {
        margin(0.r, 1.r, 1.r, 1.r)
    }

    val groupDescription by style {
        borderRadius(1.r)
        margin(0.r, 1.r, .5.r, 1.r)
        padding(.5.r, 1.r)
        border(1.px, LineStyle.Solid, Styles.colors.secondary)
        backgroundColor(Styles.colors.background)
        cursor("pointer")
        overflow("hidden auto")
        whiteSpace("pre-wrap")
        maxHeight(6.r)
        flexShrink(0)

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
        }
    }

    val groupMessageReply by style {
        display(DisplayStyle.Flex)
        borderRadius(1.r)
        margin(1.r, 1.r, 0.r, 1.r)
        padding(.5.r)
        border(1.px, LineStyle.Solid, Styles.colors.secondary)
        backgroundColor(Styles.colors.background)
        cursor("pointer")
        overflow("hidden auto")
        whiteSpace("pre-wrap")
        maxHeight(6.r)
        flexShrink(0)
        flexDirection(FlexDirection.RowReverse)
        alignItems(AlignItems.Center)
        gap(.5.r)

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
        }
    }

    val menu by style {
        padding(.5.r)
        backgroundColor(Color.white)
        borderRadius(1.r)
        property("box-shadow", "2px 2px 16px rgba(0, 0, 0, 0.125)")
        position(Position.Fixed)
        property("z-index", "102")
        property("transform", "translateX(-100%)")
        property("user-select", "none")
        boxSizing("border-box")
        property("font-size", "initial")

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
        }
    }

    val menuInline by style {
        backgroundColor(Color.white)
        borderRadius(1.r)
        property("user-select", "none")

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
        }
    }

    val menuItemSelected by style {
    }

    val menuItem by style {
        borderRadius(.5.r)
        padding(1.r)
        cursor("pointer")
        whiteSpace("nowrap")
        display(DisplayStyle.Flex)

        child(self, selector("span") + firstChild) style {
            flex(1)
        }

        self + hover style {
            backgroundColor(Styles.colors.background)
        }

        dark(self) {
            self + hover style {
                backgroundColor(Color.black)
            }
        }

        self + className(menuItemSelected) style {
            backgroundColor(Styles.colors.primary)
            color(Color.white)
        }
    }

    val baseLayout by style {
        position(Position.Relative)
        width(100.vw)
        height(100.vh)
        overflow("hidden")
        display(DisplayStyle.Flex)

        dark(self) {
            backgroundColor(Color.black)
            color(Color.white)
        }

        mobile(self) {
            flexDirection(FlexDirection.ColumnReverse)
        }
    }

    val sideLayout by style {
        overflow("hidden")
        flexShrink(0)
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.ColumnReverse)
        marginTop(1.r)
        marginLeft(1.r)
        marginBottom(1.r)
        property("z-index", "1")
        elevated()

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
        }

        mobile(self) {
            property("width", "calc(${100.percent} - ${2.r})")
            height(33.vh)
        }

        desktop(self) {
            width(24.r)
        }
    }
    val mainLayout by style {
        overflow("hidden")
        flexGrow(1)
        property("z-index", "1")
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.ColumnReverse)
    }

    @OptIn(ExperimentalComposeWebApi::class)
    val bottomBar by style {
        display(DisplayStyle.Flex)

        child(self, selector("span")) style {
            flex(1)
            textAlign("center")
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
            justifyContent(JustifyContent.Center)
            padding(.5.r)
            margin(.5.r)
            borderRadius(4.r)

            transitions {
                "color" {
                    duration = 100.ms
                }
                "background-color" {
                    duration = 100.ms
                }
            }
        }
    }

    val messages by style {
        flex(1)
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.ColumnReverse)
        overflowY("auto")
        overflowX("hidden")
    }

    val groupCards by style {
        flex(1)
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        overflowY("auto")
        overflowX("hidden")
    }

    val messageBar by style {
        flexShrink(0)
        display(DisplayStyle.Flex)
        margin(1.r)
        flexDirection(FlexDirection.RowReverse)
        alignItems(AlignItems.Center)
    }

    val groupItemOnSurface by style {
    }

    val groupItemOnBackground by style {

    }

    val groupItemDefault by style {

    }

    val groupItemCard by style {
        width(100.percent)
    }

    val groupItemCardShadow by style {
        property("box-shadow", "0 0 16px rgba(0, 0, 0, .125)")
        borderRadius(1.r)
    }

    val groupList by style {
        display(DisplayStyle.Flex)
        flexWrap(FlexWrap.Wrap)
        width(100.percent)

        desktop(self) {
            child(self, className(groupItemCard)) style {
                marginRight(1.r)
                marginBottom(1.r)
            }
        }
    }

    val groupItem by style {
        padding(.5.r, 1.r)
        borderRadius(1.r)
        cursor("pointer")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        property("word-break", "break-word")

        self + hover style {
            backgroundColor(Styles.colors.background)
        }

        self + className(groupItemDefault) style {
            backgroundColor(Styles.colors.background)

            self + hover style {
                backgroundColor(Styles.colors.background)
            }
        }

        self + className(groupItemOnSurface) style {
            self + hover style {
                backgroundColor(Color("rgb(255 255 255 / 50%)"))
            }
        }

        dark(self) {
            self + hover style {
                backgroundColor(Color.black)
            }

            self + className(groupItemOnSurface) style {
                self + hover style {
                    backgroundColor(Color.black)
                }
            }

            self + className(groupItemOnBackground) style {
                backgroundColor(Styles.colors.dark.background)
            }

            self + className(groupItemDefault) style {
                backgroundColor(Styles.colors.dark.background)

                self + hover style {
                    backgroundColor(Styles.colors.dark.background)
                }
            }
        }

        @OptIn(ExperimentalComposeWebApi::class)
        transitions {
            "color" {
                duration = 100.ms
            }
            "background-color" {
                duration = 100.ms
            }
            "border-radius" {
                duration = 100.ms
            }
        }
    }

    val groupItemSelected by style {
        backgroundColor(Styles.colors.background)

        self + className(groupItemOnSurface) style {
            backgroundColor(Color("rgb(255 255 255 / 50%)"))
        }

        dark(self) {
            backgroundColor(Color.black)

            self + className(groupItemOnSurface) style {
                backgroundColor(Color.black)
            }
        }
    }

    val groupItemSelectedPrimary by style {
        backgroundColor(Styles.colors.primary)
        color(Color.white)

        self + className(groupItem) + hover style {
            backgroundColor(Styles.colors.primary)
        }

        dark(self) {
            self + className(groupItem) + hover style {
                backgroundColor(Styles.colors.primary)
            }
        }
    }

    val groupItemName by style {

    }

    val groupItemMessage by style {
        color(Styles.colors.secondary)
        ellipsize()
    }

    val navMenuItem by style {
        self + className(groupItem) style {
            padding(1.r)
        }

        child(self, className("material-symbols-outlined")) style {
            marginRight(1.r)
        }

        child(self, className(textIcon)) style {
            marginRight(1.r)
        }
    }

    val myMessageLayout by style {

    }

    val myMessage by style {

    }

    val messageLayout by style {
        position(Position.Relative)
        display(DisplayStyle.Flex)
        margin(.5.r, 6.r, 0.r, 1.r)

        self + className(myMessageLayout) style {
            margin(1.r, 1.r, 0.r, 6.r)
            justifyContent(JustifyContent.FlexEnd)
        }
    }

    val messageItem by style {
        padding(1.r)
        backgroundColor(Color.white)
        borderRadius(1.r)
        border(1.px, LineStyle.Solid, Styles.colors.background)
        whiteSpace("pre-wrap")
        property("word-break", "break-word")

        self + className(myMessage) style {
            backgroundColor(Styles.colors.background)
            outline("${1.px} ${LineStyle.Solid} ${Color.white}")
            property("border", "none")
        }

        dark(self) {
            self + className(myMessage) style {
                backgroundColor(Styles.colors.dark.background)
                outline("${1.px} ${LineStyle.Solid} ${Color.black}")
            }
        }

        dark(self) {
            backgroundColor(Color.black)
            border(1.px, LineStyle.Solid, Color("#444444"))
        }
    }

    val myMessageBots by style {

    }

    val messageBots by style {
        position(Position.Absolute)
        top(-12.px)
        property("left", "2.5rem")
        property("z-index", "1")

        self + className(myMessageBots) style {
            right(-12.px)
            property("left", "unset")
        }
    }

    val myMessageReply by style {

    }

    val messageReply by style {
        display(DisplayStyle.Flex)
        padding(1.r)
        position(Position.Relative)
        marginBottom(1.r / 2)
        borderRadius(
            1.r / 2,
            1.r,
            1.r,
            1.r / 2,
        )
        backgroundColor(Color("#fafafa"))
        property("border-left", "4px solid ${Styles.colors.background}")

        self + className(myMessageReply) style {
            flexDirection(FlexDirection.RowReverse)
            borderRadius(
                1.r,
                1.r / 2,
                1.r / 2,
                1.r,
            )
            property("border-left", "none")
            property("border-right", "4px solid ${Styles.colors.background}")
        }

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
            property("border-left", "4px solid ${Color("#444")}")

            self + className(myMessageReply) style {
                property("border-right", "4px solid ${Color("#444")}")
            }
        }
    }

    @OptIn(ExperimentalComposeWebApi::class)
    val messageVideo by style {
        property("object-fit", "contain")
        width(100.percent)
        maxHeight(66.vh) // Approx max in viewable area
        borderRadius(1.r)
        backgroundColor(Styles.colors.background)
        cursor("pointer")
        transform {
            translateZ(1.px)
        }

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
        }
    }

    val iconButton by style {
        padding(.5.r)
        borderRadius(2.r)
        cursor("pointer")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        position(Position.Relative)
        property("user-select", "none")
    }

    val iconButtonBackground by style {
        backgroundColor(Styles.colors.background)

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
        }
    }

    val iconButtonCount by style {
        position(Position.Absolute)
        fontSize(12.px)
        width(18.px)
        height(18.px)
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        borderRadius(100.percent)
        backgroundColor(Styles.colors.background)
        fontWeight("bold")
        color(Styles.colors.primary)
        property("transform", "translate(9px, -9px)")

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
        }
    }

    val tray by style {
        display(DisplayStyle.Flex)
        height(18.r)
        maxHeight(50.vh)
        overflowX("hidden")
        overflowY("auto")
        flexDirection(FlexDirection.Column)
        backgroundColor(Color("#fafafa"))
        border(1.px, LineStyle.Solid, Color("#e4e4e4"))
        borderRadius(1.r)

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
            border(1.px, LineStyle.Solid, Color("#444444"))
        }
    }

    val messageItemPhoto by style {
        backgroundColor(Styles.colors.background)
        height(320.px)
        maxHeight(100.vw)
        maxWidth(100.percent)
        borderRadius(1.r)
        border(3.px, LineStyle.Solid, Color.white)
        cursor("pointer")
        property("object-fit", "contain")

        dark(self) {
            backgroundColor(Color.black)
            border(3.px, LineStyle.Solid, Color.black)
        }
    }

    val stickerMessage by style {
        borderRadius(4.r)
        backgroundColor(Color.white)
        padding(1.r / 2, 1.r)
        whiteSpace("nowrap")
        property("z-index", "1")

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
            color(Color.white)
        }
    }

    val messageItemStory by style {
        borderRadius(1.r)
        backgroundColor(Color.white)
        property("box-shadow", "1px 1px 4px rgba(0, 0, 0, 0.125)")
        padding(1.r)
        cursor("pointer")
        overflow("hidden")
        maxWidth(36.r)

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
        }
    }

    val urlPreview by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Stretch)
        overflow("hidden")
        borderRadius(1.r)
        backgroundColor(Styles.colors.background)
        cursor("pointer")
        marginBottom(1.r)
        maxWidth(480.px)

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
        }
    }
}

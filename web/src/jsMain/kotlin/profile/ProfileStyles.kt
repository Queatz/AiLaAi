package profile
import Styles
import app.dark
import ellipsize
import org.jetbrains.compose.web.css.*
import r

object ProfileStyles : StyleSheet() {
    val mainContent by style {
        display(DisplayStyle.Flex)
        media(mediaMaxWidth(640.px)) {
            self style {
                flexDirection(FlexDirection.Column)
                alignItems(AlignItems.Center)
            }
        }
    }

    val nophoto by style {
        backgroundColor(Color.rebeccapurple)
    }

    val photo by style {
        width(256.px)
        height(256.px)
        maxWidth(33.333.vw)
        maxHeight(33.333.vw)
        backgroundColor(Styles.colors.background)
        backgroundPosition("center")
        backgroundSize("cover")
        borderRadius(100.percent)
        border(6.px, LineStyle.Solid, Color.white)
        media(mediaMaxWidth(640.px)) {
            self style {
                property("margin-top", "-22.5vw")
                border(4.px, LineStyle.Solid, Color.white)
            }
        }
        media(mediaMinWidth(641.px)) {
            self style {
                property("transform", "translateY(calc(-128px - -1rem))")
                margin(1.r * 1.5f, 0.r, 1.r * 1.5f, 1.r * 1.5f)
                property("margin-bottom", "calc(-128px + 2.5rem)")
            }
        }

        dark(self) {
            border(6.px, LineStyle.Solid, Styles.colors.dark.background)
        }

        self + className(nophoto) style {
            property("transform", "none")

            media(mediaMinWidth(641.px)) {
                self style {
                    margin(1.5.r, 0.r, 1.5.r, 1.5.r)
                }
            }

            media(mediaMaxWidth(640.px)) {
                self style {
                    margin(1.5.r, 0.r, 0.r, 0.r)
                }
            }
        }
    }

    val profileContent by style {
        media(mediaMaxWidth(640.px)) {
            self style {
                alignSelf(AlignSelf.Stretch)
                alignItems(AlignItems.Center)
            }
        }
        media(mediaMinWidth(641.px)) {
            self style {
                flexGrow(1)
                flexShrink(1)
                width(0.px)
            }
        }
    }

    val name by style {
        media(mediaMaxWidth(640.px)) {
            self style {
                width(100.percent)
                textAlign("center")
            }
        }
    }

    val infoCard by style {
        padding(1.r)
        border(1.px, LineStyle.Solid, Styles.colors.background)
        borderRadius(1.r)
        overflow("hidden")
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Stretch)

        child(self, selector("div")) style {
            ellipsize()
            textAlign("center")
        }

        self + not(lastChild) style {
            marginRight(1.r)
        }

        media(mediaMaxWidth(640.px)) {
            self style {
                flex(1)
                width(0.px)
            }
        }

        media(mediaMinWidth(641.px)) {
            self style {
                width(4.r)
            }
        }
    }

    val infoCardName by style {
        color(Styles.colors.secondary)
    }

    val infoCardValue by style {
        fontSize(18.px)
        fontWeight("bold")
    }

    val infoAbout by style {
        whiteSpace("pre-wrap")
        lineHeight("1.25")


        media(mediaMaxWidth(640.px)) {
            self style {
                textAlign("center")
            }
        }
    }
}

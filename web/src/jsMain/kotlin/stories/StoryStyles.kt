package stories
import Styles
import app.AppStyles
import app.dark
import org.jetbrains.compose.web.css.*
import r

object StoryStyles : StyleSheet() {
    val contentTitle by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Stretch)
        justifyContent(JustifyContent.Stretch)
        fontSize(36.px)
        whiteSpace("pre-wrap")
    }

    val contentAuthors by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        flexWrap(FlexWrap.Wrap)
        alignItems(AlignItems.Stretch)
        justifyContent(JustifyContent.Stretch)
        color(Styles.colors.secondary)
        fontSize(16.px)
        whiteSpace("pre-wrap")
    }

    val dialog by style {
        position(Position.Fixed)
        top(0.px)
        left(0.px)
        width(100.vw)
        height(100.vh)
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
    }

    val dialogContent by style {
        backgroundColor(Styles.colors.dark.background)
        borderRadius(8.px)
        padding(16.px)
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Center)

    }

    val buttonRow by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Row)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        gap(8.px)
        overflowX("auto")
        fontSize(90.px)
        marginBottom(0.8.r)
    }

    val reactionButton by style {
        border {
            width = 1.px
            style = LineStyle.Solid
            color = Styles.colors.background
        }
        borderRadius(3.r)
        paddingLeft(1.r)
        paddingRight(1.r)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
    }

    val reactionSpan by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        fontSize(15.px)
        border {
            width = 1.px
            style = LineStyle.Solid
            color = Styles.colors.secondary
        }
        borderRadius(3.r)
        marginRight(0.5.r)

    }

    val contentSection by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Stretch)
        justifyContent(JustifyContent.Stretch)
        fontSize(24.px)
        fontWeight("bold")
    }

    val contentText by style {
        boxSizing("border-box")
        whiteSpace("pre-wrap")
        fontSize(16.px)
        textAlign("justify")
    }

    val contentPhotosMulti by style {  }

    val contentPhotos by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        flexWrap(FlexWrap.Wrap)
        gap(1.r)
        alignItems(AlignItems.Stretch)
        justifyContent(JustifyContent.Stretch)
        lineHeight("0")
        width(100.percent)

        self + className(contentPhotosMulti) style {
        }
    }

    val contentCards by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        flexWrap(FlexWrap.Wrap)
        position(Position.Relative)

        child(self, className(Styles.card)) style {
            self style {
                width(320.px)
                marginRight(1.r)
                marginBottom(1.r)
            }
        }
    }

    val contentGroups by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        position(Position.Relative)
        width(100.percent)
        maxWidth(32.r)
        gap(1.r)

        child(self, className(AppStyles.groupItem)) style {
            self style {
                minWidth(320.px)
            }
        }
    }

    val divider by style {
        display(DisplayStyle.Flex)
        justifyContent(JustifyContent.Center)
        alignItems(AlignItems.Center)
        alignSelf(AlignSelf.Stretch)
        padding(1.r)
    }

    val contentPhotosPhoto by style {
        backgroundColor(Styles.colors.background)
        backgroundPosition("center")
        backgroundSize("cover")
        borderRadius(1.r)
        flex(1)
        minWidth(33.percent)
        maxHeight(100.vh)
        maxWidth(100.percent)
        cursor("pointer")

        media(mediaMaxWidth(640.px)) {
            self style {
                width(100.percent)
            }
        }
    }

    val contentPhotosPhotoNoAspect by style {
        backgroundColor(Styles.colors.background)
        borderRadius(1.r)
        width(100.percent)
        cursor("pointer")
    }

    val contentAudio by style {
        borderRadius(1.r)
    }

    val comments by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        width(100.percent)
        marginTop(1.r)
    }

    val comment by style {
        display(DisplayStyle.Flex)
        width(100.percent)
    }

    val commentLayout by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.FlexStart)
        marginLeft(.5.r)
        marginBottom(.5.r)
        width(100.percent)
    }

    val commentBox by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        padding(.75.r)
        borderRadius(1.r)
        outline("${1.px} ${LineStyle.Solid} ${Color.white}")
        backgroundColor(Styles.colors.background)
        overflow("hidden")

        dark(self) {
            backgroundColor(Styles.colors.dark.background)
            outline("${1.px} ${LineStyle.Solid} ${Color.black}")
        }
    }

    val commentComment by style {
        whiteSpace("pre-wrap")
        property("word-break", "break-word")
    }

    val commentInfo by style {
        marginBottom(.25.r)
        fontSize(14.px)
        opacity(.5f)
    }

    val commentTime by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        fontSize(14.px)
        marginLeft(.75.r)
    }

    val commentRepliesLayout by style {
        alignSelf(AlignSelf.Stretch)
    }
}

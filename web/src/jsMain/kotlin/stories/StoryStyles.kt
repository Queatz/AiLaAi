package stories
import Styles
import app.AppStyles
import app.dark
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Col
import r

object StoryStyles : StyleSheet() {
    val contentTitle by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Stretch)
        justifyContent(JustifyContent.Stretch)
        fontSize(36.px)
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

    val contentPhotos by style {
        boxSizing("border-box")
        display(DisplayStyle.Flex)
        flexWrap(FlexWrap.Wrap)
        alignItems(AlignItems.Stretch)
        justifyContent(JustifyContent.Stretch)
        lineHeight("0")
        maxWidth(100.percent)
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
        backgroundPosition("center")
        backgroundSize("cover")
        borderRadius(1.r)
        height(480.px)
        maxHeight(100.vh)
        maxWidth(100.percent)
        marginRight(1.r)
        marginBottom(1.r)
        cursor("pointer")

        media(mediaMaxWidth(640.px)) {
            self style {
                width(100.percent)
                property("max-width", "calc(100vw - ${1.r * 4})")
            }
        }
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

package app.info

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.cardsCards
import appText
import baseUrl
import com.queatz.db.Card
import components.getConversation
import kotlinx.browser.window
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.paddingRight
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.vw
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.*
import r

@Composable
fun UseCasesPage() {
    var useCases by remember {
        mutableStateOf(emptyList<Card>())
    }

    LaunchedEffect(Unit) {
        api.cardsCards("19515481") {
            useCases = it.shuffled()
        }
    }

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            gap(1.r)
            alignItems(AlignItems.Stretch)
        }
    }) {
        H1 {
            appText { appUseCases }
        }
        useCases.forEachIndexed { index, useCase ->
            Div({
                style {
                    display(DisplayStyle.Flex)
                    cursor("pointer")

                    if (index % 2 == 0) {
                        flexDirection(FlexDirection.RowReverse)
                    }
                }

                onClick {
                    window.open("/page/${useCase.id}", target = "_blank")
                }
            }) {
                Div {
                    Img(src = "$baseUrl/${useCase.photo}", attrs = {
                        style {
                            width(33.vw)
                            property("aspect-ratio", "1.5")
                            borderRadius(2.r)
                        }
                    })
                }
                Div({
                    style {
                        flexGrow(1)
                        paddingLeft(1.r)
                        paddingRight(1.r)
                    }
                }) {
                    Div({
                        style {
                            fontSize(24.px)
                            marginBottom(1.r)
                        }
                    }) {
                        Text(useCase.name!!)
                    }
                    Text(useCase.getConversation().message)
                }
            }
        }
    }
}


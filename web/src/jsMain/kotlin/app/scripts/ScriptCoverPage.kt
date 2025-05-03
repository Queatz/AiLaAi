package app.scripts

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.FullPageLayout
import app.ailaai.api.runScript
import app.ailaai.api.script
import app.components.Empty
import appText
import application
import baseUrl
import com.queatz.db.RunScriptBody
import com.queatz.db.Script
import com.queatz.db.ScriptResult
import components.Loading
import components.QrImg
import mainContent
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.transform
import org.jetbrains.compose.web.css.unaryMinus
import org.jetbrains.compose.web.css.vw
import org.jetbrains.compose.web.dom.Div
import r
import stories.StoryContents
import webBaseUrl

@OptIn(ExperimentalComposeWebApi::class)
@Composable
fun ScriptCoverPage(scriptId: String) {
    Style(AppStyles)

    val layout by application.layout.collectAsState()

    var isLoading by remember {
        mutableStateOf(true)
    }
    var isError by remember {
        mutableStateOf(false)
    }
    var scriptResult by remember {
        mutableStateOf<ScriptResult?>(null)
    }
    var scriptDetails by remember {
        mutableStateOf<Script?>(null)
    }

    application.background(scriptDetails?.background?.let { bg -> "$baseUrl$bg" })

    LaunchedEffect(scriptId) {
        api.script(scriptId) {
            scriptDetails = it
        }
    }

    LaunchedEffect(scriptId) {
        api.runScript(
            id = scriptId,
            data = RunScriptBody()
        ) {
            scriptResult = it
        }
        isLoading = false
    }

    if (layout == AppLayout.Kiosk) {
        QrImg("$webBaseUrl/script/$scriptId") {
            position(Position.Fixed)
            bottom(2.r)
            left(2.r)
            maxWidth(10.vw)
            maxHeight(10.vw)
            transform {
                scale(2)
                translate(25.percent, -25.percent)
            }
        }
    }

    Div({
        mainContent(layout)
    }) {
        FullPageLayout {
            if (isError) {
                Empty {
                    appText { noScripts }
                }
            } else if (isLoading) {
                Loading {
                    style {
                        padding(2.r)
                    }
                }
            } else if (scriptResult != null) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        padding(0.r, 1.r, 1.r, 1.r)
                    }
                }) {
                    scriptResult?.content?.let {
                        Div({
                            classes(Styles.cardContent)

                            if (!scriptDetails?.background.isNullOrBlank()) {
                                classes(Styles.scriptCoverContainer)
                            }
                        }) {
                            StoryContents(
                                content = it,
                                onButtonClick = { script, data, input ->
                                    api.runScript(
                                        id = script,
                                        data = RunScriptBody(
                                            data = data,
                                            input = input
                                        )
                                    ) {
                                        scriptResult = it
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

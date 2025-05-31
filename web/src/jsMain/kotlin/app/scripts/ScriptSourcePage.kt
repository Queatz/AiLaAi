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
import app.FullPageLayout
import app.ailaai.api.script
import app.components.Empty
import appText
import application
import bulletedString
import com.queatz.db.Script
import components.Loading
import mainContent
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.boxSizing
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import lib.jsObject
import app.scripts.ReadOnlyMonacoEditor

@Composable
fun ScriptSourcePage(scriptId: String) {
    var isLoading by remember {
        mutableStateOf(true)
    }
    var isError by remember {
        mutableStateOf(false)
    }
    var scriptDetails by remember {
        mutableStateOf<Script?>(null)
    }

    val layout by application.layout.collectAsState()

    LaunchedEffect(scriptId) {
        api.script(scriptId) {
            scriptDetails = it
            isLoading = false
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
                Loading()
            } else if (scriptDetails != null) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        padding(1.r)
                    }
                }) {
                    // Display script metadata
                    Div({
                        classes(Styles.cardContent)
                    }) {
                        scriptDetails?.name?.let {
                            Div({
                                style {
                                    fontSize(24.px)
                                }
                            }) {
                                Text(
                                    bulletedString(
                                        it,
                                        scriptDetails?.categories?.firstOrNull(),
                                        scriptDetails?.id!!
                                    )
                                )
                            }
                        }

                        scriptDetails?.description?.let {
                            Div({
                                style {
                                    padding(0.r, 0.r, 1.r, 0.r)
                                }
                            }) {
                                Text(it)
                            }
                        }

                        scriptDetails?.author?.name?.let {
                            Div({
                                style {
                                    padding(0.r, 0.r, 1.r, 0.r)
                                }
                            }) {
                                Text("by $it")
                            }
                        }

                        scriptDetails?.source?.let { source ->
                            ReadOnlyMonacoEditor(
                                initialValue = source,
                                styles = {
                                    padding(0.r)
                                    borderRadius(1.r)
                                    width(100.percent)
                                    height(400.px) // Set a fixed height for the editor
                                }
                            )
                        } ?: run {
                            Text("No source code available")
                        }
                    }
                }
            }
        }
    }
}

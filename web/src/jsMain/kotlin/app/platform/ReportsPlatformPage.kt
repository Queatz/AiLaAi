package app.platform

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import api
import app.ailaai.api.reports
import app.ailaai.api.resolveReport
import com.queatz.db.Report
import components.IconButton
import components.LazyColumn
import components.Loading
import format
import kotlinx.browser.window
import kotlinx.datetime.toJSDate
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun ReportsPlatformPage() {
    var reports by remember {
        mutableStateOf(emptyList<Report>())
    }
    var isLoading by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        api.reports {
            reports = it
            isLoading = false
        }
    }

    if (isLoading) {
        Loading()
    } else {
        Div({
            style {
                margin(1.r)
            }
        }) {
            LazyColumn {
                items(reports) { report ->
                    Div({
                        style {
                            fontSize(18.px)
                            marginTop(1.r)
                            marginBottom(1.r)
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                        }
                    }) {
                        Text(report.createdAt!!.toJSDate().format())

                        if (report.reporter != null) {
                            IconButton(
                                "person",
                                // todo: translate
                                "Go to reporter's profile",
                                onClick = {
                                    window.open("/profile/${report.reporter!!}", target = "_blank")
                                }
                            )
                        }

                        IconButton(
                            "check",
                            // todo: translate
                            "Resolve",
                            onClick = {
                                scope.launch {
                                    api.resolveReport(report.id!!) {
                                        refreshTrigger++
                                    }
                                }
                            }
                        )
                    }

                    if (report.urgent == true) {
                        Div(
                            {
                                style {
                                    color(Styles.colors.red)
                                    fontWeight("bold")
                                    marginBottom(1.r)
                                }
                            }
                        ) {
                            // todo: translate
                            Text("URGENT")
                        }
                    }

                    Div({
                        style { marginBottom(1.r) }
                    }) {
                        Div {
                            // todo: translate
                            Span({ style { color(Styles.colors.gray) } }) { Text("Type ") }
                            Text(report.type.toString())
                        }
                        Div {
                            // todo: translate
                            Span({ style { color(Styles.colors.gray) } }) { Text("Entity ") }
                            Text(report.entity.orEmpty())
                        }
                    }

                    Div({
                        style {
                            whiteSpace("pre-wrap")
                            padding(1.r)
                            overflowX("auto")
                            borderRadius(1.r)
                            border(1.px, LineStyle.Solid, Styles.colors.gray)
                        }
                    }) {
                        Text(report.reporterMessage.orEmpty())
                    }
                }
            }
        }
    }
}

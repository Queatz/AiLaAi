import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.AppStyles
import app.ailaai.api.statsFeedback
import com.queatz.db.AppFeedback
import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import kotlin.js.Date

@Composable
fun FeedbackPlatformPage() {
    var feedback by remember {
        mutableStateOf(emptyList<AppFeedback>())
    }

    LaunchedEffect(Unit) {
        api.statsFeedback {
            feedback = it
        }
    }

    Div({
        style {
            padding(1.r)
            maxWidth(800.px)
            property("margin", "0 auto")
        }
    }) {
        H1({
            style {
                fontSize(32.px)
                fontWeight("bold")
                marginBottom(2.r)
            }
        }) {
            // todo: translate
            Text("Feedback")
        }

        if (feedback.isEmpty()) {
            Div({
                style {
                    padding(24.px)
                    property("text-align", "center")
                    color(Styles.colors.secondary)
                    fontSize(18.px)
                }
            }) {
                // todo: translate
                Text("No feedback yet")
            }
        } else {
            Div {
                feedback.forEach { item ->
                    Div({
                        classes(AppStyles.scriptItem)

                        style {
                            marginBottom(1.r)
                        }

                        onClick {
                            item.person?.let { person ->
                                window.open("/profile/$person", target = "_blank")
                            }
                        }
                    }) {
                        Div({
                            style {
                                fontSize(16.px)
                                property("line-height", "1.5")
                                marginBottom(1.r)
                            }
                        }) {
                            Text(item.feedback.orEmpty())
                        }

                        Div({
                            style {
                                fontSize(14.px)
                                color(Styles.colors.secondary)
                                property("text-align", "right")
                            }
                        }) {
                            val date = try {
                                Date(item.createdAt?.toString().orEmpty())
                            } catch (e: Exception) {
                                null
                            }

                            Text(date?.toLocaleDateString() ?: item.createdAt.toString())
                        }
                    }
                }
            }
        }
    }
}

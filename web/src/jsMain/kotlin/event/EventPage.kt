package event

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.FullPageLayout
import app.ailaai.api.joinReminder
import app.ailaai.api.leaveReminder
import app.ailaai.api.profile
import app.ailaai.api.reminder
import app.components.Empty
import app.reminder.scheduleText
import app.softwork.routingcompose.Router
import appString
import appText
import application
import baseUrl
import com.queatz.db.Person
import com.queatz.db.Reminder
import components.LinkifyText
import components.Loading
import components.ProfilePhoto
import kotlinx.browser.window
import kotlinx.coroutines.launch
import mainContent
import notBlank
import notEmpty
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.backgroundPosition
import org.jetbrains.compose.web.css.backgroundSize
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Text
import r
import stories.StoryContents
import stories.asStoryContents

@Composable
fun EventPage(reminderId: String) {
    var reminder by remember { mutableStateOf<Reminder?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val router = Router.current
    val me = application.me.collectAsState().value

    LaunchedEffect(reminderId) {
        api.reminder(reminderId) {
            reminder = it
        }
        isLoading = false
    }

    Div({
        mainContent()
    }) {
        FullPageLayout {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    margin(1.r)
                }
            }) {
                if (isLoading) {
                    Loading()
                } else if (reminder == null) {
                    Empty {
                        // todo: translate
                        Text("Event not found.")
                    }
                } else {
                    val reminder = reminder!!
                    // Photo
                    reminder.photo?.let { photoUrl ->
                        Div({
                            style {
                                width(100.percent)
                                marginBottom(1.r)
                                borderRadius(1.r)
                                overflow("hidden")
                                backgroundImage("url($baseUrl$photoUrl)")
                                backgroundSize("cover")
                                backgroundPosition("center")
                                backgroundColor(Styles.colors.background)
                                property("aspect-ratio", "1.5")
                            }
                        })
                    }

                    // Title
                    H1({
                        style {
                            marginBottom(1.r)
                        }
                    }) {
                        // todo: translate
                        Text(reminder.title ?: "Untitled event")
                    }

                    // Event time + duration
                    Div({
                        style {
                            marginBottom(1.r)
                            color(Styles.colors.secondary)
                        }
                    }) {
                        Text(reminder.scheduleText)
                    }

                    // People
                    (reminder.people.orEmpty() + reminder.person!!).notEmpty?.let { people ->
                        Div({
                            style {
                                marginBottom(1.r)
                            }
                        }) {
                            // todo: translate
                            H3 {
                                appText { this.people }
                            }

                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    flexWrap(FlexWrap.Wrap)
                                    gap(.5f.r)
                                }
                            }) {
                                people.forEach { personId ->
                                    var person by remember(personId) {
                                        mutableStateOf<Person?>(null)
                                    }
                                    LaunchedEffect(personId) {
                                        api.profile(personId) {
                                            person = it.person
                                        }
                                    }
                                    ProfilePhoto(
                                        photo = person?.photo,
                                        name = person?.name ?: appString { someone },
                                        size = 48.px,
                                        onClick = {
                                            window.open("/profile/$personId", "_blank")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Note
                    reminder.note?.notBlank?.let { note ->
                        Div({
                            style {
                                marginBottom(1.r)
                                padding(1.r)
                                border(1.px, LineStyle.Solid, Styles.colors.outline)
                                borderRadius(1.r)
                            }
                        }) {
                            LinkifyText(note)
                        }
                    }

                    // Content
                    val content = remember(reminder.content) {
                        reminder.content?.notBlank?.asStoryContents() ?: emptyList()
                    }

                    // Display reminder content if available
                    if (content.isNotEmpty()) {
                        Div({
                            style {
                                marginLeft(1.r)
                                marginRight(1.r)
                            }
                        }) {
                            StoryContents(
                                content = content
                            )
                        }
                    }

                    if (me?.id != reminder.person) {
                        // Join/Leave button
                        val isInReminder = me?.id in (reminder.people ?: emptyList())

                        var isJoiningOrLeaving by remember { mutableStateOf(false) }

                        Button({
                            classes(if (isInReminder) Styles.outlineButton else Styles.button)

                            if (isJoiningOrLeaving) {
                                disabled()
                            }

                            onClick {
                                if (me == null) {
                                    router.navigate("/signin")
                                } else {
                                    scope.launch {
                                        isJoiningOrLeaving = true
                                        if (isInReminder) {
                                            api.leaveReminder(reminderId) {
                                                // todo: reload
                                            }
                                        } else {
                                            api.joinReminder(reminderId) {
                                                // todo: reload
                                            }
                                        }
                                        isJoiningOrLeaving = false
                                    }
                                }
                            }
                        }) {
                            // todo: translate
                            Text(if (isInReminder) "Leave" else "Join")
                        }
                    }
                }
            }
        }
    }
}

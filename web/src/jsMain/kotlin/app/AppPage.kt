package app

import androidx.compose.runtime.*
import app.cards.CardsPage
import app.group.GroupPage
import app.nav.*
import app.page.SchedulePage
import app.page.ScheduleView
import app.page.StoriesPage
import app.widget.WidgetStyles
import application
import com.queatz.db.Card
import com.queatz.db.Reminder
import com.queatz.db.Story
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div

@Serializable
enum class NavPage {
    Groups,
    Schedule,
    Cards,
    Stories,
    Profile
}

@Composable
fun AppPage() {
    Style(AppStyles)
    Style(WidgetStyles)

    val scope = rememberCoroutineScope()

    var nav by remember {
        mutableStateOf(application.navPage)
    }

    var group by remember {
        mutableStateOf<GroupNav>(GroupNav.None)
    }

    var card by remember {
        mutableStateOf<CardNav>(CardNav.Local)
    }

    var reminder by remember {
        mutableStateOf<Reminder?>(null)
    }

    val cardUpdates = remember {
        MutableSharedFlow<Card>()
    }

    val groupUpdates = remember {
        MutableSharedFlow<Unit>()
    }

    val storyUpdates = remember {
        MutableSharedFlow<Story>()
    }

    val reminderUpdates = remember {
        MutableSharedFlow<Reminder>()
    }

    var story by remember {
        mutableStateOf<StoryNav>(StoryNav.Local)
    }

    var scheduleView by remember {
        mutableStateOf(ScheduleView.Monthly)
    }

    val goToToday = remember {
        MutableSharedFlow<Unit>()
    }

    LaunchedEffect(nav) {
        application.setNavPage(nav)
    }

    Div({
        classes(AppStyles.baseLayout)
    }) {
        Div({
            classes(AppStyles.sideLayout)
        }) {
            AppBottomBar(nav) { nav = it }
            Div({
                style {
                    flexGrow(1)
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    overflow("hidden")
                }
            }) {
                when (nav) {
                    NavPage.Groups -> GroupsNavPage(
                        groupUpdates,
                        group,
                        onSelected = {
                            group = it
                        },
                        onProfileClick = {
                            nav = NavPage.Profile
                        }
                    )

                    NavPage.Schedule -> ScheduleNavPage(
                        reminderUpdates,
                        reminder,
                        { reminder = it },
                        {
                            scope.launch {
                                reminderUpdates.emit(it)
                            }
                        },
                        scheduleView,
                        {
                            reminder = null

                            if (scheduleView == it) {
                                scope.launch { goToToday.emit(Unit) }
                            } else {
                                scheduleView = it
                            }
                        },
                        {
                            nav = NavPage.Profile
                        }
                    )

                    NavPage.Cards -> CardsNavPage(cardUpdates, card, { card = it }, {
                        nav = NavPage.Profile
                    })
                    NavPage.Stories -> StoriesNavPage(storyUpdates, story, { story = it }, { nav = NavPage.Profile })
                    NavPage.Profile -> ProfileNavPage {
                        nav = NavPage.Groups
                    }
                }
            }
        }
        Div({
            classes(AppStyles.mainLayout)
        }) {
            when (nav) {
                NavPage.Groups -> GroupPage(
                    group,
                    onGroup = {
                              group = GroupNav.Selected(it)
                    },
                    onGroupUpdated = {
                        scope.launch {
                            groupUpdates.emit(Unit)
                        }
                    },
                    onGroupGone = {
                        scope.launch {
                            groupUpdates.emit(Unit)
                        }
                    }
                )

                NavPage.Schedule -> SchedulePage(
                    scheduleView,
                    reminder,
                    goToToday,
                    { reminder = it },
                    onUpdate = {
                        scope.launch {
                            reminderUpdates.emit(it)
                        }
                    },
                    onDelete = {
                        scope.launch {
                            reminder = null
                            reminderUpdates.emit(it)
                        }
                    }
                )
                NavPage.Cards -> CardsPage(card, { card = it }) {
                    scope.launch {
                        cardUpdates.emit(it)
                    }
                }

                NavPage.Stories -> StoriesPage(
                    story,
                    onStoryUpdated = {
                        scope.launch {
                            storyUpdates.emit(it)
                        }
                    },
                    onGroupClick = {
                        group = GroupNav.Selected(it)
                        nav = NavPage.Groups
                    }
                )
                NavPage.Profile -> {

                }
            }
        }
    }
}

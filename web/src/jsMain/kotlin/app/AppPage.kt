package app

import Notification
import androidx.compose.runtime.*
import api
import app.ailaai.api.group
import app.cards.CardsPage
import app.group.GroupPage
import app.nav.*
import app.page.SchedulePage
import app.page.ScheduleView
import app.page.StoriesPage
import app.widget.WidgetStyles
import appString
import application
import call
import com.queatz.db.Card
import com.queatz.db.Reminder
import com.queatz.db.Story
import com.queatz.push.CallPushData
import com.queatz.push.PushAction
import com.queatz.push.ReminderPushData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import notifications
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Audio
import org.jetbrains.compose.web.dom.Div
import push
import stories.StoryStyles

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
    Style(StoryStyles)

    val scope = rememberCoroutineScope()
    val background by application.background.collectAsState(null)
    val me by application.me.collectAsState()

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

    var playCallSound by remember {
        mutableStateOf(false)
    }

    var playNotificationSound by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(nav) {
        application.setNavPage(nav)
    }

    val someone = appString { someone }

    LaunchedEffect(Unit) {
        push.events.filter {
            it.action == PushAction.Call
        }.collect {
            val data = it.data as? CallPushData
            if (data?.show == true) {
                playCallSound = true
                notifications.add(
                    Notification(
                        "call",
                        data.group.name ?: data.person.name ?: someone,
                        // todo translate
                        if (call.active.value == null) "Tap to answer" else "Tap to switch",
                        onDismiss = {
                            playCallSound = false
                        }
                    ) {
                        playCallSound = false
                        scope.launch {
                            api.group(data.group.id!!) {
                                call.join(me!!, it)
                            }
                        }
                    }
                )
            }
        }
    }

    val reminderString = appString { this.reminder }

    LaunchedEffect(Unit) {
        push.events.filter {
            it.action == PushAction.Reminder
        }.collect {
            val data = it.data as? ReminderPushData
            if (data?.show == true) {
                playNotificationSound = true
                notifications.add(
                    Notification(
                        null,
                        data.reminder.title ?: reminderString,
                        // todo translate
                        data.occurrence?.note ?: data.reminder.note ?: "",
                        onDismiss = {
                            playNotificationSound = false
                        }
                    ) {
                        playNotificationSound = false
                        nav = NavPage.Schedule
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        appNav.navigate.collectLatest {
            when (it) {
                is AppNavigation.Group -> {
                    if (it.groupExtended == null) {
                        api.group(it.group) {
                            nav = NavPage.Groups
                            group = GroupNav.Selected(it)
                        }
                    } else {
                        nav = NavPage.Groups
                        group = GroupNav.Selected(it.groupExtended)
                    }
                }
            }
        }
    }

    if (playCallSound || playNotificationSound) {
        Audio({
            attr("playsinline", "true")
            attr("autoplay", "true")

            style {
                display(DisplayStyle.None)
            }

            ref {
                it.src = if (playCallSound) "/call.ogg" else "/notify.ogg"
                it.currentTime = 0.0
                it.onended = {
                    playCallSound = false
                    Unit
                }
                it.oncanplay = { _ ->
                    it.play()
                    Unit
                }

                onDispose { }
            }
        }) {}
    }

    Div({
        classes(AppStyles.baseLayout)

        style {
            if (background != null) {
                backgroundImage("url($background)")
            }
        }
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

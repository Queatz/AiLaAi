package app

import Notification
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.card
import app.ailaai.api.comment
import app.ailaai.api.gameScene
import app.ailaai.api.group
import app.ailaai.api.script
import app.cards.CardsPage
import app.components.Background
import app.group.FeaturePreview
import app.group.GroupPage
import app.nav.CardNav
import app.nav.CardsNavPage
import app.nav.GroupNav
import app.nav.GroupsNavPage
import app.nav.ProfileNavPage
import app.nav.SceneNav
import app.nav.SceneNavPage
import app.nav.ScheduleNavPage
import app.nav.StoriesNavPage
import app.nav.StoryNav
import app.page.ScenePage
import app.page.SchedulePage
import app.page.ScheduleView
import app.page.ScheduleViewType
import app.page.StoriesPage
import app.platform.PlatformNav
import app.platform.PlatformNavPage
import app.platform.PlatformPage
import app.scripts.ScriptsNav
import app.scripts.ScriptsNavPage
import app.scripts.ScriptsPage
import app.widget.WidgetStyles
import appString
import application
import asNaturalList
import call
import com.queatz.db.Card
import com.queatz.db.GameScene
import com.queatz.db.Reminder
import com.queatz.db.Script
import com.queatz.db.Story
import com.queatz.push.CallPushData
import com.queatz.push.CommentPushData
import com.queatz.push.CommentReplyPushData
import com.queatz.push.MessagePushData
import com.queatz.push.PushAction
import com.queatz.push.ReminderPushData
import com.queatz.push.StoryPushData
import components.IconButton
import format
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import lib.hidden
import notBlank
import notifications
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.dom.Audio
import org.jetbrains.compose.web.dom.Div
import push
import stories.StoryStyles
import stories.commentDialog

@Serializable
enum class NavPage {
    Groups,
    Schedule,
    Cards,
    Stories,
    Apps,
    Profile,
    Platform,
    Scripts,
    Scenes,
}

@Composable
fun AppPage() {
    Style(WidgetStyles)
    Style(StoryStyles)

    val scope = rememberCoroutineScope()
    val me by application.me.collectAsState()

    var nav by remember {
        mutableStateOf(application.navPage)
    }

    var group by remember {
        mutableStateOf<GroupNav>(GroupNav.None)
    }

    var card by remember {
        mutableStateOf<CardNav>(CardNav.Map)
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

    val scriptUpdates = remember {
        MutableSharedFlow<Script>()
    }

    val reminderUpdates = remember {
        MutableSharedFlow<Reminder>()
    }

    val sceneUpdates = remember {
        MutableSharedFlow<GameScene>()
    }

    var reminderSearch by remember {
        mutableStateOf<String?>(null)
    }

    var story by remember {
        mutableStateOf<StoryNav>(StoryNav.Friends)
    }

    var scheduleView by remember {
        mutableStateOf(ScheduleView.Daily)
    }

    var scheduleViewType by remember {
        mutableStateOf(ScheduleViewType.Schedule)
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

    var playMessageSound by remember {
        mutableStateOf(false)
    }

    var sideLayoutVisible by remember {
        mutableStateOf(true) // Default to visible
    }

    var platform by remember {
        mutableStateOf<PlatformNav>(PlatformNav.None)
    }

    var script by remember {
        mutableStateOf<ScriptsNav>(ScriptsNav.None)
    }

    var scene by remember {
        mutableStateOf<SceneNav>(SceneNav.None)
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
                        icon = "call",
                        title = data.group.name ?: data.person.name ?: someone,
                        message = if (call.active.value == null) {
                            application.appString { tapToAnswer }
                        } else {
                            application.appString { tapToSwitch }
                        },
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
                // todo: reminder as alarm
                notifications.add(
                    Notification(
                        null,
                        data.reminder.title ?: reminderString,
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
        push.events.filter {
            it.action == PushAction.Story
        }.collect {
            val data = it.data as? StoryPushData
            playNotificationSound = true
            notifications.add(
                Notification(
                    null,
                    data?.story?.title ?: reminderString,
                    "${data!!.authors.asNaturalList(application) { it.name ?: someone }} posted a story",
                    onDismiss = {
                        playNotificationSound = false
                    }
                ) {
                    playNotificationSound = false
                    window.open("/story/${data.story.id!!}", target = "_blank")
                }
            )
        }
    }

    fun showComment(comment: String) {
        scope.launch {
            api.comment(comment) {
                commentDialog(it)
            }
        }
    }

    LaunchedEffect(Unit) {
        push.events.filter {
            it.action == PushAction.Comment || it.action == PushAction.CommentReply
        }.collect {
            when (val data = it.data) {
                is CommentPushData -> {
                    if (data.person?.id != me?.id) {
                        playNotificationSound = true
                        notifications.add(
                            Notification(
                                icon = null,
                                title = application
                                    .appString { personCommentedOnYourStory }
                                    .format(data.person!!.name ?: someone),
                                message = data.comment?.comment ?: "",
                                onDismiss = {
                                    playNotificationSound = false
                                }
                            ) {
                                playNotificationSound = false
                                window.open("/story/${data.story!!.id!!}", target = "_blank")
                            }
                        )
                    }
                }
                is CommentReplyPushData -> {
                    if (data.person?.id != me?.id) {
                        playNotificationSound = true
                        notifications.add(
                            Notification(
                                icon = null,
                                title = application
                                    .appString { personRepliedToYourComment }
                                    .format(data.person!!.name ?: someone),
                                message = data.comment?.comment ?: "",
                                onDismiss = {
                                    playNotificationSound = false
                                }
                            ) {
                                playNotificationSound = false
                                showComment(data.onComment!!.id!!)
                            }
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        push.events.filter {
            it.action == PushAction.Message
        }.collect {
            if (document.hidden) {
                if ((it.data as? MessagePushData)?.person?.id != me?.id) {
                    playMessageSound = true
                }
            }
        }
    }

    // Perform the AppNav navigation
    LaunchedEffect(Unit) {
        appNav.navigate.collectLatest {
            when (it) {
                is AppNavigation.Nav -> {
                    nav = it.nav
                }
                is AppNavigation.Group -> {
                    if (it.groupExtended == null) {
                        api.group(it.id) {
                            nav = NavPage.Groups
                            group = GroupNav.Selected(it)
                        }
                    } else {
                        nav = NavPage.Groups
                        group = GroupNav.Selected(it.groupExtended)
                    }
                }
                is AppNavigation.Page -> {
                    if (it.card == null) {
                        api.card(it.id) {
                            nav = NavPage.Cards
                            card = CardNav.Selected(it)
                        }
                    } else {
                        nav = NavPage.Cards
                        card = CardNav.Selected(it.card)
                    }
                }
                is AppNavigation.ExploreScripts -> {
                    script = ScriptsNav.Explore
                }
                is AppNavigation.Script -> {
                    if (it.script == null) {
                        api.script(it.id) {
                            script = ScriptsNav.Script(it)
                        }
                    } else {
                        script = ScriptsNav.Script(it.script)
                    }
                }
                is AppNavigation.GameScene -> {
                    // Handle GameScene navigation
                    val gameSceneId = it.id

                    if (it.gameScene == null) {
                        // If gameScene is null, fetch it by ID
                        api.gameScene(gameSceneId) { fetchedScene ->
                            nav = NavPage.Scenes
                            scene = SceneNav.Selected(fetchedScene)
                        }
                    } else {
                        // If gameScene is provided, use it directly
                        nav = NavPage.Scenes
                        scene = SceneNav.Selected(it.gameScene)
                    }
                }
            }
        }
    }

    if (playCallSound || playNotificationSound || playMessageSound) {
        Audio({
            attr("playsinline", "true")
            attr("autoplay", "true")

            style {
                display(DisplayStyle.None)
            }

            ref {
                it.src = if (playCallSound) "/call.ogg" else if (playMessageSound) "/message.ogg" else "/notify.ogg"
                it.currentTime = 0.0
                it.onended = {
                    playCallSound = false
                    playNotificationSound = false
                    playMessageSound = false
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

    Background({
        classes(AppStyles.baseLayout)
    }) {
        Div({
            classes(AppStyles.sideLayout)
            style {
                // Add conditional styling based on visibility
                if (!sideLayoutVisible) {
                    display(DisplayStyle.None)
                }
            }
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
                        groupUpdates = groupUpdates,
                        selected = group,
                        onSelected = {
                            group = it
                        },
                        onProfileClick = {
                            nav = NavPage.Profile
                        }
                    )

                    NavPage.Schedule -> ScheduleNavPage(
                        reminderUpdates = reminderUpdates,
                        reminder = reminder,
                        onReminder = { reminder = it },
                        onUpdate = {
                            scope.launch {
                                reminderUpdates.emit(it)

                                // This will reload the list
                                goToToday.emit(Unit)
                            }
                        },
                        view = scheduleView,
                        onViewClick = {
                            reminder = null

                            if (scheduleView == it) {
                                scope.launch { goToToday.emit(Unit) }
                            } else {
                                scheduleView = it
                            }
                        },
                        viewType = scheduleViewType,
                        onViewTypeClick = {
                            scheduleViewType = it
                        },
                        onProfileClick = {
                            nav = NavPage.Profile
                        },
                        onSearchChange = {
                            reminderSearch = it.notBlank
                        }
                    )

                    NavPage.Cards -> CardsNavPage(cardUpdates, card, { card = it }, {
                        nav = NavPage.Profile
                    })

                    NavPage.Stories -> StoriesNavPage(storyUpdates, story, { story = it }, { nav = NavPage.Profile })
                    NavPage.Profile -> ProfileNavPage(
                        onProfileClick = {
                            nav = NavPage.Groups
                        },
                        onPlatformClick = {
                            nav = NavPage.Platform
                        },
                        onScriptsClick = {
                            nav = NavPage.Scripts
                        },
                        onScenesClick = {
                            nav = NavPage.Scenes
                        }
                    )
                    NavPage.Platform -> PlatformNavPage(
                        onProfileClick = {
                            nav = NavPage.Profile
                        },
                        selected = platform,
                        onSelected = { platform = it }
                    )
                    NavPage.Scripts -> ScriptsNavPage(
                        updates = scriptUpdates,
                        selected = script,
                        onSelected = {
                            scope.launch {
                                when (it) {
                                    is ScriptsNav.Explore -> {
                                        appNav.navigate(AppNavigation.ExploreScripts)
                                    }
                                    is ScriptsNav.Script -> {
                                        appNav.navigate(AppNavigation.Script(it.script.id!!, it.script))
                                    }
                                    else -> {
                                        script = it
                                    }
                                }
                            }
                        },
                        onCreated = {
                            scope.launch {
                                scriptUpdates.emit(it)
                            }
                        },
                        onProfileClick = {
                            nav = NavPage.Profile
                        }
                    )
                    NavPage.Apps -> FeaturePreview(listOf(NavPage.Scenes, NavPage.Scripts))
                    NavPage.Scenes -> SceneNavPage(
                        selected = scene,
                        onSelected = { selectedScene ->
                            scene = selectedScene
                        },
                        onBackClick = {
                            nav = NavPage.Profile
                        },
                        updates = sceneUpdates
                    )
                }
            }
        }
        Div({
            classes(AppStyles.mainLayout)
        }) {
            // Add toggle button at the top of mainLayout
            Div({
                classes(AppStyles.fullscreenButton)
            }) {
                IconButton(
                    name = if (sideLayoutVisible) "fullscreen" else "fullscreen_exit",
                    title = appString { fullscreen },
                    background = true,
                    styles = {
                        opacity(.5f)
                    }

                ) {
                    // Toggle the visibility state
                    sideLayoutVisible = !sideLayoutVisible
                }
            }
            when (nav) {
                NavPage.Profile -> {
                    FeaturePreview()
                }

                NavPage.Groups -> GroupPage(
                    nav = group,
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
                            group = GroupNav.None
                        }
                    }
                )

                NavPage.Schedule -> SchedulePage(
                    view = scheduleView,
                    viewType = scheduleViewType,
                    reminder = reminder,
                    search = reminderSearch,
                    goToToday = goToToday,
                    onReminder = { reminder = it },
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
                    selected = story,
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
                NavPage.Apps -> {
                }

                NavPage.Profile -> {

                }

                NavPage.Platform -> PlatformPage(platform)

                NavPage.Scripts -> ScriptsPage(
                    nav = script,
                    onUpdate = {
                        scope.launch {
                            scriptUpdates.emit(it)
                        }
                    },
                    onScriptDeleted = {
                         scope.launch {
                             script = ScriptsNav.None
                             scriptUpdates.emit(it)
                        }
                    },
                    onScriptCreated = {
                        scope.launch {
                            script = ScriptsNav.Script(it)
                            scriptUpdates.emit(it)
                        }
                    }
                )

                NavPage.Scenes -> {
                    ScenePage(
                        nav = scene,
                        onBackClick = {
                            scene = SceneNav.None
                        },
                        onSceneSelected = { selectedScene ->
                            scene = selectedScene
                        },
                        onSceneUpdated = {
                            scope.launch {
                                sceneUpdates.emit(it)
                            }
                        }
                    )
                }
            }
        }
    }
}

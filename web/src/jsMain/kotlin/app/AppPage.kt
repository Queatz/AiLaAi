package app

import Notification
import StyleManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import app.nav.AppNavPage
import app.nav.CardNav
import app.nav.CardsNavPage
import app.nav.GroupNav
import app.nav.GroupsNavPage
import app.nav.ProfileNavPage
import app.nav.SceneNav
import app.nav.SceneNavPage
import app.nav.ScheduleNavPage
import app.nav.StoriesNavPage
import app.page.ScenePage
import app.page.SchedulePage
import app.page.StoriesPage
import app.platform.PlatformNavPage
import app.platform.PlatformPage
import app.scripts.ScriptsNav
import app.scripts.ScriptsNavPage
import app.scripts.ScriptsPage
import app.softwork.routingcompose.Router
import app.widget.WidgetStyleSheet
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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import lib.hidden
import notBlank
import notifications
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.dom.Audio
import org.jetbrains.compose.web.dom.Div
import push
import stories.StoryStyleSheet
import stories.commentDialog
import kotlin.time.Duration.Companion.seconds

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
fun AppPage(tabId: String? = null) {
    StyleManager.use(
        WidgetStyleSheet::class,
        StoryStyleSheet::class
    )

    val scope = rememberCoroutineScope()
    val me by application.me.collectAsState()
    val router = Router.current

    LaunchedEffect(tabId) {
        if (tabId != null) {
            val savedState = AppPageStateManager.loadState(tabId)
            if (savedState != null) {
                AppPageStateManager.updateState { savedState }
            }
        } else {
            val newTabId = tabStateManager.generateTabId()
            router.navigate("/tab/$newTabId")
        }
    }

    // Get state from AppPageStateManager
    val state by AppPageStateManager.state.collectAsState()

    // Create individual state variables for backward compatibility
    var nav by remember {
        mutableStateOf(state.nav)
    }

    var group by remember {
        mutableStateOf(state.groupNav)
    }

    var card by remember {
        mutableStateOf(state.cardNav)
    }

    var story by remember {
        mutableStateOf(state.storyNav)
    }

    var platform by remember {
        mutableStateOf(state.platformNav)
    }

    var script by remember {
        mutableStateOf(state.scriptsNav)
    }

    var scene by remember {
        mutableStateOf(state.sceneNav)
    }

    var scheduleView by remember {
        mutableStateOf(state.scheduleView)
    }

    var scheduleViewType by remember {
        mutableStateOf(state.scheduleViewType)
    }

    var reminderSearch by remember {
        mutableStateOf(state.reminderSearch)
    }

    var reminder by remember {
        mutableStateOf(state.reminder)
    }

    var sideLayoutVisible by remember {
        mutableStateOf(state.sideLayoutVisible)
    }

    // Update individual state variables when state changes
    LaunchedEffect(state) {
        nav = state.nav
        group = state.groupNav
        card = state.cardNav
        story = state.storyNav
        platform = state.platformNav
        script = state.scriptsNav
        scene = state.sceneNav
        scheduleView = state.scheduleView
        scheduleViewType = state.scheduleViewType
        reminderSearch = state.reminderSearch
        reminder = state.reminder
        sideLayoutVisible = state.sideLayoutVisible
    }

    // Create flow objects for updates
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

    // Save state when it changes
    LaunchedEffect(state) {
        if (tabId != null) {
            tabStateManager.saveState(tabId, state)
        }
    }

    LaunchedEffect(state.nav) {
        application.setNavPage(state.nav)
    }

    // Add window event listener for tab lifecycle
    DisposableEffect(tabId) {
        var handler: ((dynamic) -> dynamic)? = null

        if (tabId != null) {
            handler = AppPageStateManager.setupBeforeUnloadHandler(tabId) {
                AppPageStateManager.state.value
            }
        }

        onDispose {
            if (handler != null && tabId != null) {
                window.removeEventListener("beforeunload", handler)
            }
        }
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
                        AppPageStateManager.updateState { currentState ->
                            currentState.copy(nav = NavPage.Schedule)
                        }
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
                    AppPageStateManager.updateState { currentState ->
                        currentState.copy(nav = it.nav)
                    }
                }
                is AppNavigation.Group -> {
                    if (it.groupExtended == null) {
                        api.group(it.id) { group ->
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(
                                    nav = NavPage.Groups,
                                    groupNav = GroupNav.Selected(group)
                                )
                            }
                        }
                    } else {
                        AppPageStateManager.updateState { currentState ->
                            currentState.copy(
                                nav = NavPage.Groups,
                                groupNav = GroupNav.Selected(it.groupExtended)
                            )
                        }
                    }
                }
                is AppNavigation.Page -> {
                    if (it.card == null) {
                        api.card(it.id) { card ->
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(
                                    nav = NavPage.Cards,
                                    cardNav = CardNav.Selected(card)
                                )
                            }
                        }
                    } else {
                        AppPageStateManager.updateState { currentState ->
                            currentState.copy(
                                nav = NavPage.Cards,
                                cardNav = CardNav.Selected(it.card)
                            )
                        }
                    }
                }
                is AppNavigation.ExploreScripts -> {
                    AppPageStateManager.updateState { currentState ->
                        currentState.copy(scriptsNav = ScriptsNav.Explore)
                    }
                }
                is AppNavigation.Script -> {
                    if (it.script == null) {
                        api.script(it.id) { script ->
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(scriptsNav = ScriptsNav.Script(script))
                            }
                        }
                    } else {
                        AppPageStateManager.updateState { currentState ->
                            currentState.copy(scriptsNav = ScriptsNav.Script(it.script))
                        }
                    }
                }
                is AppNavigation.ExploreScenes -> {
                    AppPageStateManager.updateState { currentState ->
                        currentState.copy(sceneNav = SceneNav.Explore)
                    }
                }
                is AppNavigation.GameScene -> {
                    // Handle GameScene navigation
                    val gameSceneId = it.id

                    if (it.gameScene == null) {
                        // If gameScene is null, fetch it by ID
                        api.gameScene(gameSceneId) { fetchedScene ->
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(
                                    nav = NavPage.Scenes,
                                    sceneNav = SceneNav.Selected(fetchedScene)
                                )
                            }
                        }
                    } else {
                        // If gameScene is provided, use it directly
                        AppPageStateManager.updateState { currentState ->
                            currentState.copy(
                                nav = NavPage.Scenes,
                                sceneNav = SceneNav.Selected(it.gameScene)
                            )
                        }
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
                if (!state.sideLayoutVisible) {
                    display(DisplayStyle.None)
                }
            }
        }) {
            AppBottomBar(nav) { newNav ->
                AppPageStateManager.updateState { currentState ->
                    currentState.copy(nav = newNav)
                }
            }
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
                        onSelected = { newGroup ->
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(groupNav = newGroup)
                            }
                        },
                        onProfileClick = {
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(nav = NavPage.Profile)
                            }
                        }
                    )

                    NavPage.Schedule -> ScheduleNavPage(
                        reminderUpdates = reminderUpdates,
                        reminder = reminder,
                        onReminder = { newReminder ->
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(reminder = newReminder)
                            }
                        },
                        onUpdate = {
                            scope.launch {
                                reminderUpdates.emit(it)

                                // This will reload the list
                                goToToday.emit(Unit)
                            }
                        },
                        view = scheduleView,
                        onViewClick = { newView ->
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(reminder = null)
                            }

                            if (scheduleView == newView) {
                                scope.launch { goToToday.emit(Unit) }
                            } else {
                                AppPageStateManager.updateState { currentState ->
                                    currentState.copy(scheduleView = newView)
                                }
                            }
                        },
                        viewType = scheduleViewType,
                        onViewTypeClick = { newViewType ->
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(scheduleViewType = newViewType)
                            }
                        },
                        onProfileClick = {
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(nav = NavPage.Profile)
                            }
                        },
                        onSearchChange = { newSearch ->
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(reminderSearch = newSearch.notBlank)
                            }
                        }
                    )

                    NavPage.Cards -> CardsNavPage(
                        cardUpdates, 
                        card, 
                        { newCard ->
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(cardNav = newCard)
                            }
                        }, 
                        {
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(nav = NavPage.Profile)
                            }
                        }
                    )

                    NavPage.Stories -> StoriesNavPage(
                        storyUpdates, 
                        story, 
                        { newStory ->
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(storyNav = newStory)
                            }
                        }, 
                        {
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(nav = NavPage.Profile)
                            }
                        }
                    )
                    NavPage.Profile -> ProfileNavPage(
                        onProfileClick = {
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(nav = NavPage.Groups)
                            }
                        },
                        onPlatformClick = {
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(nav = NavPage.Platform)
                            }
                        },
                        onScriptsClick = {
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(nav = NavPage.Scripts)
                            }
                        },
                        onScenesClick = {
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(nav = NavPage.Scenes)
                            }
                        }
                    )
                    NavPage.Platform -> PlatformNavPage(
                        onProfileClick = {
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(nav = NavPage.Profile)
                            }
                        },
                        selected = platform,
                        onSelected = { newPlatform ->
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(platformNav = newPlatform)
                            }
                        }
                    )
                    NavPage.Scripts -> ScriptsNavPage(
                        updates = scriptUpdates,
                        selected = script,
                        onSelected = { newScript ->
                            scope.launch {
                                when (newScript) {
                                    is ScriptsNav.Explore -> {
                                        appNav.navigate(AppNavigation.ExploreScripts)
                                    }
                                    is ScriptsNav.Script -> {
                                        appNav.navigate(AppNavigation.Script(newScript.script.id!!, newScript.script))
                                    }
                                    else -> {
                                        AppPageStateManager.updateState { currentState ->
                                            currentState.copy(scriptsNav = newScript)
                                        }
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
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(nav = NavPage.Profile)
                            }
                        }
                    )
                    NavPage.Apps -> AppNavPage()
                    NavPage.Scenes -> SceneNavPage(
                        selected = scene,
                        onSelected = { selectedScene ->
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(sceneNav = selectedScene)
                            }
                        },
                        onBackClick = {
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(nav = NavPage.Profile)
                            }
                        },
                        updates = sceneUpdates
                    )
                }
            }
        }
        Div({
            classes(AppStyles.mainLayout)
        }) {
            var isFullscreenHovered by remember { mutableStateOf(false) }
            var isFullscreenHoveredTime by remember { mutableStateOf(Instant.DISTANT_PAST) }
            // Add toggle button at the top of mainLayout
            Div({
                classes(AppStyles.fullscreenButton)

                if (isFullscreenHovered) {
                    classes(AppStyles.fullscreenButtonHovered)
                }

                onMouseEnter {
                    if (!isFullscreenHovered) {
                        isFullscreenHovered = true
                        isFullscreenHoveredTime = Clock.System.now()
                    }
                }

                onMouseLeave {
                    if (isFullscreenHoveredTime < Clock.System.now() - 1.seconds) {
                        isFullscreenHovered = false
                    }
                }
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
                    AppPageStateManager.updateState { currentState ->
                        currentState.copy(sideLayoutVisible = !currentState.sideLayoutVisible)
                    }
                }
            }
            when (nav) {
                NavPage.Profile -> {
                    FeaturePreview()
                }

                NavPage.Groups -> GroupPage(
                    nav = group,
                    onGroup = { newGroup ->
                        AppPageStateManager.updateState { currentState ->
                            currentState.copy(groupNav = GroupNav.Selected(newGroup))
                        }
                    },
                    onGroupUpdated = {
                        scope.launch {
                            groupUpdates.emit(Unit)
                        }
                    },
                    onGroupGone = {
                        scope.launch {
                            groupUpdates.emit(Unit)
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(groupNav = GroupNav.None)
                            }
                        }
                    }
                )

                NavPage.Schedule -> SchedulePage(
                    view = scheduleView,
                    viewType = scheduleViewType,
                    reminder = reminder,
                    search = reminderSearch,
                    goToToday = goToToday,
                    onReminder = { newReminder ->
                        AppPageStateManager.updateState { currentState ->
                            currentState.copy(reminder = newReminder)
                        }
                    },
                    onUpdate = {
                        scope.launch {
                            reminderUpdates.emit(it)
                        }
                    },
                    onDelete = {
                        scope.launch {
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(reminder = null)
                            }
                            reminderUpdates.emit(it)
                        }
                    }
                )

                NavPage.Cards -> CardsPage(
                    card, 
                    { newCard ->
                        AppPageStateManager.updateState { currentState ->
                            currentState.copy(cardNav = newCard)
                        }
                    }
                ) {
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
                    onGroupClick = { clickedGroup ->
                        AppPageStateManager.updateState { currentState ->
                            currentState.copy(
                                groupNav = GroupNav.Selected(clickedGroup),
                                nav = NavPage.Groups
                            )
                        }
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
                             AppPageStateManager.updateState { currentState ->
                                 currentState.copy(scriptsNav = ScriptsNav.None)
                             }
                             scriptUpdates.emit(it)
                        }
                    },
                    onScriptCreated = {
                        scope.launch {
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(scriptsNav = ScriptsNav.Script(it))
                            }
                            scriptUpdates.emit(it)
                        }
                    }
                )

                NavPage.Scenes -> {
                    ScenePage(
                        nav = scene,
                        onBackClick = {
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(sceneNav = SceneNav.None)
                            }
                        },
                        onSceneSelected = { selectedScene ->
                            AppPageStateManager.updateState { currentState ->
                                currentState.copy(sceneNav = selectedScene)
                            }
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

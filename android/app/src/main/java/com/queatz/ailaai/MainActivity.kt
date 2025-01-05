package com.queatz.ailaai

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavDeepLink
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import app.ailaai.api.ErrorBlock
import app.ailaai.api.groups
import app.ailaai.api.me
import app.ailaai.api.updateMe
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.cache.CacheKey
import com.queatz.ailaai.cache.cache
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.data.appDomains
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.copyToClipboard
import com.queatz.ailaai.extensions.invoke
import com.queatz.ailaai.extensions.isInstalledFromPlayStore
import com.queatz.ailaai.extensions.launchUrl
import com.queatz.ailaai.extensions.px
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.helpers.LifecycleEffect
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.helpers.StartEffect
import com.queatz.ailaai.helpers.StopEffect
import com.queatz.ailaai.item.InventoryScreen
import com.queatz.ailaai.item.MyItemsScreen
import com.queatz.ailaai.schedule.ReminderScreen
import com.queatz.ailaai.schedule.RemindersScreen
import com.queatz.ailaai.services.calls
import com.queatz.ailaai.services.connectivity
import com.queatz.ailaai.services.joins
import com.queatz.ailaai.services.mePresence
import com.queatz.ailaai.services.messages
import com.queatz.ailaai.services.push
import com.queatz.ailaai.services.saves
import com.queatz.ailaai.services.say
import com.queatz.ailaai.services.trading
import com.queatz.ailaai.slideshow.slideshow
import com.queatz.ailaai.ui.dialogs.ReleaseNotesDialog
import com.queatz.ailaai.ui.screens.CardScreen
import com.queatz.ailaai.ui.screens.ExploreScreen
import com.queatz.ailaai.ui.screens.FriendsScreen
import com.queatz.ailaai.ui.screens.GroupScreen
import com.queatz.ailaai.ui.screens.InitialScreen
import com.queatz.ailaai.ui.screens.LinkDeviceScreen
import com.queatz.ailaai.ui.screens.MeScreen
import com.queatz.ailaai.ui.screens.ProfileScreen
import com.queatz.ailaai.ui.screens.ScheduleScreen
import com.queatz.ailaai.ui.screens.SettingsScreen
import com.queatz.ailaai.ui.screens.WelcomeDialog
import com.queatz.ailaai.ui.stickers.StickerPackEditorScreen
import com.queatz.ailaai.ui.stickers.StickerPackScreen
import com.queatz.ailaai.ui.stickers.StickerPacksScreen
import com.queatz.ailaai.ui.story.MyStoriesScreen
import com.queatz.ailaai.ui.story.StoriesScreen
import com.queatz.ailaai.ui.story.StoryCreatorScreen
import com.queatz.ailaai.ui.story.StoryScreen
import com.queatz.ailaai.ui.story.StorySource
import com.queatz.ailaai.ui.theme.AiLaAiTheme
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.serialization.encodeToString
import kotlin.time.Duration.Companion.seconds

private val appTabKey = stringPreferencesKey("app.tab")
private val appVersionCodeKey = intPreferencesKey("app.versionCode")
private val appUiKey = stringPreferencesKey("app.ui")

private class Background(val url: String)

private val _background = MutableStateFlow<List<Background>>(emptyList())

val background = _background.map { it.lastOrNull()?.url }

@Composable
fun background(url: String?) {
    if (url != null) {
        val value = Background(url)
        var stopped by rememberStateOf(false)

        StopEffect {
            _background.update {
                it - value
            }
            stopped = true
        }

        StartEffect {
            if (stopped) {
                stopped = false
                _background.update {
                    it + value
                }
            }
        }

        DisposableEffect(url) {
            _background.update {
                it + value
            }
            onDispose {
                _background.update {
                    it - value
                }
            }
        }
    }
}

class MainActivity : AppCompatActivity() {
    private val menuItems by lazy {
        listOf(
            NavButton(AppNav.Messages, getString(R.string.groups), Icons.Outlined.Forum),
            NavButton(AppNav.Stories, getString(R.string.posts), Icons.Outlined.Newspaper),
            NavButton(AppNav.Explore, getString(R.string.map), Icons.Outlined.Map),
//            NavButton(AppNav.Inventory, getString(R.string.inventory), Icons.Outlined.Rocket, selectedIcon = Icons.Outlined.RocketLaunch),
            NavButton(AppNav.Schedule, getString(R.string.calendar), Icons.Outlined.CalendarMonth),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )

            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        } else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        super.onCreate(savedInstanceState)

        setContent {
            AiLaAiTheme {
                val navController = rememberNavController()
                push.navController = navController
                slideshow.navController = navController

                var newMessages by rememberStateOf(0)
                val presence by mePresence.rememberPresence()

                LifecycleEffect {
                    if (push.navController == navController) {
                        push.latestEvent = it
                    }
                }

                val windowInsetsController = WindowCompat.getInsetsController(window, LocalView.current)

                LaunchedEffect(Unit) {
                    slideshow.active.collectLatest {
                        if (it) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                        }
                    }
                }

                val context = LocalContext.current
                var startDestination by remember { mutableStateOf<String?>(null) }
                var startDestinationLoaded by rememberStateOf(false)

                // Save last route
                if (startDestinationLoaded) {
                    LaunchedEffect(Unit) {
                        navController.currentBackStackEntryFlow.collectLatest { entry ->
                            val route = entry.destination.route
                            if (route != null && menuItems.any { it.route.route == route }) {
                                context.dataStore.edit {
                                    it[appTabKey] = route
                                }
                            }
                        }
                    }
                } else {
                    LaunchedEffect(Unit) {
                        startDestination = context.dataStore.data.first()[appTabKey]
                        startDestinationLoaded = true
                    }
                }

                val slideshowActive by slideshow.active.collectAsState()
                val userIsInactive by slideshow.userIsInactive.collectAsState()

                val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
                val showNavigation = navController.currentBackStackEntryAsState()
                    .value
                    ?.destination
                    ?.route
                    ?.let {
                        it.startsWith("group/")
                                || it.startsWith("write/")
                                || it.startsWith("story/")
                                || it.startsWith("sticker-pack/")
                                || (it.startsWith("card/") && it.endsWith("/edit"))
                                || (it.startsWith("profile/") && it.endsWith("/edit"))
                                || it == "sticker-packs"
                    } != true && !(slideshowActive && userIsInactive)

                var known by remember { mutableStateOf(api.hasToken()) }
                var showReleaseNotes by rememberStateOf(false)
                var showWelcomeDialog by rememberStateOf(false)

                if (showReleaseNotes) {
                    ReleaseNotesDialog {
                        showReleaseNotes = false
                    }
                }

                if (!known) {
                    InitialScreen { isSignUp ->
                        known = true
                        showWelcomeDialog = isSignUp
                    }
                } else {
                    var me by remember { mutableStateOf<Person?>(cache.get(CacheKey.Me)) }
                    var showSignedOut by rememberStateOf(false)
                    val snackbarHostState = remember { SnackbarHostState() }
                    val scope = rememberCoroutineScope()
                    val downloadString = stringResource(R.string.download)
                    val seeWhatsNewString = stringResource(R.string.see_whats_new)
                    var appUi by rememberStateOf(AppUi())
                    var apiIsReachable by rememberStateOf(true)

                    LaunchedEffect(Unit) {
                        appUi = context.dataStore.data.first()[appUiKey]?.let {
                            runCatching {
                                json.decodeFromString<AppUi>(it)
                            }.getOrNull()
                        } ?: AppUi()
                    }

                    LaunchedEffect(appUi) {
                        context.dataStore.edit {
                            it[appUiKey] = json.encodeToString(appUi)
                        }
                    }

                    fun updateAppLanguage(me: Person?) {
                        val language = appLanguage
                        if (me != null && language != null && me.language != language) {
                            scope.launch {
                                api.updateMe(Person(language = language))
                            }
                        }
                    }

                    suspend fun loadMe(onError: ErrorBlock) {
                        api.me(onError = {
                            apiIsReachable = false
                            onError?.invoke(it)
                        }) {
                            apiIsReachable = true
                            me = it
                            cache.put(CacheKey.Me, me!!)
                            push.setMe(me!!.id!!)
                            calls.setMe(me!!.id!!)
                            updateAppLanguage(me)
                        }
                    }

                    LaunchedEffect(me) {
                        if (me == null) {
                            messages.clear()
                        } else {
                            api.groups(
                                onError = {
                                    messages.refresh(me, emptyList())
                                }
                            ) {
                                messages.refresh(me, it)
                            }
                            messages.new.collectLatest {
                                newMessages = it
                            }
                        }
                    }

                    LaunchedEffect(me) {
                        if (me != null) {
                            saves.reload()
                            joins.reload()
                            mePresence.reload()
                            trading.reload()
                        }
                    }

                    LaunchedEffect(me) {
                        me?.utcOffset?.let {
                            val utcOffset = TimeZone.currentSystemDefault().offsetAt(Clock.System.now()).totalSeconds / (60.0 * 60.0)
                            if (it != utcOffset) {
                                api.updateMe(Person(utcOffset = utcOffset))
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        while (me == null) try {
                            loadMe(onError = {
                                throw Exception("Error loading me")
                            })
                        } catch (ex: Exception) {
                            delay(5.seconds)
                        }
                    }

                    LaunchedEffect(Unit) {
                        api.onUnauthorized.collect {
                            showSignedOut = true
                        }
                    }

                    if (!isInstalledFromPlayStore) {
                        LaunchedEffect(Unit) {
                            try {
                                val versionInfo = api.latestAppVersionInfo()

                                if (versionInfo.versionCode > BuildConfig.VERSION_CODE) {
                                    if (snackbarHostState.showSnackbar(
                                            context.getString(
                                                R.string.version_x_available,
                                                versionInfo.versionName,
                                                BuildConfig.VERSION_NAME
                                            ),
                                            actionLabel = downloadString,
                                            withDismissAction = true
                                        ) == SnackbarResult.ActionPerformed
                                    ) {
                                        "$appDomain/ailaai-${versionInfo.versionName}.apk".launchUrl(context)
                                    }
                                }
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        try {
                            if (
                                context.dataStore.data.first()[appVersionCodeKey].let {
                                    it != null && it != BuildConfig.VERSION_CODE
                                }
                            ) {
                                context.dataStore.edit {
                                    it[appVersionCodeKey] = BuildConfig.VERSION_CODE
                                }
                                if (snackbarHostState.showSnackbar(
                                        context.getString(R.string.updated_to_x, BuildConfig.VERSION_NAME),
                                        actionLabel = seeWhatsNewString,
                                        withDismissAction = true
                                    ) == SnackbarResult.ActionPerformed
                                ) {
                                    showReleaseNotes = true
                                }
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }

                    ResumeEffect {
                        connectivity.refresh()
                    }

                    if (showSignedOut) {
                        AlertDialog(
                            {
                                showSignedOut = false
                            },
                            text = {
                                Text(getString(R.string.youve_been_signed_out))
                            },
                            confirmButton = {
                                TextButton(
                                    {
                                        known = false
                                        me = null
                                        cache.remove(CacheKey.Me)
                                        showSignedOut = false
                                    }
                                ) {
                                    Text(stringResource(R.string.sign_out))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    {
                                        showSignedOut = false
                                    }
                                ) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        )
                    }

                    // Navigation bar background color
                    WindowInsets.navigationBars.getBottom(LocalDensity.current).let { height ->
                        if (height > 0 && !isLandscape) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation))
                                        .background(
                                            verticalGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation),
                                                    MaterialTheme.colorScheme.tertiaryContainer
                                                )
                                            )
                                        )
                                        .height(height.px)
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                ) {}
                            }
                        }
                    }

                    Scaffold(
                        bottomBar = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                AnimatedVisibility(showNavigation && !isLandscape) {
                                    NavigationBar(
                                        modifier = Modifier.height(54.dp)
                                    ) {
                                        menuItems.forEach { item ->
                                            val selected = navController.currentDestination?.route == item.route.route
                                            NavigationBarItem(
                                                icon = {
                                                    Box {
                                                        Icon(
                                                            if (selected) item.selectedIcon ?: item.icon else item.icon,
                                                            contentDescription = null
                                                        )
                                                        // todo reusable icon IconAndCount
                                                        if (item.route == AppNav.Messages && newMessages > 0) {
                                                            Text(
                                                                newMessages.toString(),
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier
                                                                    .offset(2.pad, -.5f.pad)
                                                                    .align(Alignment.TopEnd)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                                    .padding(1.pad, .25f.pad)
                                                            )
                                                        }
                                                        if (item.route == AppNav.Stories && (presence?.unreadStoriesCount
                                                                ?: 0) > 0
                                                        ) {
                                                            // todo reusable icon IconAndCount
                                                            Text(
                                                                (presence?.unreadStoriesCount ?: 0).toString(),
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier
                                                                    .offset(2.pad, -.5f.pad)
                                                                    .align(Alignment.TopEnd)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                                    .padding(1.pad, .25f.pad)
                                                            )
                                                        }
                                                    }
                                                },
                                                alwaysShowLabel = appUi.showNavLabels,
                                                label = if (appUi.showNavLabels) {
                                                    { Text(item.text) }
                                                } else {
                                                    null
                                                },
                                                selected = selected,
                                                onClick = {
                                                    navController.popBackStack()
                                                    navController.appNavigate(item.route)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        modifier = Modifier.safePadding(showNavigation)
                    ) {
                        PermanentNavigationDrawer(
                            drawerContent = {
                                AnimatedVisibility(
                                    showNavigation && isLandscape,
                                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                                    exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                                ) {
                                    PermanentDrawerSheet(Modifier.width(240.dp)) {
                                        Spacer(Modifier.height(1.pad))
                                        menuItems.forEach { item ->
                                            NavigationDrawerItem(
                                                icon = { Icon(item.icon, contentDescription = null) },
                                                label = {
                                                    Row(
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                    ) {
                                                        Text(item.text)
                                                        if (item.route == AppNav.Messages && newMessages > 0) {
                                                            Text(
                                                                newMessages.toString(),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        if (item.route == AppNav.Stories && (presence?.unreadStoriesCount
                                                                ?: 0) > 0
                                                        ) {
                                                            Text(
                                                                (presence?.unreadStoriesCount ?: 0).toString(),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                },
                                                selected = navController.currentDestination?.route == item.route.route,
                                                onClick = {
                                                    navController.popBackStack()
                                                    navController.appNavigate(item.route)
                                                },
                                                modifier = Modifier.padding(horizontal = .5f.pad)
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .let {
                                        if (showNavigation) {
                                            it.consumeWindowInsets(PaddingValues(bottom = 54.dp))
                                        } else {
                                            it
                                        }
                                    }
                                    .imePadding()
                            ) {
                                val background by background.collectAsState(null)
                                var lastBackground by rememberStateOf(background)

                                LaunchedEffect(background) {
                                    lastBackground = background ?: lastBackground
                                }

                                Crossfade(background != null) { show ->
                                    if (show) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(lastBackground)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "",
                                            contentScale = ContentScale.Crop,
                                            alignment = Alignment.Center,
                                            modifier = Modifier
                                                .fillMaxSize()
                                        )
                                    }
                                }

                                if (startDestinationLoaded) {
                                    CompositionLocalProvider(
                                        LocalAppState provides AppState(me, navController, apiIsReachable)
                                    ) {
                                        if (showWelcomeDialog) {
                                            WelcomeDialog(
                                                {
                                                    showWelcomeDialog = false
                                                }
                                            ) {
                                                scope.launch {
                                                    api.me {
                                                        me = it
                                                        cache.put(CacheKey.Me, me!!)
                                                    }
                                                }
                                            }
                                        }

                                        NavHost(
                                            navController = navController,
                                            startDestination = startDestination ?: AppNav.Explore.route,
                                            modifier = Modifier
                                                .padding(it)
                                                .fillMaxSize()
                                                .safePadding(!showNavigation)
                                        ) {
                                            composable(
                                                "profile/{id}",
                                                deepLinks = listOf(navDeepLink {
                                                    uriPattern = "$appDomain/profile/{id}"
                                                })
                                            ) {
                                                ProfileScreen(it.arguments!!.getString("id")!!)
                                            }
                                            composable(
                                                "profile/{id}/edit"
                                            ) {
                                                StoryCreatorScreen(
                                                    StorySource.Profile(it.arguments!!.getString("id")!!)
                                                )
                                            }
                                            composable("explore") {
                                                ExploreScreen()
                                            }
                                            composable(
                                                "inventory",
                                                deepLinks = listOf(navDeepLink {
                                                    uriPattern = "$appDomain/inventory"
                                                }
                                                )
                                            ) {
                                                InventoryScreen()
                                            }
                                            composable("items") {
                                                MyItemsScreen()
                                            }
                                            composable("link-device/{token}") {
                                                LinkDeviceScreen(
                                                    it.arguments!!.getString("token")!!
                                                )
                                            }
                                            composable(
                                                "schedule",
                                                deepLinks = listOf(
                                                    navDeepLink {
                                                        uriPattern = "$appDomain/schedule"
                                                    }
                                                )
                                            ) {
                                                ScheduleScreen()
                                            }
                                            composable("reminders") {
                                                RemindersScreen()
                                            }
                                            composable("reminder/{id}") {
                                                ReminderScreen(it.arguments!!.getString("id")!!)
                                            }
                                            composable("stories") {
                                                StoriesScreen()
                                            }
                                            composable(
                                                "story/{id}",
                                                deepLinks = listOf(navDeepLink { uriPattern = "$appDomain/story/{id}?comment={comment}" })
                                            ) {
                                                StoryScreen(
                                                    it.arguments!!.getString("id")!!,
                                                    commentId = it.arguments!!.getString("comment")
                                                )
                                            }
                                            composable("write") {
                                                MyStoriesScreen()
                                            }
                                            composable("write/{id}") {
                                                StoryCreatorScreen(
                                                    StorySource.Story(it.arguments!!.getString("id")!!),
                                                )
                                            }
                                            composable(
                                                "card/{id}",
                                                deepLinks = deeplink("/page/{id}", "/card/{id}")
                                            ) {
                                                CardScreen(it.arguments!!.getString("id")!!)
                                            }
                                            composable(
                                                "card/{id}/edit"
                                            ) {
                                                StoryCreatorScreen(
                                                    StorySource.Card(it.arguments!!.getString("id")!!)
                                                )
                                            }
                                            composable("messages") {
                                                FriendsScreen()
                                            }
                                            composable(
                                                "group/{id}",
                                                deepLinks = deeplink("/group/{id}")
                                            ) {
                                                GroupScreen(it.arguments!!.getString("id")!!)
                                            }
                                            composable("me") {
                                                MeScreen()
                                            }
                                            composable("sticker-packs") {
                                                StickerPacksScreen()
                                            }
                                            composable("sticker-pack/{id}") {
                                                StickerPackScreen(
                                                    it.arguments!!.getString("id")!!
                                                )
                                            }
                                            composable("sticker-pack/{id}/edit") {
                                                StickerPackEditorScreen(
                                                    it.arguments!!.getString("id")!!
                                                )
                                            }
                                            composable("settings") {
                                                SettingsScreen(appUi, { appUi = it }) {
                                                    if (api.hasToken()) {
                                                        scope.launch {
                                                            loadMe(onError = {
                                                                snackbarHostState.showSnackbar(
                                                                    getString(R.string.cant_connect),
                                                                    withDismissAction = true
                                                                )
                                                            })
                                                        }
                                                    } else {
                                                        known = false
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                val say by say.rememberSays()
                                Crossfade(
                                    say != null,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter),
                                    label = ""
                                ) {
                                    var lastSay by rememberStateOf(say)
                                    LaunchedEffect(say) {
                                        if (say != null) {
                                            lastSay = say
                                        }
                                    }
                                    when (it) {
                                        true -> {
                                            Text(
                                                lastSay ?: "",
                                                modifier = Modifier
                                                    .padding(
                                                        horizontal = 3.pad,
                                                        vertical = 1.pad * (isLandscape { 6 } ?: 12)
                                                    )
                                                    .shadow(1.elevation, MaterialTheme.shapes.large)
                                                    .clip(MaterialTheme.shapes.large)
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .clickable {
                                                        (lastSay ?: "").copyToClipboard(context)
                                                        context.toast(R.string.copied)
                                                    }
                                                    .padding(
                                                        horizontal = 2.pad,
                                                        vertical = 1.pad
                                                    )
                                            )
                                        }

                                        false -> {
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onUserInteraction() {
        slideshow.onUserInteraction()
    }
}

@Composable
private fun Modifier.safePadding(enabled: Boolean) = then(
    if (!enabled) Modifier else Modifier.windowInsetsPadding(
        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
    )
)

private fun deeplink(vararg urls: String): List<NavDeepLink> = urls.flatMap { url ->
    appDomains.map { domain ->
        navDeepLink { uriPattern = "$domain$url" }
    }
}

data class NavButton(
    val route: AppNav,
    val text: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector? = null
)

package com.queatz.ailaai

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.helpers.LifecycleEffect
import com.queatz.ailaai.helpers.StartEffect
import com.queatz.ailaai.helpers.StopEffect
import com.queatz.ailaai.schedule.ReminderScreen
import com.queatz.ailaai.schedule.RemindersScreen
import com.queatz.ailaai.services.*
import com.queatz.ailaai.ui.dialogs.ReleaseNotesDialog
import com.queatz.ailaai.ui.screens.*
import com.queatz.ailaai.ui.stickers.StickerPackEditorScreen
import com.queatz.ailaai.ui.stickers.StickerPackScreen
import com.queatz.ailaai.ui.stickers.StickerPacksScreen
import com.queatz.ailaai.ui.story.*
import com.queatz.ailaai.ui.theme.AiLaAiTheme
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private val appTabKey = stringPreferencesKey("app.tab")
private val appVersionCode = intPreferencesKey("app.versionCode")

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

@OptIn(ExperimentalLayoutApi::class)
class MainActivity : AppCompatActivity() {
    private val menuItems by lazy {
        listOf(
            NavButton("messages", getString(R.string.talk), Icons.Outlined.Group),
            NavButton("schedule", getString(R.string.reminders), Icons.Outlined.Schedule),
            NavButton("explore", getString(R.string.cards), Icons.Outlined.Home),
            NavButton("stories", getString(R.string.explore), Icons.Outlined.Explore),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AiLaAiTheme {
                val navController = rememberNavController()
                push.navController = navController

                var newMessages by remember { mutableStateOf(0) }
                val presence by mePresence.rememberPresence()

                LifecycleEffect {
                    if (push.navController == navController) {
                        push.latestEvent = it
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
                            if (route != null && menuItems.any { it.route == route }) {
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

                val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
                val showNavigation = navController.currentBackStackEntryAsState()
                    .value
                    ?.destination
                    ?.route
                    ?.let {
                        it.startsWith("group/")
                                || it.startsWith("write/")
                                || it.startsWith("sticker-pack/")
                                || (it.startsWith("card/") && it.endsWith("/edit"))
                                || (it.startsWith("profile/") && it.endsWith("/edit"))
                                || it == "sticker-packs"
                    } != true

                var known by remember { mutableStateOf(api.hasToken()) }
                var wasKnown by remember { mutableStateOf(known) }
                var showReleaseNotes by rememberStateOf(false)

                if (showReleaseNotes) {
                    ReleaseNotesDialog {
                        showReleaseNotes = false
                    }
                }

                if (!known) {
                    InitialScreen { known = true }
                } else {
                    var me by remember { mutableStateOf<Person?>(null) }
                    var showSignedOut by rememberStateOf(false)
                    val snackbarHostState = remember { SnackbarHostState() }
                    val scope = rememberCoroutineScope()
                    val cantConnectString = stringResource(R.string.cant_connect)
                    val downloadString = stringResource(R.string.download)
                    val seeWhatsNewString = stringResource(R.string.see_whats_new)

//                    window.setSoftInputMode(if (isLandscape) SOFT_INPUT_ADJUST_PAN else SOFT_INPUT_ADJUST_RESIZE)

                    fun updateAppLanguage(me: Person?) {
                        val language = appLanguage
                        if (me != null && language != null && me.language != language) {
                            scope.launch {
                                api.updateMe(Person(language = language))
                            }
                        }
                    }

                    suspend fun loadMe(onError: ErrorBlock) {
                        api.me(onError = onError) {
                            me = it
                            push.setMe(me!!.id!!)
                            updateAppLanguage(me)
                            if (!wasKnown) {
                                navController.navigate("profile/${me!!.id!!}")
                                wasKnown = true
                            }
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
                        }
                    }

                    LaunchedEffect(Unit) {
                        while (me == null) try {
                            loadMe(onError = {
                                throw Exception("Error loading me")
                            })
                        } catch (ex: Exception) {
                            snackbarHostState.showSnackbar(cantConnectString, withDismissAction = true)
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
                                context.dataStore.data.first()[appVersionCode].let {
                                    it != null && it != BuildConfig.VERSION_CODE
                                }
                            ) {
                                context.dataStore.edit {
                                    it[appVersionCode] = BuildConfig.VERSION_CODE
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
                                            NavigationBarItem(
                                                icon = {
                                                    Box {
                                                        Icon(item.icon, contentDescription = null)
                                                        // todo reusable icon IconAndCount
                                                        if (item.route == "messages" && newMessages > 0) {
                                                            Text(
                                                                newMessages.toString(),
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier
                                                                    .offset(2.pad, -2.pad)
                                                                    .align(Alignment.TopEnd)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                                    .padding(1.pad, .25f.pad)
                                                            )
                                                        }
                                                        if (item.route == "stories" && (presence?.unreadStoriesCount
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
                                                alwaysShowLabel = false,
                                                label = null,
                                                selected = navController.currentDestination?.route == item.route,
                                                onClick = {
                                                    navController.popBackStack()
                                                    navController.navigate(item.route)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                                                        if (item.route == "messages" && newMessages > 0) {
                                                            Text(
                                                                newMessages.toString(),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        if (item.route == "stories" && (presence?.unreadStoriesCount
                                                                ?: 0) > 0
                                                        ) {
                                                            Text(
                                                                (presence?.unreadStoriesCount ?: 0).toString(),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                },
                                                selected = navController.currentDestination?.route == item.route,
                                                onClick = {
                                                    navController.popBackStack()
                                                    navController.navigate(item.route)
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
                                        LocalAppState provides AppState(me, navController)
                                    ) {
                                        NavHost(
                                            navController,
                                            startDestination ?: "explore",
                                            modifier = Modifier
                                                .padding(it)
                                                .fillMaxSize()
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
                                                deepLinks = listOf(navDeepLink { uriPattern = "$appDomain/story/{id}" })
                                            ) {
                                                StoryScreen(it.arguments!!.getString("id")!!)
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
                                                deepLinks = listOf(
                                                    navDeepLink { uriPattern = "$appDomain/page/{id}" },
                                                    navDeepLink { uriPattern = "$appDomain/card/{id}" })
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
                                                deepLinks = listOf(navDeepLink { uriPattern = "$appDomain/group/{id}" })
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
                                                SettingsScreen {
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
}

data class NavButton(
    val route: String,
    val text: String,
    val icon: ImageVector,
)

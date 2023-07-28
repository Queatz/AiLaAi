package com.queatz.ailaai

import android.location.Location
import android.os.Bundle
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.api.groups
import com.queatz.ailaai.api.me
import com.queatz.ailaai.api.updateMe
import com.queatz.ailaai.data.ErrorBlock
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.services.*
import com.queatz.ailaai.ui.dialogs.ReleaseNotesDialog
import com.queatz.ailaai.ui.screens.*
import com.queatz.ailaai.ui.stickers.StickerPackEditorScreen
import com.queatz.ailaai.ui.stickers.StickerPackScreen
import com.queatz.ailaai.ui.stickers.StickerPacksScreen
import com.queatz.ailaai.ui.story.MyStoriesScreen
import com.queatz.ailaai.ui.story.StoriesScreen
import com.queatz.ailaai.ui.story.StoryCreatorScreen
import com.queatz.ailaai.ui.story.StoryScreen
import com.queatz.ailaai.ui.theme.AiLaAiTheme
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private val appTabKey = stringPreferencesKey("app.tab")
private val appVersionCode = intPreferencesKey("app.versionCode")

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavHostController

    private val menuItems by lazy {
        listOf(
            NavButton("messages", getString(R.string.talk), Icons.Outlined.People),
//            NavButton("schedule", getString(R.string.explore), Icons.Outlined.Event),
            NavButton("explore", getString(R.string.explore), Icons.Outlined.Style),
            NavButton("stories", getString(R.string.stories), Icons.Outlined.EventNote),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AiLaAiTheme {
                navController = rememberNavController()
                push.navController = navController

                var newMessages by remember { mutableStateOf(0) }
                val presence by mePresence.rememberPresence()

                OnLifecycleEvent {
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

                    window.setSoftInputMode(if (showNavigation || isLandscape) SOFT_INPUT_ADJUST_PAN else SOFT_INPUT_ADJUST_RESIZE)

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
                        mePresence.reload()
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
                            if (context.dataStore.data.first()[appVersionCode] != BuildConfig.VERSION_CODE) {
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

                    LaunchedEffect(me) {
                        saves.reload()
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
                                                        if (item.route == "messages" && newMessages > 0) {
                                                            Text(
                                                                newMessages.toString(),
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier
                                                                    .offset(PaddingDefault * 2, -PaddingDefault / 2)
                                                                    .align(Alignment.TopEnd)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                                    .padding(PaddingDefault, PaddingDefault / 4)
                                                            )
                                                        }
                                                        if (item.route == "stories" && (presence?.unreadStoriesCount ?: 0) > 0
                                                        ) {
                                                            // todo reusable icon
                                                            Text(
                                                                (presence?.unreadStoriesCount ?: 0).toString(),
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier
                                                                    .offset(PaddingDefault * 2, -PaddingDefault / 2)
                                                                    .align(Alignment.TopEnd)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                                    .padding(PaddingDefault, PaddingDefault / 4)
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
                                        Spacer(Modifier.height(PaddingDefault))
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
                                                modifier = Modifier.padding(horizontal = PaddingDefault / 2)
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (startDestinationLoaded) {
                                    NavHost(
                                        navController,
                                        startDestination ?: "explore",
                                        modifier = Modifier
                                            .padding(it)
                                            .fillMaxSize()
                                    ) {
                                        composable(
                                            "profile/{id}",
                                            deepLinks = listOf(navDeepLink { uriPattern = "$appDomain/profile/{id}" })
                                        ) {
                                            ProfileScreen(it.arguments!!.getString("id")!!, navController) { me }
                                        }
                                        composable("explore") {
                                            ExploreScreen(navController) { me }
                                        }
                                        composable("schedule") {
                                            MapScreen(navController) { me }
                                        }
                                        composable("map") {
                                            MapScreen(navController) { me }
                                        }
                                        composable("stories") {
                                            StoriesScreen(navController) { me }
                                        }
                                        composable(
                                            "story/{id}",
                                            deepLinks = listOf(navDeepLink { uriPattern = "$appDomain/story/{id}" })
                                        ) {
                                            StoryScreen(it.arguments!!.getString("id")!!, navController) { me }
                                        }
                                        composable("write") {
                                            MyStoriesScreen(navController) { me }
                                        }
                                        composable("write/{id}") {
                                            StoryCreatorScreen(it.arguments!!.getString("id")!!, navController) { me }
                                        }
                                        composable("saved") {
                                            SavedScreen(navController) { me }
                                        }
                                        composable(
                                            "card/{id}",
                                            deepLinks = listOf(navDeepLink { uriPattern = "$appDomain/card/{id}" })
                                        ) {
                                            CardScreen(it.arguments!!.getString("id")!!, navController) { me }
                                        }
                                        composable("messages") {
                                            FriendsScreen(navController) { me }
                                        }
                                        composable(
                                            "group/{id}",
                                            deepLinks = listOf(navDeepLink { uriPattern = "$appDomain/group/{id}" })
                                        ) {
                                            GroupScreen(it.arguments!!.getString("id")!!, navController) { me }
                                        }
                                        composable("me") {
                                            MeScreen(navController) { me }
                                        }
                                        composable("sticker-packs") {
                                            StickerPacksScreen(navController) { me }
                                        }
                                        composable("sticker-pack/{id}") {
                                            StickerPackScreen(navController, it.arguments!!.getString("id")!!) { me }
                                        }
                                        composable("sticker-pack/{id}/edit") {
                                            StickerPackEditorScreen(
                                                navController,
                                                it.arguments!!.getString("id")!!
                                            ) { me }
                                        }
                                        composable("settings") {
                                            SettingsScreen(navController, { me }) {
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
                                val say by say.rememberSays()
                                Crossfade(
                                    say != null,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
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
                                                        horizontal = PaddingDefault * 3,
                                                        vertical = PaddingDefault * (isLandscape { 6 } ?: 12)
                                                    )
                                                    .shadow(ElevationDefault, MaterialTheme.shapes.large)
                                                    .clip(MaterialTheme.shapes.large)
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .clickable {
                                                        (lastSay ?: "").copyToClipboard(context)
                                                        context.toast(R.string.copied)
                                                    }
                                                    .padding(
                                                        horizontal = PaddingDefault * 2,
                                                        vertical = PaddingDefault
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

@Composable
fun OnLifecycleEvent(onEvent: suspend (event: Lifecycle.Event) -> Unit) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            scope.launch {
                eventHandler.value(event)
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

fun Location.toLatLng() = LatLng(latitude, longitude)

data class NavButton(
    val route: String,
    val text: String,
    val icon: ImageVector,
)

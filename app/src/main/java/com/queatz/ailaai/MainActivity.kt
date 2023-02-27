package com.queatz.ailaai

import android.location.Location
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.extensions.launchUrl
import com.queatz.ailaai.ui.screens.*
import com.queatz.ailaai.ui.theme.AiLaAiTheme
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AiLaAiTheme {
                navController = rememberNavController()
                push.navController = navController

                OnLifecycleEvent {
                    if (push.navController == navController) {
                        push.latestEvent = it
                    }
                }

                val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
                val showNavigation = navController.currentBackStackEntryAsState()
                    .value
                    ?.destination
                    ?.route
                    ?.let { it.startsWith("group/") || it.startsWith("card/") } != true

                var known by remember { mutableStateOf(api.hasToken()) }

                if (!known) {
                    InitialScreen { known = true }
                } else {
                    var me by remember { mutableStateOf<Person?>(null) }
                    val snackbarHostState = remember { SnackbarHostState() }
                    val coroutineScope = rememberCoroutineScope()
                    val cantConnectString = stringResource(R.string.cant_connect)
                    val updateAvailableString = stringResource(R.string.update_available)
                    val downloadString = stringResource(R.string.download)
                    val context = LocalContext.current

                    LaunchedEffect(true) {
                        while (me == null) try {
                            me = api.me()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            snackbarHostState.showSnackbar(cantConnectString, withDismissAction = true)
                            delay(5.seconds)
                        }
                    }

                    LaunchedEffect(Unit) {
                        api.onUnauthorized.collect {
                            known = false
                            me = null
                        }
                    }

                    LaunchedEffect(true) {
                        try {
                            val version = api.latestAppVersion() ?: -1

                            if (version > BuildConfig.VERSION_CODE) {
                                if (snackbarHostState.showSnackbar(
                                        updateAvailableString,
                                        actionLabel = downloadString,
                                        withDismissAction = true
                                    ) == SnackbarResult.ActionPerformed
                                ) {
                                    appDomain.launchUrl(context)
                                }
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }

                    LaunchedEffect(me) {
                        saves.reload()
                    }

                    Scaffold(
                        bottomBar = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                AnimatedVisibility(showNavigation && !isLandscape) {
                                    NavigationBar {
                                        listOf(
                                            NavButton("explore", getString(R.string.explore), Icons.Outlined.Search),
                                            NavButton("saved", getString(R.string.saved), Icons.Outlined.FavoriteBorder),
                                            NavButton("messages", getString(R.string.conversations), Icons.Outlined.Email),
                                            NavButton("me", getString(R.string.me), Icons.Outlined.Person)
                                        ).forEach { item ->
                                            NavigationBarItem(
                                                icon = { Icon(item.icon, contentDescription = null) },
                                                label = { Text(item.text) },
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
                                        listOf(
                                            NavButton("explore", getString(R.string.explore), Icons.Outlined.Search),
                                            NavButton("saved", getString(R.string.saved), Icons.Outlined.FavoriteBorder),
                                            NavButton("messages", getString(R.string.conversations), Icons.Outlined.Email),
                                            NavButton("me", getString(R.string.me), Icons.Outlined.Person)
                                        ).forEach { item ->
                                            NavigationDrawerItem(
                                                icon = { Icon(item.icon, contentDescription = null) },
                                                label = { Text(item.text) },
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
                            }, modifier = Modifier.fillMaxSize()
                        ) {
                            NavHost(
                                navController,
                                "explore",
                                modifier = Modifier
                                    .padding(it)
                                    .fillMaxSize()
                            ) {
                                composable("explore") { ExploreScreen(this@MainActivity, navController) { me } }
                                composable("saved") { SavedScreen(this@MainActivity, navController) { me } }
                                composable(
                                    "card/{id}",
                                    deepLinks = listOf(navDeepLink { uriPattern = "${appDomain}/card/{id}" })
                                ) { CardScreen(it, navController) { me } }
                                composable("messages") { MessagesScreen(navController) { me } }
                                composable(
                                    "group/{id}",
                                    deepLinks = listOf(navDeepLink { uriPattern = "${appDomain}/group/{id}" })
                                ) { GroupScreen(it, navController) { me } }
                                composable("me") { MeScreen(navController) { me } }
                                composable("settings") {
                                    SettingsScreen(navController, { me }) {
                                        if (api.hasToken()) {
                                            coroutineScope.launch {
                                                try {
                                                    me = api.me()
                                                } catch (ex: Exception) {
                                                    ex.printStackTrace()
                                                    snackbarHostState.showSnackbar(
                                                        getString(R.string.cant_connect),
                                                        withDismissAction = true
                                                    )
                                                }
                                            }
                                        } else {
                                            known = false
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
fun OnLifecycleEvent(onEvent: (event: Lifecycle.Event) -> Unit) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { owner, event ->
            eventHandler.value(event)
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
    val icon: ImageVector
)

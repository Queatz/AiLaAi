import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.AppStyles
import app.appNav
import app.call.CallLayout
import app.call.CallStyles
import app.components.Background
import app.compose.rememberMobileMode
import app.game.GameCoverPage
import app.group.GroupCoverPage
import app.info.PrivacyPage
import app.info.TosPage
import app.invites.InvitePage
import app.scripts.ScriptCoverPage
import app.scripts.ScriptSourcePage
import app.softwork.routingcompose.BrowserRouter
import app.softwork.routingcompose.Router
import app.theme.ThemeManager
import app.theme.ThemeSettingsPage
import app.widget.WidgetStyles
import components.AppFooter
import components.AppHeader
import components.CardPage
import components.IconButton
import components.InfoPage
import components.NotificationsLayout
import components.ProfilePage
import components.SigninPage
import components.StoryPage
import event.EventPage
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import lib.mapboxgl
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposableInBody
import org.w3c.dom.HTMLLinkElement
import org.w3c.dom.get
import org.w3c.dom.set
import stories.StoryStyles

const val baseUrl = "http://0.0.0.0:8080"
//const val baseUrl = "https://api.ailaai.app"
//
const val webBaseUrl = "http://0.0.0.0:4040"
//const val webBaseUrl = "https://hitown.chat"

val json = Json {
    encodeDefaults = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    ignoreUnknownKeys = true
    explicitNulls = false
}

val http = HttpClient(Js) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(json)
    }
}

fun main() {
    // Mapbox
    js("require(\"mapbox-gl/dist/mapbox-gl.css\")")

    mapboxgl.accessToken =
        "pk.eyJ1IjoiamFjb2JmZXJyZXJvIiwiYSI6ImNraXdyY211eTBlMmcycW02eDNubWNpZzcifQ.1KtSoMzrPCM0A8UVtI_gdg"

    val savedTheme = ThemeManager.getCurrentTheme()
    if (savedTheme != null) {
        StyleManager.setTheme(savedTheme)
    }

    renderComposableInBody {
        StyleManager.use(
            MainStyleSheet::class,
            AppStyles::class,
            EffectStyles::class,
            WidgetStyles::class,
            CallStyles::class,
            StoryStyles::class
        )

        var language by remember {
            mutableStateOf(
                when {
                    (localStorage["language"] ?: window.navigator.language).startsWith("vi") -> "vi"
                    (localStorage["language"] ?: window.navigator.language).startsWith("ru") -> "ru"
                    else -> "en"
                }
            )
        }

        CompositionLocalProvider(LocalConfiguration provides Configuration(language) { language = it }) {
            var title by remember { mutableStateOf<String?>(null) }
            var parentCardId by remember { mutableStateOf<String?>(null) }
            var personId by remember { mutableStateOf<String?>(null) }
            val appName = appString { appName }

            LaunchedEffect(title) {
                document.title = title ?: appName
            }

            LaunchedEffect(language) {
                localStorage["language"] = language
                application.language = language
            }

            LaunchedEffect(Unit) {
                indicator.hasIndicator.collectLatest {
                    val faviconElement = document.querySelector("link[rel*='icon']") as HTMLLinkElement

                    faviconElement.href = if (it) "/icon-new.png" else "/icon.png"
                }
            }

            LaunchedEffect(Unit) {
                delay(500)
                application.sync()
                push.start(this)
                saves.start(this)
                joins.start(this)
                call.init(this)
            }

            BrowserRouter("") {
                val router = Router.current

                LaunchedEffect(Unit) {
                    appNav.route.collectLatest {
                        router.navigate(it)
                    }
                }

                LaunchedEffect(router.currentPath) {
                    window.scrollTo(0.0, 0.0)
                    document.title = appName
                }

                route("signin") {
                    AppHeader(
                        title = appString { signIn },
                        showBack = true,
                        onBack = {
                            router.navigate("/")
                        },
                        showMe = false
                    )
                    SigninPage()
                    AppFooter()
                }

                route("group") {
                    string { groupId ->
                        Background({
                            classes(Styles.background)
                        }) {
                            AppHeader(appName)
                            GroupCoverPage(groupId) {
                                title = it.group?.name ?: appName
                            }
                            AppFooter()
                        }
                    }

                    noMatch {
                        router.navigate("/")
                    }
                }

                route("invite") {
                    string { code ->
                        AppHeader(
                            title = appName,
                            showMe = true
                        )
                        InvitePage(code = code)
                        AppFooter()
                    }
                }

                route("script") {
                    string { script ->
                        Background({
                            classes(Styles.background)
                        }) {
                            AppHeader(
                                title = "",
                                background = false,
                                customContent = {
                                    val isMobile = rememberMobileMode()

                                    if (isMobile) {
                                        // Use IconButton on mobile
                                        IconButton(
                                            name = "code",
                                            title = appString { viewSource },
                                            outlined = true,
                                            onClick = {
                                                window.open("/script-source/$script", "_blank")
                                            }
                                        )
                                    } else {
                                        // Use regular Button on desktop
                                        Button({
                                            classes(Styles.outlineButton)
                                            style {
                                                marginRight(1.r)
                                            }
                                            onClick {
                                                window.open("/script-source/$script", "_blank")
                                            }
                                        }) {
                                            Span({
                                                classes("material-symbols-outlined")
                                            }) {
                                                Text("code")
                                            }
                                            Text(" ${appString { viewSource }}")
                                        }
                                    }
                                }
                            )
                            ScriptCoverPage(scriptId = script)
                            AppFooter(
                                showHome = true
                            )
                        }
                    }
                }

                route("script-source") {
                    string { script ->
                        Background({
                            classes(Styles.background)
                        }) {
                            AppHeader(
                                title = appString { scriptSource },
                                showBack = true,
                                onBack = {
                                    router.navigate("/script/$script")
                                }
                            )
                            ScriptSourcePage(scriptId = script)
                            AppFooter(
                                showHome = true
                            )
                        }
                    }
                }

                route("page") {
                    string { cardIdOrUrl ->
                        Background({
                            classes(Styles.background)
                        }) {
                            val me by application.me.collectAsState()
                            AppHeader(
                                title = appName,
                                showBack = parentCardId != null || personId != null,
                                showDownloadApp = me == null,
                                onBack = {
                                    if (parentCardId != null) {
                                        router.navigate("/page/$parentCardId")
                                    } else if (personId != null) {
                                        router.navigate("/profile/$personId")
                                    }
                                }
                            )
                            CardPage(
                                url = cardIdOrUrl,
                                onError = { parentCardId = null }
                            ) {
                                title = it.name
                                parentCardId = it.parent
                                personId = if (it.equipped == true) it.person else null
                            }
                            AppFooter()
                        }
                    }

                    noMatch {
                        router.navigate("/")
                    }
                }

                route("story") {
                    string { storyUrl ->

                        Background({
                            classes(Styles.background)
                        }) {
                            AppHeader(appString { stories })
                            StoryPage(storyUrl) {
                                title = it.title
                            }
                            AppFooter()
                        }
                    }

                    noMatch {
                        router.navigate("/")
                    }
                }

                route("profile") {
                    string { profileUrl ->

                        Background({
                            classes(Styles.background)
                        }) {
                            AppHeader(appName)
                            val someoneString = appString { someone }
                            ProfilePage(profileUrl) {
                                title = it.person.name ?: someoneString
                            }
                            AppFooter()
                        }
                    }

                    noMatch {
                        router.navigate("/")
                    }
                }

                route("info") {
                    string { page ->
                        AppHeader(appName, showBack = true) {
                            router.navigate("/")
                        }
                        InfoPage(page)
                        AppFooter()
                    }
                }

                route("privacy") {
                    AppHeader(appName)
                    PrivacyPage()
                    AppFooter()
                }

                route("terms") {
                    AppHeader(appName)
                    TosPage()
                    AppFooter()
                }

                route("theme") {
                    AppHeader(
                        title = "Theme",
                        showBack = true,
                        onBack = {
                            router.navigate("/")
                        }
                    )
                    ThemeSettingsPage()
                    AppFooter()
                }

                route("event") {
                    string { eventId ->
                        AppHeader(appName)
                        EventPage(eventId)
                        AppFooter()
                    }
                }

                route("scene") {
                    string { sceneId ->
                        GameCoverPage(sceneId)
                    }
                }

                string { profileUrl ->
                    Background({
                        classes(Styles.background)
                    }) {
                        AppHeader(appName)
                        val someoneString = appString { someone }
                        ProfilePage(url = profileUrl) {
                            title = it.person.name ?: someoneString
                        }
                        AppFooter()
                    }
                }

                noMatch {
                    MainPage()
                }
            }

            val activeCall by call.active.collectAsState()

            if (activeCall != null) {
                CallLayout(activeCall!!)
            }

            NotificationsLayout()
        }
    }
}

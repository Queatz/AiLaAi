package components

import AppLayout
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
import app.AppNavigation
import app.AppStyles
import app.ailaai.api.activeCardsOfPerson
import app.ailaai.api.createGroup
import app.ailaai.api.groupsOfPerson
import app.ailaai.api.profile
import app.ailaai.api.profileByUrl
import app.ailaai.api.profileCards
import app.appNav
import app.components.Empty
import app.components.TopBarSearch
import app.dialog.photoDialog
import app.group.GroupList
import app.softwork.routingcompose.Router
import appString
import application
import baseUrl
import com.queatz.db.Card
import com.queatz.db.GroupExtended
import com.queatz.db.PersonProfile
import format
import kotlinx.coroutines.launch
import mainContent
import notBlank
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.backgroundPosition
import org.jetbrains.compose.web.css.backgroundSize
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.minHeight
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.transform
import org.jetbrains.compose.web.css.unaryMinus
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.vw
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Source
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Video
import org.w3c.dom.HTMLVideoElement
import profile.ProfileStyles
import r
import webBaseUrl
import kotlin.js.Date

@OptIn(ExperimentalComposeWebApi::class)
@Composable
fun ProfilePage(personId: String? = null, url: String? = null, onProfile: (PersonProfile) -> Unit) {
    Style(ProfileStyles)
    Style(AppStyles)

    var personId by remember { mutableStateOf(personId) }
    val scope = rememberCoroutineScope()
    val router = Router.current
    val appNav = appNav
    var profile by remember { mutableStateOf<PersonProfile?>(null) }
    var cards by remember { mutableStateOf<List<Card>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val me by application.me.collectAsState()
    val layout by application.layout.collectAsState()

    var search by remember {
        mutableStateOf("")
    }

    if (personId == null && url == null) {
        router.navigate("/")
        return
    }

    application.background(profile?.profile?.background?.let { "$baseUrl$it" })

    LaunchedEffect(Unit) {
        isLoading = true
        if (personId != null) {
            api.profile(
                personId!!,
                onError = {
                    // todo show not found page
                    router.navigate("/")
                }
            ) {
                profile = it
                onProfile(profile!!)

                if (personId == null) {
                    personId = profile!!.person.id
                }
            }
        } else {
            api.profileByUrl(
                url!!,
                onError = {
                    // todo show not found page
                    router.navigate("/")
                }
            ) {
                profile = it
                onProfile(profile!!)

                if (personId == null) {
                    personId = profile!!.person.id
                }
            }
        }
        isLoading = false
    }

    LaunchedEffect(personId, search) {
        if (personId != null) {
            if (search.isBlank()) {
                api.profileCards(personId!!) {
                    cards = it
                }
            } else {
                api.activeCardsOfPerson(personId!!, search) {
                    cards = it
                }
            }
        }
    }

    if (layout == AppLayout.Kiosk) {
        QrImg("$webBaseUrl/profile/$personId") {
            position(Position.Fixed)
            bottom(2.r)
            left(2.r)
            maxWidth(10.vw)
            maxHeight(10.vw)
            transform {
                scale(2)
                translate(25.percent, -25.percent)
            }
        }
    }

    if (!isLoading && profile == null) {
        Div({
            mainContent(layout)
            style {
                display(DisplayStyle.Flex)
                minHeight(100.vh)
                width(100.percent)
                flexDirection(FlexDirection.Column)
                padding(2.r)
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.FlexStart)
            }
        }) {
            Text(appString { profileNotFound })
        }
    } else {
        application.layout.collectAsState().value
        profile?.let { profile ->
            Div({
                mainContent(layout)
            }) {
                Div({
                    classes(Styles.navContainer)
                }) {
                    Div({
                        classes(Styles.navContent)
                    }) {
                        profile.profile.photo?.let { // photo or video
                            val url = "$baseUrl$it"
                            Div({
                                style {
                                    width(100.percent)
                                    backgroundColor(Styles.colors.background)
                                    backgroundImage("url($url)")
                                    backgroundPosition("center")
                                    backgroundSize("cover")
                                    maxHeight(50.vh)
                                    cursor("pointer")
                                    property("aspect-ratio", "2")
                                }

                                onClick {
                                    scope.launch {
                                        photoDialog(url)
                                    }
                                }
                            }) {}
                        } ?: profile.profile.video?.let {
                            var videoElement by remember { mutableStateOf<HTMLVideoElement?>(null) }
//                            LaunchedEffect(videoElement) {
//                                if (videoElement != null) {
//                                    delay(250)
//                                    try {
////                                        if (window.navigator.getAutoplayPolicy)
//                                        videoElement!!.muted = false
//                                    } catch (e: Throwable) {
//                                        // ignore
//                                    }
//                                }
//                            }
                            Video({
                                attr("autoplay", "")
                                attr("loop", "")
                                attr("playsinline", "")
                                attr("muted", "")
                                style {
                                    property("object-fit", "cover")
                                    width(100.percent)
                                    backgroundColor(Styles.colors.background)
                                    property("aspect-ratio", "2")
                                }
                                onClick {
                                    (it.target as? HTMLVideoElement)?.apply {
                                        play()
                                        muted = false
                                    }
                                }
                                // Do this so that auto-play works on page load, but unmute on page navigation
                                ref { videoEl ->
                                    videoEl.onloadedmetadata = {
                                        videoEl.muted = true
                                        videoElement = videoEl
                                        it
                                    }
                                    onDispose { }
                                }
                            }) {
                                Source({
                                    attr("src", "$baseUrl$it")
                                    attr("type", "video/webm")
                                })
                            }
                        }
                        Div({
                            classes(ProfileStyles.mainContent)
                        }) {
                            profile.person.photo?.let {
                                val url = "$baseUrl$it"
                                Div({
                                    if (profile.profile.photo == null && profile.profile.video == null) {
                                        classes(ProfileStyles.photo, ProfileStyles.nophoto)
                                    } else {
                                        classes(ProfileStyles.photo)
                                    }
                                    style {
                                        backgroundImage("url($url)")
                                        cursor("pointer")
                                    }

                                    onClick {
                                        scope.launch {
                                            photoDialog(url)
                                        }
                                    }
                                }) {}
                            }
                            Div({
                                classes(Styles.cardContent, ProfileStyles.profileContent)
                            }) {
                                Div({
                                    classes(ProfileStyles.name)
                                }) {
                                    NameAndLocation(profile.person.name, profile.profile.location?.takeIf { layout == AppLayout.Kiosk })
                                }
                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                    }
                                }) {
                                    if (layout == AppLayout.Default) {
                                        if (me == null) {
                                            Button({
                                                classes(Styles.button)

                                                onClick {
                                                    router.navigate("/signin")
                                                }
                                            }) {
                                                Text(appString { connect })
                                            }
                                        } else {
                                            Button({
                                                classes(Styles.button)

                                                onClick {
                                                    scope.launch {
                                                        api.createGroup(listOf(personId!!), reuse = true) {
                                                            appNav.navigate(AppNavigation.Group(it.id!!))
                                                        }
                                                    }
                                                }
                                            }) {
                                                Text(appString { message })
                                            }
                                        }
                                    }
//                                    Button({
//                                        classes(Styles.button)
//                                    style {
//                                        marginLeft(1.r)
//                                    }
//                                    }) {
//                                        Text("Trade")
//                                    }
//                                    Button({
//                                        classes(Styles.button)
//                                    style {
//                                        marginLeft(1.r)
//                                    }
//                                    }) {
//                                        Text("Un/Subscribe")
//                                    }
                                }
                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                        alignItems(AlignItems.Stretch)
                                        flexWrap(FlexWrap.Wrap)
                                        width(100.percent)
                                    }
                                }) {
                                    Div({
                                        classes(ProfileStyles.infoCard)
                                    }) {
                                        Div({ classes(ProfileStyles.infoCardValue) }) { Text("${profile.stats.friendsCount}") }
                                        Div({ classes(ProfileStyles.infoCardName) }) { Text(appString { friends }) }
                                    }
                                    Div({
                                        classes(ProfileStyles.infoCard)
                                    }) {
                                        Div({ classes(ProfileStyles.infoCardValue) }) { Text("${profile.stats.cardCount}") }
                                        Div({ classes(ProfileStyles.infoCardName) }) { Text(appString { this.cards }) }
                                    }
                                    Div({
                                        classes(ProfileStyles.infoCard)
                                    }) {
                                        Div(
                                            {
                                                classes(ProfileStyles.infoCardValue)
                                                title("${Date(profile.person.createdAt!!.toEpochMilliseconds())}")
                                            }
                                        ) { Text("${Date(profile.person.createdAt!!.toEpochMilliseconds()).getFullYear()}") }
                                        Div({ classes(ProfileStyles.infoCardName) }) { Text(appString { joined }) }
                                    }
                                    Div({
                                        classes(ProfileStyles.infoCard)
                                    }) {
                                        Div({ classes(ProfileStyles.infoCardValue) }) { Text("${profile.stats.storiesCount}") }
                                        Div({ classes(ProfileStyles.infoCardName) }) { Text(appString { stories }) }
                                    }
                                    Div({
                                        classes(ProfileStyles.infoCard)
                                    }) {
                                        Div({ classes(ProfileStyles.infoCardValue) }) { Text("${profile.stats.subscriberCount}") }
                                        Div({ classes(ProfileStyles.infoCardName) }) { Text(appString { subscribers }) }
                                    }
                                }
                                if (profile.profile.about != null) {
                                    Div({
                                        classes(ProfileStyles.infoAbout)
                                    }) {
                                        LinkifyText(profile.profile.about!!)
                                    }
                                }

                                Content(profile.profile.content?.notBlank)

                                var groups by remember {
                                    mutableStateOf<List<GroupExtended>>(emptyList())
                                }

                                LaunchedEffect(personId) {
                                    api.groupsOfPerson(personId ?: return@LaunchedEffect) {
                                        groups = it
                                    }
                                }

                                if (groups.isNotEmpty()) {
                                    Span({
                                        style {
                                            marginTop(1.r)
                                            fontWeight("bold")
                                            fontSize(24.px)
                                        }
                                    }) {
                                        Text("${profile.person.name ?: appString { someone }} ${appString { inlineIsAMember }}")
                                    }
                                    Div({
                                        classes(AppStyles.groupList)
                                    }) {
                                        GroupList(groups, coverPhoto = true, onSurface = true, maxWidth = 32.r) {
                                                // todo navigate to group
                                            router.navigate("/signin")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                TopBarSearch(
                    search,
                    { search = it },
                    focus = false,
                    placeholder = appString { searchPagesOfPerson }.format(profile.person.name ?: appString { someone })
                ) {
                    marginTop(2.r)
                    marginLeft(1.r)
                    marginRight(1.r)
                }

                Div({
                    classes(Styles.content)
                }) {
                    if (search.isNotBlank() && cards.isEmpty()) {
                        Empty {
                            Text(appString { noCards })
                        }
                    } else {
                        cards.forEach { card ->
                            CardItem(card, styles = {
                                margin(1.r)
                            })
                        }
                    }
                }
            }
        }
    }
}

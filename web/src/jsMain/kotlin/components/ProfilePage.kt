package components

import Styles
import androidx.compose.runtime.*
import api
import app.AppStyles
import app.ailaai.api.*
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
import notBlank
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLVideoElement
import profile.ProfileStyles
import r
import kotlin.js.Date

@Composable
fun ProfilePage(personId: String? = null, url: String? = null, onProfile: (PersonProfile) -> Unit) {
    Style(ProfileStyles)
    Style(AppStyles)

    var personId by remember { mutableStateOf(personId) }
    val scope = rememberCoroutineScope()
    val router = Router.current
    var profile by remember { mutableStateOf<PersonProfile?>(null) }
    var cards by remember { mutableStateOf<List<Card>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val me by application.me.collectAsState()

    var search by remember {
        mutableStateOf("")
    }

    if (personId == null && url == null) {
        router.navigate("/")
        return
    }

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

    if (!isLoading && profile == null) {
        Div({
            classes(Styles.mainContent)
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
        profile?.let { profile ->
            Div({
                classes(Styles.mainContent)
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
                                    NameAndLocation(profile.person.name, "")
                                }
                                Div({
                                    style {
                                        display(DisplayStyle.Flex)
                                    }
                                }) {
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
                                                // todo go to specific group
                                                router.navigate("/")
                                            }
                                        }) {
                                            Text(appString { message })
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

package app.dialog

import Configuration
import Styles
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.ailaai.api.card
import app.ailaai.api.profile
import application
import com.queatz.ailaai.api.storyDraft
import com.queatz.ailaai.api.updateStory
import com.queatz.ailaai.api.updateStoryDraft
import com.queatz.db.Card
import com.queatz.db.Geo
import com.queatz.db.Group
import com.queatz.db.Story
import com.queatz.db.StoryContent
import com.queatz.db.StoryDraft
import com.queatz.db.asGeo
import com.queatz.db.toList
import components.Icon
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r
import stories.isNotBlank

suspend fun publishStoryDialog(
    configuration: Configuration,
    story: Story,
    storyContents: List<StoryContent>,
    onStoryUpdated: (Story) -> Unit
): Boolean {
    var publishEnabled by mutableStateOf(false)
    var groupsToShare by mutableStateOf<List<Group>?>(null)
    var geo by mutableStateOf(story.geo?.takeIf { it.size >= 2 }?.asGeo())
    var shareToGeo by mutableStateOf(story.geo?.takeIf { it.size >= 2 }?.asGeo())
    var containsCards by mutableStateOf<Boolean?>(null)
    var allCardsArePublished by mutableStateOf<Boolean?>(null)
    var friendCount by mutableStateOf(0)

    suspend fun saveGroups(groups: List<Group>) {
        api.updateStoryDraft(
            id = story.id!!,
            draft = StoryDraft(groups = groups.mapNotNull { it.id })
        ) {}
    }

    suspend fun saveLocation(newGeo: Geo?) {
        api.updateStory(
            id = story.id!!,
            story = Story(geo = newGeo?.toList())
        ) {
            onStoryUpdated(it)
        }
    }

    val confirmed = dialog(
        title = null,
        confirmButton = application.appString { post },
        cancelButton = application.appString { cancel },
        enableConfirm = { publishEnabled }
    ) { _ ->
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            api.storyDraft(
                id = story.id!!,
                onError = { groupsToShare = emptyList() }
            ) {
                groupsToShare = it.groupDetails ?: emptyList()
            }
        }

        LaunchedEffect(Unit) {
            application.me.value?.id?.let { me ->
                api.profile(me) {
                    friendCount = it.stats?.friendsCount ?: 0
                }
            }
        }

        LaunchedEffect(Unit) {
            allCardsArePublished = storyContents
                .mapNotNull { it as? StoryContent.Cards }
                .flatMap { cards ->
                    cards.cards.map { cardId ->
                        var card: Card? = null
                        api.card(cardId) {
                            card = it
                        }
                        card
                    }
                }
                .also {
                    containsCards = it.isNotEmpty()
                }
                .all { it?.active == true }
        }

        LaunchedEffect(allCardsArePublished) {
            publishEnabled = !story.title.isNullOrBlank() &&
                storyContents.any { it.isNotBlank() } &&
                allCardsArePublished == true
        }

        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(.5.r)
            }
        }) {
            if (containsCards == true) {
                allCardsArePublished?.let { published ->
                    if (published) {
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                alignItems(AlignItems.Center)
                                gap(1.r)
                            }
                        }) {
                            Icon(
                                name = "check_circle",
                                styles = {
                                    color(Styles.colors.primary)
                                }
                            )
                            Text(application.appString { allCardsArePosted })
                        }
                    } else {
                        Div({
                            style {
                                padding(1.r)
                                borderRadius(1.r)
                                border(1.px, LineStyle.Solid, Styles.colors.secondary)
                            }
                        }) {
                            Text(application.appString { storyContainsUnpostedCards })
                        }
                    }
                }
            }

            if (friendCount > 0) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        gap(1.r)
                    }
                }) {
                    Icon(
                        name = "group",
                        styles = {
                            color(Styles.colors.secondary)
                        }
                    )
                    Span {
                        Text(application.appString { sharedWithYour } + " ")
                    }
                    Span({
                        style {
                            fontWeight("bold")
                        }
                    }) {
                        val friends = application.appString {
                            if (friendCount == 1) xFriend else xFriends
                        }
                        Text(friends.replace("%1\$s", friendCount.toString()))
                    }
                }
            }

            if (shareToGeo != null) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        gap(.5.r)
                        cursor("pointer")
                        borderRadius(1.r)
                    }

                    onClick {
                        shareToGeo = null
                        scope.launch {
                            saveLocation(null)
                        }
                    }
                }) {
                    Icon(
                        name = "place",
                        styles = {
                            color(Styles.colors.secondary)
                        }
                    )
                    Span {
                        Text(application.appString { sharedWith } + " ")
                    }
                    Span({
                        style {
                            fontWeight("bold")
                        }
                    }) {
                        Text(application.appString { peopleNearby })
                    }
                    Icon(
                        name = "close",
                        styles = {
                            color(Styles.colors.primary)
                            fontSize(16.px)
                        }
                    )
                }
            }

            groupsToShare?.takeIf { it.isNotEmpty() }?.let { groups ->
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexWrap(FlexWrap.Wrap)
                        gap(.5.r)
                    }
                }) {
                    groups.forEach { group ->
                        Div({
                            classes(Styles.buttonSelectedThin)

                            style {
                                display(DisplayStyle.Flex)
                                alignItems(AlignItems.Center)
                                gap(.5.r)
                                padding(.5.r, 1.r)
                                borderRadius(2.r)
                                cursor("pointer")
                            }

                            onClick {
                                val newGroups = groups.filter { it.id != group.id }
                                groupsToShare = newGroups
                                scope.launch {
                                    saveGroups(newGroups)
                                }
                            }
                        }) {
                            Text(group.name ?: application.appString { someone })
                            Icon(
                                name = "close",
                                styles = {
                                    fontSize(16.px)
                                }
                            )
                        }
                    }
                }
            }

            Button({
                classes(Styles.outlineButton)

                onClick {
                    scope.launch {
                        val selected = selectGroupDialog(
                            configuration = configuration,
                            title = application.appString { shareToGroups },
                            cancelButton = application.appString { cancel }
                        )

                        if (selected != null) {
                            val group = selected.group ?: return@launch
                            val current = groupsToShare.orEmpty()

                            if (current.none { it.id == group.id }) {
                                val newGroups = current + group
                                groupsToShare = newGroups
                                saveGroups(newGroups)
                            }
                        }
                    }
                }
            }) {
                Icon(
                    name = "group",
                    styles = {
                        fontSize(18.px)
                    }
                )
                Span({
                    style {
                        property("margin-left", ".5rem")
                    }
                }) {
                    Text(application.appString { shareToGroups })
                }
            }

            Div({
                classes(AppStyles.groupItemMessage)
            }) {
                Text(application.appString { shareToGroupsDescription })
            }

            if (shareToGeo == null) {
                Button({
                    classes(Styles.outlineButton)

                    onClick {
                        scope.launch {
                            val selectedGeo = setLocationDialog(
                                initialGeo = geo
                            )

                            if (selectedGeo != null) {
                                geo = selectedGeo
                                shareToGeo = selectedGeo
                                saveLocation(selectedGeo)
                            }
                        }
                    }
                }) {
                    Icon(
                        name = "place",
                        styles = {
                            fontSize(18.px)
                        }
                    )
                    Span({
                        style {
                            property("margin-left", ".5rem")
                        }
                    }) {
                        Text(application.appString { addALocation })
                    }
                }

                Div({
                    classes(AppStyles.groupItemMessage)
                }) {
                    Text(application.appString { addALocationDescription })
                }
            }
        }
    }

    return confirmed == true
}

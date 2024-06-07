package com.queatz.ailaai.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.groupsOfPerson
import app.ailaai.api.storiesOfPerson
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.extensions.navigate
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.ContactItem
import com.queatz.ailaai.ui.components.GroupInfo
import com.queatz.ailaai.ui.components.SearchResult
import com.queatz.ailaai.ui.story.StoryCard
import com.queatz.ailaai.ui.story.StoryContents
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.GroupExtended
import com.queatz.db.Person
import com.queatz.db.Story
import kotlinx.coroutines.launch

@Composable
fun ProfileGroups(person: Person?) {
    var showAll by rememberStateOf(false)
    var groups by remember {
        mutableStateOf<List<GroupExtended>>(emptyList())
    }
    var stories by remember {
        mutableStateOf<List<Story>>(emptyList())
    }
    val nav = nav

    LaunchedEffect(person?.id) {
        api.groupsOfPerson(person?.id ?: return@LaunchedEffect) {
            groups = it
        }
    }

    LaunchedEffect(person?.id) {
        api.storiesOfPerson(person?.id ?: return@LaunchedEffect) {
            stories = it
        }
    }

    if (stories.isNotEmpty()) {
        Column(
            modifier = Modifier.padding(1.pad),
            verticalArrangement = Arrangement.spacedBy(1.pad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val state = rememberLazyListState()

            Text(
                stringResource(R.string.recent_stories),
                modifier = Modifier
                    .padding(bottom = 1.pad)
                    .fillMaxWidth()
            )
            LazyRow(
                state = state,
                horizontalArrangement = Arrangement.spacedBy(1.pad),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalFadingEdge(state, factor = 18f)
            ) {
                items(stories) {
                    StoryCard(
                        it,
                        false,
                        modifier = Modifier
                            .requiredHeight(180.dp)
                            .fillParentMaxWidth(.8f)
                    ) {
                        nav.navigate(AppNav.Story(it.id!!))
                    }
                }
            }
        }
    }

    if (groups.isNotEmpty()) {
        Column(
            modifier = Modifier
                .padding(1.pad)
                .widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(1.pad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(
                    R.string.x_is_a_member,
                    person?.name ?: stringResource(R.string.someone)
                ),
                modifier = Modifier
                    .padding(bottom = 1.pad)
                    .fillMaxWidth()
            )

            groups.let {
                if (showAll) {
                    it
                } else {
                    it.take(5)
                }
            }.forEach { group ->
                ContactItem(
                    SearchResult.Group(group),
                    onChange = {},
                    coverPhoto = true,
                    info = GroupInfo.Members
                )
            }

            if (!showAll && groups.size > 5) {
                OutlinedButton(
                    {
                        showAll = true
                    }
                ) {
                    Text(stringResource(R.string.show_more))
                }
            }
        }
    }
}

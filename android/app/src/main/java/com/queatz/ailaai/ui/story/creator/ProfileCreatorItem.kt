package com.queatz.ailaai.ui.story.creator

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.ailaai.api.profile
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.dialogs.ChoosePeopleDialog
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.profile.ProfileCard
import com.queatz.ailaai.ui.story.CreatorScope
import com.queatz.db.PersonProfile
import com.queatz.db.StoryContent

fun LazyGridScope.profilesCreatorItem(creatorScope: CreatorScope<StoryContent.Profiles>) = with(creatorScope) {
    itemsIndexed(
        items = creatorScope.part.profiles,
        key = { index, it -> "${creatorScope.id}.${it}" }
    ) { index, personId ->
        var profile by remember(personId) { mutableStateOf<PersonProfile?>(null)}
        var showMenu by rememberStateOf(false)
        var showAddDialog by rememberStateOf(false)

        if (showAddDialog) {
            ChoosePeopleDialog(
                onDismissRequest = { showAddDialog = false },
                title = stringResource(R.string.people),
                confirmFormatter = { stringResource(R.string.add) },
                multiple = true,
                onPeopleSelected = {
                    edit {
                        copy(
                            profiles = profiles + it
                                .mapNotNull { it.id }
                                .distinctBy { it }
                        )
                    }
                    showAddDialog = false
                },
                omit = { person ->
                    part.profiles.any { id -> person.id == id }
                }
            )
        }

        if (showMenu) {
            Menu(
                {
                    showMenu = false
                }
            ) {
                if (creatorScope.part.profiles.size > 1) {
                    menuItem(stringResource(R.string.remove)) {
                        showMenu = false
                        edit {
                            copy(
                                profiles = profiles.toMutableList().apply {
                                    removeAt(index)
                                }.toList()
                            )
                        }
                    }
                    menuItem(stringResource(R.string.remove_all)) {
                        showMenu = false
                        remove(partIndex)
                    }
                } else {
                    menuItem(stringResource(R.string.remove)) {
                        showMenu = false
                        remove(partIndex)
                    }
                }
                menuItem(stringResource(R.string.add)) {
                    showMenu = false
                    showAddDialog = true
                }
                // todo: Reorder
            }
        }

        LaunchedEffect(personId) {
            api.profile(personId) {
                profile = it
            }
        }

        profile?.let {
            ProfileCard(it) {
                showMenu = true
            }
        }
    }
}

package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import app.ailaai.api.profile
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.profile.ProfileCard
import com.queatz.db.PersonProfile
import com.queatz.db.StoryContent

fun LazyGridScope.profilesItem(
    content: StoryContent.Profiles,
) {
    items(content.profiles, key = { it }) { personId ->
        val nav = nav
        var profile by rememberStateOf<PersonProfile?>(null)

        LaunchedEffect(Unit) {
            api.profile(personId) {
                profile = it
            }
        }

        profile?.let {
            ProfileCard(it) {
                nav.appNavigate(AppNav.Profile(personId))
            }
        }
    }
}

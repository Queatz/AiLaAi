package com.queatz.ailaai.ui.story.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.data.Story
import com.queatz.ailaai.data.api
import com.queatz.ailaai.api.createGroup
import com.queatz.ailaai.api.deleteStory
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.dialogs.*
import kotlinx.coroutines.launch

@Composable
fun StoryActions(
    navController: NavController,
    storyId: String,
    story: Story?,
    me: () -> Person?,
    showOpen: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    var showMenu by rememberStateOf(false)
    var showMessageDialog by rememberStateOf(false)
    var showDeleteDialog by rememberStateOf(false)
    var showManageMenu by rememberStateOf(false)
    if (showManageMenu) {
        Menu({
            showManageMenu = false
        }) {
            menuItem(stringResource(R.string.delete)) {
                showDeleteDialog = true
                showManageMenu = false
            }
        }
    }

    if (showDeleteDialog) {
        Alert(
            { showDeleteDialog = false },
            title = stringResource(R.string.delete_story),
            text = stringResource(R.string.you_cannot_undo_this_story),
            dismissButton = stringResource(R.string.cancel),
            confirmButton = stringResource(R.string.delete),
            confirmColor = MaterialTheme.colorScheme.error
        ) {
            scope.launch {
                api.deleteStory(story!!.id!!) {
                    showDeleteDialog = false
                    navController.popBackStack()
                }
            }
        }
    }

    if (showMessageDialog) {
        val someone = stringResource(R.string.someone)
        ChoosePeopleDialog(
            title = stringResource(R.string.authors),
            onDismissRequest = {
                showMessageDialog = false
            },
            // Todo, show last active, it could be useful to know if the author has been inactive for 1 year
//            infoFormatter = { person ->
//                person.seen?.timeAgo()?.let { timeAgo ->
//                    "${context.getString(R.string.active)} ${timeAgo.lowercase()}"
//                }
//            },
            confirmFormatter = defaultConfirmFormatter(
                R.string.send_message,
                R.string.send_message_to_person,
                R.string.send_message_to_people,
                R.string.send_message_to_x_people
            ) { it.name ?: someone },
            people = story?.authors ?: emptyList(),
            onPeopleSelected = { authors ->
                scope.launch {
                    api.createGroup(authors.map { it.id!! } + me()!!.id!!, reuse = true) {
                        navController.navigate("group/${it.id!!}")
                    }
                }
                showMessageDialog = false
            }
        )
    }

    IconButton(
        {
            showMessageDialog = true
        }
    ) {
        Icon(
            Icons.Outlined.Message,
            stringResource(R.string.message),
            tint = MaterialTheme.colorScheme.primary
        )
    }
    IconButton(
        {
            showMenu = true
        }
    ) {
        Icon(
            Icons.Outlined.MoreVert,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        StoryMenu(
            showMenu,
            {
                showMenu = false
            },
            navController,
            storyId,
            story,
            me = me(),
            isMine = story?.person == me()?.id,
            showOpen = showOpen,
            editing = false
        )
    }
}

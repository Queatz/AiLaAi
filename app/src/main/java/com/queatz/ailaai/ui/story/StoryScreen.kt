package com.queatz.ailaai.ui.story

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.Story
import com.queatz.ailaai.api
import com.queatz.ailaai.api.deleteStory
import com.queatz.ailaai.api.story
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.dialogs.*
import com.queatz.ailaai.ui.dialogs.PeopleDialog
import com.queatz.ailaai.ui.story.editor.StoryMenu
import kotlinx.coroutines.launch

@Composable
fun StoryScreen(storyId: String, navController: NavController, me: () -> Person?) {
    // todo storyId could be a url from a deeplink
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val context = LocalContext.current
    var isLoading by rememberStateOf(true)
    var showMenu by rememberStateOf(false)
    var showMessageDialog by rememberStateOf(false)
    var showDeleteDialog by rememberStateOf(false)
    var showManageMenu by rememberStateOf(false)
    var story by remember { mutableStateOf<Story?>(null) }
    var contents by remember { mutableStateOf(emptyList<StoryContent>()) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            story = api.story(storyId)
        } catch (e: Exception) {
            e.printStackTrace()
            context.showDidntWork()
        }
        isLoading = false
    }

    LaunchedEffect(story) {
        contents = story?.asContents() ?: emptyList()
    }

    // todo use a loading/error/empty scaffold
    if (isLoading) {
        Loading()
        return
    }

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
                try {
                    api.deleteStory(story!!.id!!)
                    showDeleteDialog = false
                    navController.popBackStack()
                } catch (e: Exception) {
                    e.printStackTrace()
                    context.showDidntWork()
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
                    val group = api.createGroup(authors.map { it.id!! } + me()!!.id!!, reuse = true)
                    navController.navigate("group/${group.id!!}")
                }
                showMessageDialog = false
            }
        )
    }

    StoryScaffold(
        {
            navController.popBackStack()
        },
        actions = {
            if (story == null) return@StoryScaffold

            StoryTitle(state, story)
            IconButton(
                {
                    showMessageDialog = true
                }
            ) {
                Icon(
                    Icons.Outlined.Message,
                    stringResource(R.string.message),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                    editing = false
                )
            }
        }
    ) {
        StoryContents(
            contents,
            state,
            navController,
            modifier = Modifier.fillMaxSize()
        )
    }
}

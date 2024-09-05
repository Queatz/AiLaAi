package com.queatz.ailaai.ui.story.editor

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.ailaai.api.card
import app.ailaai.api.updateCard
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.api.deleteStory
import com.queatz.ailaai.api.updateStory
import com.queatz.ailaai.api.uploadPhotosFromUris
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialog
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialogState
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.QrCodeDialog
import com.queatz.ailaai.ui.dialogs.ReportDialog
import com.queatz.ailaai.ui.dialogs.ViewSourceDialog
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.db.Card
import com.queatz.db.Story
import kotlinx.coroutines.launch

@Composable
fun StoryMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    storyId: String,
    story: Story?,
    isMine: Boolean,
    showOpen: Boolean = false,
    edited: Boolean = false,
    editing: Boolean = false,
    onIsLoading: (Boolean) -> Unit = {},
    onReorder: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by rememberStateOf(false)
    var showManageMenu by rememberStateOf(false)
    var showSendDialog by rememberStateOf(false)
    var showQrCode by rememberStateOf(false)
    var showReportDialog by rememberStateOf(false)
    var showSourceDialog by rememberStateOf(false)
    var showBackgroundDialog by rememberStateOf(false)

    val setBackgroundState = remember(story?.title == null) {
        ChoosePhotoDialogState(mutableStateOf(story?.title ?: ""))
    }

    val textCopied = stringResource(R.string.copied)
    val storyString = stringResource(R.string.story)
    val nav = nav

    if (showSourceDialog) {
        ViewSourceDialog({ showSourceDialog = false }, story?.content)
    }

    if (showDeleteDialog) {
        DeleteStoryDialog(
            {
                showDeleteDialog = false
            }
        ) {
            scope.launch {
                api.deleteStory(storyId) {
                    showDeleteDialog = false
                    nav.popBackStack()
                }
            }
        }
    }

    if (showBackgroundDialog) {
        ChoosePhotoDialog(
            scope = scope,
            state = setBackgroundState,
            onDismissRequest = { showBackgroundDialog = false },
            multiple = false,
            imagesOnly = true,
            onPhotos = { photos ->
                scope.launch {
                    onIsLoading(true)
                    api.uploadPhotosFromUris(context, photos) {
                        api.updateStory(storyId, Story(background = it.urls.first())) {
                            context.toast(R.string.background_updated)
                        }
                    }
                    onIsLoading(false)
                }
            },
            onGeneratedPhoto = { photo ->
                scope.launch {
                    api.updateStory(storyId, Story(background = photo)) {
                        context.toast(R.string.background_updated)
                    }
                }
            },
            onIsGeneratingPhoto = {
                onIsLoading(it)
            }
        )
    }
    if (showManageMenu) {
        Menu({
            showManageMenu = false
        }) {
            menuItem(stringResource(R.string.delete)) {
                showManageMenu = false
                showDeleteDialog = true
            }
        }
    }

    if (showSendDialog) {
        SendStoryDialog(
            {
                showSendDialog = false
            },
            storyId
        )
    }

    if (showQrCode) {
        QrCodeDialog(
            {
                showQrCode = false
            },
            storyUrl(storyId),
            story?.title
        )
    }

    if (showReportDialog) {
        ReportDialog("story/$storyId") {
            showReportDialog = false
        }
    }

    Dropdown(
        expanded,
        {
            onDismissRequest()
        }
    ) {
        if (showOpen) {
            DropdownMenuItem({ Text(stringResource(R.string.open_story)) }, {
                onDismissRequest()
                nav.appNavigate(AppNav.Story(storyId))
            })
        } else if (editing) {
            DropdownMenuItem({ Text(stringResource(R.string.preview)) }, {
                onDismissRequest()
                nav.appNavigate(AppNav.Story(storyId))
            }, enabled = !edited)

            DropdownMenuItem({ Text(stringResource(R.string.reorder)) }, {
                onDismissRequest()
                onReorder?.invoke()
            })
        }
        if (isMine) {
            if (editing) {
                DropdownMenuItem({ Text(stringResource(R.string.background)) }, {
                    showBackgroundDialog = true
                    onDismissRequest()
                })
            }
            DropdownMenuItem({ Text(stringResource(R.string.manage)) }, {
                showManageMenu = true
                onDismissRequest()
            })
        }
        DropdownMenuItem({ Text(stringResource(R.string.send)) }, {
            onDismissRequest()
            showSendDialog = true
        })
        DropdownMenuItem({ Text(stringResource(R.string.qr_code)) }, {
            showQrCode = true
            onDismissRequest()
        })
        if (story?.geo != null) {
            DropdownMenuItem({ Text(stringResource(R.string.show_on_map)) }, {
                onDismissRequest()
                val uri = Uri.parse(
                    "geo:${story.geo!![0]},${story.geo!![1]}?q=${story.geo!![0]},${story.geo!![1]}(${
                        Uri.encode(story.title ?: storyString)
                    })"
                )
                val intent = Intent(Intent.ACTION_VIEW, uri)
                nav.context.startActivity(Intent.createChooser(intent, null))
            })
        }
        DropdownMenuItem({ Text(stringResource(R.string.share)) }, {
            storyUrl(story?.url ?: storyId).shareAsUrl(context, story?.title ?: storyString)
            onDismissRequest()
        })
        DropdownMenuItem({ Text(stringResource(R.string.copy_link)) }, {
            storyUrl(story?.url ?: storyId).copyToClipboard(context, story?.title ?: storyString)
            context.toast(textCopied)
            onDismissRequest()
        })

                                    DropdownMenuItem({
                                        Text(stringResource(R.string.view_source))
                                    }, {
                                        showSourceDialog = true
                                        onDismissRequest()
                                    })
        DropdownMenuItem({ Text(stringResource(R.string.report)) }, {
            showReportDialog = true
            onDismissRequest()
        })
    }
}

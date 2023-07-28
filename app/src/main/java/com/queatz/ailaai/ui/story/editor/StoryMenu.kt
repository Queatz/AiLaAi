package com.queatz.ailaai.ui.story.editor

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.data.Story
import com.queatz.ailaai.data.api
import com.queatz.ailaai.api.deleteStory
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.QrCodeDialog
import com.queatz.ailaai.ui.dialogs.ReportDialog
import com.queatz.ailaai.ui.dialogs.menuItem
import kotlinx.coroutines.launch

@Composable
fun StoryMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    navController: NavController,
    storyId: String,
    story: Story?,
    me: Person?,
    isMine: Boolean,
    showOpen: Boolean = false,
    edited: Boolean = false,
    editing: Boolean = false,
    onReorder: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by rememberStateOf(false)
    var showManageMenu by rememberStateOf(false)
    var showSendDialog by rememberStateOf(false)
    var showQrCode by rememberStateOf(false)
    var showReportDialog by rememberStateOf(false)
    val textCopied = stringResource(R.string.copied)
    val storyString = stringResource(R.string.story)

    if (showDeleteDialog) {
        DeleteStoryDialog(
            {
                showDeleteDialog = false
            }
        ) {
            scope.launch {
                api.deleteStory(storyId) {
                    showDeleteDialog = false
                    navController.popBackStack()
                }
            }
        }
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
            storyId,
            me
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

    DropdownMenu(
        expanded,
        {
            onDismissRequest()
        }
    ) {
        if (showOpen) {
            DropdownMenuItem({ Text(stringResource(R.string.open_story)) }, {
                onDismissRequest()
                navController.navigate("story/$storyId")
            })
        } else if (!edited && editing) {
            DropdownMenuItem({ Text(stringResource(R.string.preview)) }, {
                onDismissRequest()
                navController.navigate("story/$storyId")
            })
        }
        if (editing) {
            DropdownMenuItem({ Text(stringResource(R.string.reorder)) }, {
                onDismissRequest()
                onReorder?.invoke()
            })
        }
        if (isMine) {
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
                navController.context.startActivity(Intent.createChooser(intent, null))
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
        DropdownMenuItem({ Text(stringResource(R.string.report)) }, {
            showReportDialog = true
            onDismissRequest()
        })
    }
}

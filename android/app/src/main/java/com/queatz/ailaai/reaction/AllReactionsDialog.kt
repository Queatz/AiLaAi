package com.queatz.ailaai.reaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.bulletedString
import com.queatz.ailaai.extensions.shortAgo
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.dialogs.ItemsPeopleDialog
import com.queatz.db.Reaction
import com.queatz.db.ReactionAndPerson

@Composable
fun AllReactionsDialog(
    onDismissRequest: () -> Unit,
    reactions: List<ReactionAndPerson>,
    meId: String?,
    onRemoveReaction: (Reaction) -> Unit,
    onEditReactionComment: ((Reaction) -> Unit)? = null
) {
    val nav = nav
    val context = LocalContext.current

    ItemsPeopleDialog(
        title = stringResource(id = R.string.reactions),
        onDismissRequest = onDismissRequest,
        items = reactions,
        key = { it.reaction!!.id!! },
        people = { it.person!! },
        infoFormatter = { reaction ->
            bulletedString(
                reaction.reaction!!.reaction!!,
                reaction.reaction!!.createdAt!!.shortAgo(context),
                reaction.reaction!!.comment,
                if (onEditReactionComment != null && reaction.person?.id == meId) {
                    context.getString(R.string.tap_to_edit)
                } else {
                    null
                }
            )
        },
        itemAction = { reaction ->
            if (reaction.person?.id == meId) {
                IconButton(
                    onClick = {
                        onDismissRequest()
                        onRemoveReaction(reaction.reaction!!)
                    }
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    ) { reaction ->
        onDismissRequest()
        if (onEditReactionComment != null && reaction.person?.id == meId) {
            onEditReactionComment(reaction.reaction!!)
        } else {
            nav.appNavigate(AppNav.Profile(reaction.person!!.id!!))
        }
    }
}

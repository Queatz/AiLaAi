package com.queatz.ailaai.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.ailaai.api.*
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.api.*
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.data.json
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.npc.NpcDialog
import com.queatz.ailaai.ui.card.*
import com.queatz.ailaai.ui.dialogs.*
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import com.queatz.db.CardAttachment
import com.queatz.db.Message
import io.ktor.http.HttpStatusCode
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

@Composable
fun CardDialogs(state: CardScreenState) {
    val scope = state.scope
    val context = state.context
    val cardId = state.cardId
    val me = state.me
    val nav = nav
    val someone = stringResource(R.string.someone)
    val emptyGroup = stringResource(R.string.empty_group_name)

    if (state.showManageMenu) {
        Menu(
            onDismissRequest = {
                state.showManageMenu = false
            }
        ) {
            menuItem(stringResource(R.string.page_url)) {
                state.openChangeUrl = true
                state.showManageMenu = false
            }
            menuItem(stringResource(R.string.change_owner)) {
                state.openChangeOwner = true
                state.showManageMenu = false
            }
            if ((state.card?.level ?: 0) > 0) {
                menuItem(stringResource(R.string.downgrade)) {
                    state.showDowngradeDialog = true
                    state.showManageMenu = false
                }
            }
            menuItem(stringResource(R.string.delete_card)) {
                state.openDeleteCard = true
                state.showManageMenu = false
            }
        }
    }

    if (state.openChangeUrl && state.card != null) {
        TextFieldDialog(
            onDismissRequest = { state.openChangeUrl = false },
            title = stringResource(R.string.page_url),
            button = stringResource(R.string.update),
            singleLine = true,
            initialValue = state.card?.url ?: "",
            bottomContent = { url ->
                if (url.isNotBlank()) {
                    Text(
                        text = "$appDomain/page/$url",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }
        ) { value ->
            api.updateCard(
                state.card?.id!!,
                Card(url = value.trim()),
                onError = {
                    if (it.status == HttpStatusCode.Conflict) {
                        context.toast(R.string.url_already_in_use)
                    }
                }
            ) {
                state.openChangeUrl = false
                state.reload()
            }
        }
    }

    if (state.showStatisticsDialog && state.card != null) {
        PageStatisticsDialog(state.card!!) {
            state.showStatisticsDialog = false
        }
    }

    if (state.showPhotoDialog) {
        ChoosePhotoDialog(
            scope = scope,
            state = state.setPhotoState,
            onDismissRequest = { state.showPhotoDialog = false },
            multiple = false,
            onPhotos = { photos ->
                scope.launch {
                    api.uploadCardPhotoFromUri(context, state.card!!.id!!, photos.firstOrNull() ?: return@launch) {
                        state.reload()
                    }
                }
            },
            onVideos = { videos ->
                val it = videos.firstOrNull() ?: return@ChoosePhotoDialog
                state.uploadJob = scope.launch {
                    state.videoUploadProgress = 0f
                    state.isUploadingVideo = true
                    api.uploadCardVideoFromUri(
                        context = context,
                        id = state.card!!.id!!,
                        video = it,
                        contentType = context.contentResolver.getType(it) ?: "video/*",
                        filename = it.lastPathSegment ?: "video.${
                            context.contentResolver.getType(it)?.split("/")?.lastOrNull() ?: ""
                        }",
                        processingCallback = {
                            state.videoUploadStage = ProcessingVideoStage.Processing
                            state.videoUploadProgress = it
                        },
                        uploadCallback = {
                            state.videoUploadStage = ProcessingVideoStage.Uploading
                            state.videoUploadProgress = it
                        }
                    )
                    state.reload()
                    state.uploadJob = null
                    state.isUploadingVideo = false
                }
            },
            onGeneratedPhoto = { photo ->
                scope.launch {
                    api.updateCard(state.card!!.id!!, Card(photo = photo)) {
                        state.reload()
                    }
                }
            },
            onIsGeneratingPhoto = {
                state.isGeneratingPhoto = it
            }
        )
    }

    if (state.showBackgroundDialog) {
        ChoosePhotoDialog(
            scope = scope,
            state = state.setBackgroundState,
            onDismissRequest = { state.showBackgroundDialog = false },
            multiple = false,
            imagesOnly = true,
            onPhotos = { photos ->
                scope.launch {
                    state.isGeneratingBackground = true
                    api.uploadPhotosFromUris(context, photos) {
                        api.updateCard(cardId, Card(background = it.urls.first())) {
                            state.reload()
                        }
                    }
                    state.isGeneratingBackground = false
                }
            },
            onGeneratedPhoto = { photo ->
                scope.launch {
                    api.updateCard(cardId, Card(background = photo)) {
                        state.reload()
                    }
                }
            },
            onIsGeneratingPhoto = {
                state.isGeneratingBackground = it
            }
        )
    }

    if (state.isUploadingVideo) {
        ProcessingVideoDialog(
            onDismissRequest = { state.isUploadingVideo = false },
            onCancelRequest = { state.uploadJob?.cancel() },
            stage = state.videoUploadStage,
            progress = state.videoUploadProgress
        )
    }

    if (state.showRegeneratePhotoDialog) {
        AlertDialog(
            onDismissRequest = {
                state.showRegeneratePhotoDialog = false
            },
            title = {
                Text(stringResource(R.string.generate_a_new_photo))
            },
            text = {
                Text(stringResource(R.string.this_will_replace_the_current_photo))
            },
            confirmButton = {
                TextButton({
                    state.showRegeneratePhotoDialog = false
                    state.generatePhoto()
                }) {
                    Text(stringResource(R.string.yes))
                }
            }
        )
    }

    if (state.showGeneratingPhotoDialog) {
        AlertDialog(
            onDismissRequest = {
                state.showGeneratingPhotoDialog = false
            },
            title = {
                Text(stringResource(R.string.generating))
            },
            text = {
                Text(stringResource(R.string.generating_description))
            },
            dismissButton = {
                DialogCloseButton {
                    state.showGeneratingPhotoDialog = false
                }
            },
            confirmButton = {
                TextButton({
                    state.showGeneratingPhotoDialog = false
                    scope.launch {
                        context.dataStore.edit {
                            it[booleanPreferencesKey("ui.showGeneratingMessage")] = false
                        }
                    }
                }) {
                    Text(stringResource(R.string.dont_show))
                }
            },
        )
    }

    if (state.showSetCategory) {
        ChooseCategoryDialog(
            {
                state.showSetCategory = false
            },
            preselect = state.card?.categories?.firstOrNull(),
            { category ->
                scope.launch {
                    api.updateCard(
                        state.card!!.id!!,
                        Card().apply {
                            categories = if (category == null) emptyList() else listOf(category)
                        }
                    ) {
                        state.reload()
                    }
                }
            }
        )
    }

    if (state.showPay) {
        PayDialog(
            onDismissRequest = {
                state.showPay = false
            },
            defaultPay = state.card?.pay?.pay,
            defaultFrequency = state.card?.pay?.frequency
        ) { pay ->
            api.updateCard(
                id = cardId,
                card = Card(pay = pay)
            ) {
                state.reload()
            }
        }
    }

    if (state.showNpc) {
        NpcDialog(
            npc = state.card?.npc,
            onDismissRequest = {
                state.showNpc = false
            }
        ) { npc ->
            api.updateCard(
                cardId,
                Card(npc = npc)
            ) {
                state.reload()
                state.showNpc = false
            }
        }
    }

    if (state.showReportDialog) {
        ReportDialog("card/$cardId") {
            state.showReportDialog = false
        }
    }

    if (state.showSourceDialog) {
        ViewSourceDialog({ state.showSourceDialog = false }, state.card?.content)
    }

    if (state.openLocationDialog) {
        EditCardLocationDialog(state.card!!, context as Activity, {
            state.openLocationDialog = false
        }, {
            state.reload()
        })
    }

    if (state.openEditDialog) {
        EditCardDialog(state.card!!, {
            state.openEditDialog = false
        }) {
            state.reload()
        }
    }

    if (state.newCard != null) {
        EditCardDialog(
            state.newCard!!,
            {
                state.newCard = null
            },
            create = true
        ) {
            state.reloadCards()
            nav.appNavigate(AppNav.Page(it.id!!))
        }
    }

    state.card?.let { card ->
        if (state.openDeleteCard) {
            DeleteCardDialog(card, {
                state.openDeleteCard = false
            }) {
                nav.popBackStackOrFinish()
            }
        }
    }

    if (state.openChangeOwner) {
        val someone = stringResource(R.string.someone)
        ChoosePeopleDialog(
            {
                state.openChangeOwner = false
            },
            title = stringResource(R.string.change_owner),
            confirmFormatter = defaultConfirmFormatter(
                R.string.give,
                R.string.give_to_person,
                R.string.give_to_people,
                R.string.give_to_x_people
            ) { it.name ?: someone },
            omit = { it.id == me?.id },
            multiple = false,
            onPeopleSelected = {
                if (it.size == 1) {
                    val newOwner = it.first().id
                    scope.launch {
                        state.card!!.person = newOwner
                        api.updateCard(state.card!!.id!!, Card().apply {
                            person = state.card!!.person
                        }) {
                            state.card = it
                        }
                    }
                }
            }
        )
    }

    if (state.openLeaveCollaboratorsDialog) {
        AlertDialog(
            onDismissRequest = {
                state.openLeaveCollaboratorsDialog = false
            },
            title = {
                Text(stringResource(R.string.leave_card))
            },
            confirmButton = {
                TextButton({
                    scope.launch {
                        api.leaveCollaboration(cardId)
                        state.reload()
                        state.reloadCards()
                        state.openLeaveCollaboratorsDialog = false
                    }
                }) {
                    Text(stringResource(R.string.leave))
                }
            },
            dismissButton = {
                TextButton({
                    state.openLeaveCollaboratorsDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (state.openAddCollaboratorDialog) {
        val someone = stringResource(R.string.someone)
        ChoosePeopleDialog(
            onDismissRequest = {
                state.openAddCollaboratorDialog = false
            },
            title = stringResource(R.string.add_collaborators),
            confirmFormatter = defaultConfirmFormatter(
                R.string.add,
                R.string.add_person,
                R.string.add_people,
                R.string.add_x_people
            ) { it.name ?: someone },
            onPeopleSelected = { people ->
                state.card!!.collaborators = (state.card?.collaborators ?: emptyList()) + people.map { it.id!! }
                api.updateCard(state.card!!.id!!, Card().apply {
                    collaborators = state.card!!.collaborators
                }) {
                    state.card = it
                }
            },
            omit = { it.id == me?.id || state.card!!.collaborators?.contains(it.id) == true }
        )
    }

    if (state.openRemoveCollaboratorsDialog) {
        val someone = stringResource(R.string.someone)
        ChoosePeopleDialog(
            {
                state.openRemoveCollaboratorsDialog = false
            },
            title = stringResource(R.string.collaborators),
            confirmFormatter = defaultConfirmFormatter(
                none = R.string.remove,
                one = R.string.remove_person,
                two = R.string.remove_people,
                many = R.string.remove_x_people
            ) { it.name ?: someone },
            extraButtons = {
                TextButton(
                    {
                        state.openRemoveCollaboratorsDialog = false
                        state.openAddCollaboratorDialog = true
                    }
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            onPeopleSelected = { people ->
                state.card!!.collaborators = (state.card?.collaborators ?: emptyList()) - people.map { it.id!! }.toSet()
                api.updateCard(state.card!!.id!!, Card().apply {
                    collaborators = state.card!!.collaborators
                }) {
                    state.card = it
                }
            },
            omit = { it.id !in (state.card!!.collaborators ?: emptyList()) }
        )
    }

    if (state.openCollaboratorsDialog && state.collaborators.isNotEmpty()) {
        PeopleDialog(
            title = stringResource(R.string.collaborators),
            onDismissRequest = {
                state.openCollaboratorsDialog = false
            },
            people = state.collaborators,
            infoFormatter = { person ->
                if (person.id == me?.id) {
                    context.getString(R.string.leave)
                } else {
                    person.seen?.timeAgo()?.let { timeAgo ->
                        "${context.getString(R.string.active)} ${timeAgo.lowercase()}"
                    }
                }
            }
        ) { person ->
            if (person.id == me?.id) {
                state.openLeaveCollaboratorsDialog = true
                state.openCollaboratorsDialog = false
            } else {
                scope.launch {
                    api.createGroup(listOf(me!!.id!!, person.id!!), reuse = true) {
                        nav.appNavigate(AppNav.Group(it.id!!))
                        state.openCollaboratorsDialog = false
                    }
                }
            }
        }
    }

    if (state.showQrCode) {
        QrCodeDialog(
            onDismissRequest = { state.showQrCode = false },
            url = cardUrl(cardId),
            name = state.card?.name
        )
    }

    if (state.showSendDialog) {
        val sent = stringResource(R.string.sent)
        ChooseGroupDialog(
            onDismissRequest = {
                state.showSendDialog = false
            },
            title = stringResource(R.string.send_card),
            confirmFormatter = defaultConfirmFormatter(
                R.string.send_card,
                R.string.send_card_to_group,
                R.string.send_card_to_groups,
                R.string.send_card_to_x_groups
            ) { it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()) }
        ) { groups ->
            scope.launch {
                coroutineScope {
                    var sendSuccess = false
                    groups.map { group ->
                        async {
                            api.sendMessage(
                                group.id!!,
                                Message(attachment = json.encodeToString(CardAttachment(cardId)))
                            ) {
                                sendSuccess = true
                            }
                        }
                    }.awaitAll()
                    if (sendSuccess) {
                        context.toast(sent)
                    }
                }
            }
        }
    }

    if (state.showUpgradeDialog) {
        CardUpgradeDialog(
            onDismissRequest = { state.showUpgradeDialog = false },
            cardId = cardId,
            currentLevel = state.card?.level ?: 0
        ) {
            state.reload()
        }
    }

    if (state.showDowngradeDialog) {
        CardDowngradeDialog(
            onDismissRequest = { state.showDowngradeDialog = false },
            cardId = cardId,
            currentLevel = state.card?.level ?: 0
        ) {
            state.reload()
        }
    }

    if (state.showPageSizeDialog) {
        TextFieldDialog(
            onDismissRequest = {
                state.showPageSizeDialog = false
            },
            title = stringResource(R.string.page_size),
            button = stringResource(R.string.update),
            showDismiss = true,
            initialValue = state.card?.size?.format().orEmpty(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            valueFormatter = {
                if (it.isNumericTextInput()) it else null
            },
            extraContent = {
                Text(
                    text = stringResource(R.string.page_size_description),
                    modifier = Modifier.padding(bottom = 1.pad)
                )
            }
        ) { size ->
            api.updateCard(
                id = state.card?.id ?: return@TextFieldDialog,
                card = Card(
                    size = size.toDoubleOrNull() ?: 0.0
                )
            ) {
                state.reload()
                state.showPageSizeDialog = false
            }
        }
    }
}

private fun booleanPreferencesKey(name: String) = androidx.datastore.preferences.core.booleanPreferencesKey(name)

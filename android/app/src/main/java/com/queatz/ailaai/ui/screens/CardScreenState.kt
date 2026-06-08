package com.queatz.ailaai.ui.screens

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import app.ailaai.api.*
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.api.*
import com.queatz.ailaai.data.api
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialogState
import com.queatz.ailaai.ui.dialogs.ProcessingVideoStage
import com.queatz.ailaai.ui.state.jsonSaver
import com.queatz.db.Card
import com.queatz.db.Person
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant.Companion.fromEpochMilliseconds

@Composable
fun rememberCardScreenState(cardId: String): CardScreenState {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val me = me
    val gridState = rememberLazyGridState()
    val gridStateLandscape = rememberLazyGridState()

    val cardState = rememberSaveable(cardId, stateSaver = jsonSaver<Card?>()) { mutableStateOf(null) }
    val cardsState = rememberSaveable(cardId, stateSaver = jsonSaver<List<Card>>(emptyList())) { mutableStateOf(emptyList()) }
    val isLoadingState = rememberSaveable(cardId) { mutableStateOf(false) }
    val notFoundState = rememberSaveable(cardId) { mutableStateOf(false) }

    return remember(cardId) {
        CardScreenState(
            cardId = cardId,
            scope = scope,
            context = context,
            me = me,
            gridState = gridState,
            gridStateLandscape = gridStateLandscape,
            cardState = cardState,
            cardsState = cardsState,
            isLoadingState = isLoadingState,
            notFoundState = notFoundState
        )
    }
}

class CardScreenState(
    val cardId: String,
    val scope: CoroutineScope,
    val context: Context,
    val me: Person?,
    val gridState: LazyGridState,
    val gridStateLandscape: LazyGridState,
    cardState: MutableState<Card?>,
    cardsState: MutableState<List<Card>>,
    isLoadingState: MutableState<Boolean>,
    notFoundState: MutableState<Boolean>
) {
    var isLoading by isLoadingState
    var showTools by mutableStateOf(true)
    var notFound by notFoundState
    var showMenu by mutableStateOf(false)
    var showManageMenu by mutableStateOf(false)
    var showAdditionalPhotosDialog by mutableStateOf(false)
    var showStatisticsDialog by mutableStateOf(false)
    var openDeleteCard by mutableStateOf(false)
    var openLocationDialog by mutableStateOf(false)
    var showReportDialog by mutableStateOf(false)
    var openEditDialog by mutableStateOf(false)
    var openChangeOwner by mutableStateOf(false)
    var openChangeUrl by mutableStateOf(false)
    var showQrCode by mutableStateOf(false)
    var showSendDialog by mutableStateOf(false)
    var openAddCollaboratorDialog by mutableStateOf(false)
    var openRemoveCollaboratorsDialog by mutableStateOf(false)
    var openCollaboratorsDialog by mutableStateOf(false)
    var openLeaveCollaboratorsDialog by mutableStateOf(false)
    var showUpgradeDialog by mutableStateOf(false)
    var showDowngradeDialog by mutableStateOf(false)
    var showPageSizeDialog by mutableStateOf(false)
    var card by cardState
    var cards by cardsState
    var uploadJob by mutableStateOf<Job?>(null)
    var isUploadingVideo by mutableStateOf(false)
    var videoUploadStage by mutableStateOf(ProcessingVideoStage.Processing)
    var videoUploadProgress by mutableStateOf(0f)
    var showSetCategory by mutableStateOf(false)
    var isGeneratingBackground by mutableStateOf(false)
    var showBackgroundDialog by mutableStateOf(false)
    var showPay by mutableStateOf(false)
    var showNpc by mutableStateOf(false)
    var showRegeneratePhotoDialog by mutableStateOf(false)
    var showGeneratingPhotoDialog by mutableStateOf(false)
    var isGeneratingPhoto by mutableStateOf(false)
    var isRegeneratingPhoto by mutableStateOf(false)
    var showPhotoDialog by mutableStateOf(false)
    var oldPhoto by mutableStateOf<String?>(null)
    var showSourceDialog by mutableStateOf(false)
    var showScanMe by mutableStateOf(false)
    var newCard by mutableStateOf<Card?>(null)
    var collaborators by mutableStateOf(emptyList<Person>())

    val setPhotoState = ChoosePhotoDialogState(mutableStateOf(card?.name ?: ""))
    val setBackgroundState = ChoosePhotoDialogState(mutableStateOf(card?.name ?: ""))

    val isMine get() = me?.id != null && card?.person == me?.id
    val isMineOrIAmACollaborator get() = isMine || card?.collaborators?.contains(me?.id) == true

    fun reload() {
        scope.launch {
            api.card(cardId) { card = it }
        }
    }

    fun reloadCards() {
        scope.launch {
            api.cardsCards(cardId) { cards = it }
        }
    }

    fun toggleShowTools() {
        scope.launch {
            showTools = context.dataStore.data.first().let {
                it[booleanPreferencesKey("ui.showMyCardTools")]?.not() ?: false
            }
            context.dataStore.edit {
                it[booleanPreferencesKey("ui.showMyCardTools")] = showTools
            }
        }
    }

    fun generatePhoto() {
        isRegeneratingPhoto = true
        scope.launch {
            api.generateCardPhoto(cardId) {
                if (
                    context.dataStore.data.first().let {
                        it[booleanPreferencesKey("ui.showGeneratingMessage")] != false
                    }
                ) {
                    showGeneratingPhotoDialog = true
                }
                oldPhoto = card?.photo ?: ""
            }
            isRegeneratingPhoto = false
        }
    }

    fun regeneratePhoto() {
        if (card?.photo.isNullOrBlank()) {
            generatePhoto()
        } else {
            showRegeneratePhotoDialog = true
        }
    }

    fun addToHomescreen() {
        scope.launch {
            val pinShortcutInfo = ShortcutInfoCompat.Builder(context, "page/$cardId")
                .setIcon(
                    card?.photo?.let { api.url(it) }?.asOvalBitmap(context)?.let { IconCompat.createWithBitmap(it) }
                        ?: IconCompat.createWithResource(context, R.mipmap.ic_app)
                )
                .setShortLabel(card?.name ?: context.getString(R.string.app_name))
                .setIntent(Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = cardUrl(cardId).let { "$it?fullscreen=true" }.toUri()
                })
                .build()
            val pinnedShortcutCallbackIntent =
                ShortcutManagerCompat.createShortcutResultIntent(context, pinShortcutInfo)
            val successCallback = PendingIntent.getBroadcast(
                context,
                0,
                pinnedShortcutCallbackIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            ShortcutManagerCompat.requestPinShortcut(
                context,
                pinShortcutInfo,
                successCallback.intentSender
            )
        }
    }

    fun fetchCollaborators() {
        scope.launch {
            api.cardPeople(cardId) {
                collaborators = it.sortedByDescending { it.seen ?: fromEpochMilliseconds(0) }
            }
        }
    }
}

package com.queatz.ailaai.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import at.bluesource.choicesdk.maps.common.LatLng
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.api.updateCard
import com.queatz.ailaai.api.uploadCardPhoto
import com.queatz.ailaai.api.uploadCardVideo
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.ui.dialogs.*
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun CardItem(
    onClick: (() -> Unit)?,
    onCategoryClick: (String) -> Unit = {},
    onReply: (List<String>) -> Unit = {},
    onChange: () -> Unit = {},
    card: Card?,
    activity: Activity? = null,
    showDistance: LatLng? = null,
    edit: EditCard? = null,
    isMine: Boolean = false,
    isMineToolbar: Boolean = true,
    isChoosing: Boolean = false,
    playVideo: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
        modifier = Modifier.clip(MaterialTheme.shapes.large).then(modifier)
    ) {
        var hideContent by rememberStateOf(false)
        val alpha by animateFloatAsState(if (!hideContent) 1f else 0f, tween())
        val scale by animateFloatAsState(if (!hideContent) 1f else 1.125f, tween(DefaultDurationMillis * 2))
        var isSelectingText by rememberStateOf(false)
        var showSetCategory by rememberStateOf(false)
        var uploadJob by remember { mutableStateOf<Job?>(null) }
        var isUploadingVideo by rememberStateOf(false)
        var videoUploadProgress by remember { mutableStateOf(0f) }
        var videoUploadStage by remember { mutableStateOf(ProcessingVideoStage.Processing) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        LaunchedEffect(hideContent) {
            if (hideContent) {
                delay(2.seconds)
                hideContent = false
            }
        }

        if (isUploadingVideo) {
            ProcessingVideoDialog(
                onDismissRequest = { isUploadingVideo = false },
                onCancelRequest = { uploadJob?.cancel() },
                stage = videoUploadStage,
                progress = videoUploadProgress
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(.75f)
                .motionEventSpy {
                    if (it.action == MotionEvent.ACTION_UP) {
                        isSelectingText = false
                    }
                }
                .let {
                    if (onClick != null) {
                        it.combinedClickable(
                            enabled = !isSelectingText,
                            onClick = onClick,
                            onLongClick = {
                                hideContent = true
                            }
                        )
                    } else {
                        it
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            if (card != null) {
                if (card.video != null) {
                    Video(
                        card.video!!.let(api::url),
                        modifier = Modifier.matchParentSize().scale(scale).clip(MaterialTheme.shapes.large),
                        isPlaying = playVideo
                    )
                } else if (card.photo != null) {
                    card.photo?.also {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(api.url(it))
                                .crossfade(true)
                                .build(),
                            contentDescription = "",
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                            modifier = Modifier.matchParentSize().scale(scale)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                ShaderBrush(
                                    ImageShader(
                                        ImageBitmap.imageResource(R.drawable.bkg),
                                        tileModeX = TileMode.Repeated,
                                        tileModeY = TileMode.Repeated
                                    )
                                ),
                                alpha = .5f
                            )
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .alpha(alpha)
                        .padding(PaddingDefault)
                        .align(Alignment.TopEnd)
                ) {
                    if (isMine && isMineToolbar) {
                        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
                            if (it == null) return@rememberLauncherForActivityResult

                            uploadJob = scope.launch {
                                videoUploadProgress = 0f
                                if (it.isVideo(context)) {
                                    isUploadingVideo = true
                                    api.uploadCardVideo(
                                        card.id!!,
                                        it,
                                        context.contentResolver.getType(it) ?: "video/*",
                                        it.lastPathSegment ?: "video.${
                                            context.contentResolver.getType(it)?.split("/")?.lastOrNull() ?: ""
                                        }",
                                        processingCallback = {
                                            videoUploadStage = ProcessingVideoStage.Processing
                                            videoUploadProgress = it
                                        },
                                        uploadCallback = {
                                            videoUploadStage = ProcessingVideoStage.Uploading
                                            videoUploadProgress = it
                                        }
                                    )
                                } else if (it.isPhoto(context)) {
                                    api.uploadCardPhoto(card.id!!, it)
                                }
                                onChange()
                                isUploadingVideo = false
                                uploadJob = null
                            }
                        }
                        TextButton(
                            {
                                launcher.launch(PickVisualMediaRequest())
                            },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = MaterialTheme.colorScheme.background.copy(alpha = .8f)
                            ),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                        ) {
                            Icon(Icons.Outlined.Edit, "")
                            Text(
                                stringResource(R.string.set_photo),
                                modifier = Modifier.padding(start = PaddingDefault)
                            )
                        }
                    } else if (!isMine) {
                        IconButton({
                            scope.launch {
                                when (saves.toggleSave(card)) {
                                    ToggleSaveResult.Saved -> {
                                        context.toast(R.string.card_saved)
                                    }

                                    ToggleSaveResult.Unsaved -> {
                                        context.toast(R.string.card_unsaved)
                                    }

                                    else -> {
                                        context.showDidntWork()
                                    }
                                }
                            }
                        }) {
                            SavedIcon(card)
                        }
                    }

                    val hasCards = (card.cardCount ?: 0) > 0
                    val distanceText = showDistance?.let {
                        if (card.geo != null) {
                            it.distance(card.latLng!!).takeIf { it < nearbyMaxDistanceKm }?.let { metersAway ->
                                when {
                                    metersAway >= 1000f -> ceil(metersAway / 1000).toInt()
                                        .let { km -> pluralStringResource(R.plurals.km_away, km, km) }

                                    else -> metersAway.approximate(10)
                                        .let { meters -> pluralStringResource(R.plurals.meters_away, meters, meters) }
                                } + (if (hasCards) " • " else "")
                            } ?: (stringResource(R.string.your_friend) + (if (hasCards) " • " else ""))
                        } else {
                            stringResource(R.string.your_friend) + (if (hasCards) " • " else "")
                        }
                    }

                    if (hasCards || distanceText != null) {
                        Text(
                            (distanceText ?: "") + if (hasCards) pluralStringResource(
                                R.plurals.number_of_cards,
                                card.cardCount ?: 0,
                                card.cardCount ?: 0
                            ) else "",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.background.copy(alpha = .8f),
                                    MaterialTheme.shapes.extraLarge
                                )
                                .padding(vertical = PaddingDefault, horizontal = PaddingDefault * 2)
                        )
                    }
                }

                val conversationScrollState = rememberScrollState()
                var conversation by remember { mutableStateOf(emptyList<ConversationItem>()) }
                val recomposeScope = currentRecomposeScope

                LaunchedEffect(conversation) {
                    recomposeScope.invalidate()
                }

                ConstraintLayout(
                    modifier = Modifier
                        .alpha(alpha)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = .8f))
                        .animateContentSize(
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
//                                dampingRatio = Spring.DampingRatioLowBouncy,
                                visibilityThreshold = IntSize.VisibilityThreshold
                            )
                        )
                        .minAspectRatio(if (conversation.isNotEmpty()) .75f else 1.5f)
                        .padding(PaddingDefault * 2)
                ) {
                    val (conversationRef, toolbarRef) = createRefs()
                    var viewport by remember { mutableStateOf(Size(0f, 0f)) }

                    CardConversation(
                        card,
                        interactable = !isChoosing,
                        onReply = onReply,
                        isMine = isMine,
                        isMineToolbar = isMineToolbar,
                        selectingText = {
                            isSelectingText = it
                        },
                        conversationChange = {
                            conversation = it
                        },
                        onCategoryClick = {
                            if (isMine && isMineToolbar) {
                                showSetCategory = true
                            } else {
                                onCategoryClick(it)
                            }
                        },
                        onSetCategoryClick = {
                            showSetCategory = true
                        },
                        modifier = Modifier
                            .constrainAs(conversationRef) {
                                linkTo(parent.top, toolbarRef.top, bias = 1f)
                                height = Dimension.preferredWrapContent
                            }
                            .verticalScroll(conversationScrollState)
                            .onPlaced { viewport = it.boundsInParent().size }
                            .fadingEdge(viewport, conversationScrollState)
                    )

                    if (isMine && isMineToolbar && activity != null) {
                        CardToolbar(
                            activity,
                            onChange,
                            card,
                            edit,
                            modifier = Modifier.constrainAs(toolbarRef) {
                                linkTo(conversationRef.bottom, parent.bottom, bias = 1f)
                                height = Dimension.preferredWrapContent
                            }
                        )
                    }
                }

                if (showSetCategory) {
                    ChooseCategoryDialog(
                        {
                            showSetCategory = false
                        },
                        { category ->
                            scope.launch {
                                api.updateCard(
                                    card.id!!,
                                    Card().apply {
                                        categories = if (category == null) emptyList() else listOf(category)
                                    }
                                ) {
                                    onChange()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission", "UnrememberedMutableState")
@Composable
private fun CardToolbar(
    activity: Activity,
    onChange: () -> Unit,
    card: Card,
    edit: EditCard?,
    modifier: Modifier = Modifier,
) {
    var openDeleteDialog by rememberStateOf(false)
    var openEditDialog by remember { mutableStateOf(edit == EditCard.Conversation) }
    var openLocationDialog by remember { mutableStateOf(edit == EditCard.Location) }
    val scrollState = rememberScrollState()

    Row(
        horizontalArrangement = Arrangement.End,
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingValues(top = PaddingDefault))
            .horizontalScroll(scrollState, reverseScrolling = true)
    ) {
        var active by remember { mutableStateOf(card.active ?: false) }
        var activeCommitted by remember { mutableStateOf(active) }
        val coroutineScope = rememberCoroutineScope()

        Switch(
            active,
            {
                active = it
                coroutineScope.launch {
                    api.updateCard(card.id!!, Card(active = active)) {
                        card.active = it.active
                        activeCommitted = it.active ?: false
                        active = activeCommitted
                    }
                }
            }
        )
        Text(
            if (activeCommitted) stringResource(R.string.published) else stringResource(R.string.draft),
            style = MaterialTheme.typography.labelMedium,
            color = if (activeCommitted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = PaddingDefault)
        )
        Box(modifier = Modifier.weight(1f))
        IconButton({
            openLocationDialog = true
        }) {
            Icon(Icons.Outlined.Place, "")
        }
        IconButton({
            openEditDialog = true
        }) {
            Icon(Icons.Outlined.Edit, "")
        }
        IconButton({
            openDeleteDialog = true
        }) {
            Icon(Icons.Outlined.Delete, "", tint = MaterialTheme.colorScheme.error)
        }
    }

    if (openLocationDialog) {
        EditCardLocationDialog(card, activity, {
            openLocationDialog = false
        }, onChange)
    }

    if (openEditDialog) {
        EditCardDialog(card, {
            openEditDialog = false
        }, onChange)
    }

    if (openDeleteDialog) {
        DeleteCardDialog(card, {
            openDeleteDialog = false
        }, onChange)
    }
}

@Serializable
data class ConversationItem(
    var title: String = "",
    var message: String = "",
    var items: MutableList<ConversationItem> = mutableListOf(),
)

enum class CardParentType {
    Map,
    Card,
    Person,
    Offline
}

enum class EditCard {
    Conversation,
    Location
}


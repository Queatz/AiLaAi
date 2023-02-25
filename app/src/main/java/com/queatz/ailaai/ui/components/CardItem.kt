package com.queatz.ailaai.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import at.bluesource.choicesdk.maps.common.LatLng
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.dialogs.DeleteCardDialog
import com.queatz.ailaai.ui.dialogs.EditCardDialog
import com.queatz.ailaai.ui.dialogs.EditCardLocationDialog
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun BasicCard(
    onClick: () -> Unit,
    onReply: () -> Unit = {},
    onChange: () -> Unit = {},
    activity: Activity,
    card: Card,
    edit: Boolean = false,
    isMine: Boolean = false,
    isChoosing: Boolean = false
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        var hideContent by remember { mutableStateOf(false) }
        val alpha by animateFloatAsState(if (!hideContent) 1f else 0f, tween())
        val scale by animateFloatAsState(if (!hideContent) 1f else 1.125f, tween(DefaultDurationMillis * 2))
        var isSelectingText by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(hideContent) {
            if (hideContent) {
                delay(2.seconds)
                hideContent = false
            }
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
                .combinedClickable(
                    enabled = !isSelectingText,
                    onClick = {
                        onClick()
                    },
                    onLongClick = {
                        hideContent = true
                    }
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .alpha(alpha)
                    .padding(PaddingDefault)
                    .align(Alignment.TopEnd)
            ) {
                if (isMine) {
                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                        if (it == null) return@rememberLauncherForActivityResult

                        coroutineScope.launch {
                            try {
                                api.uploadCardPhoto(card.id!!, it)
                                onChange()
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    }
                    TextButton(
                        {
                            launcher.launch("image/*")
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                    ) {
                        Icon(Icons.Outlined.Edit, "")
                        Text(stringResource(R.string.set_photo), modifier = Modifier.padding(start = PaddingDefault))
                    }
                } else {
                    val context = LocalContext.current
                    IconButton({
                        coroutineScope.launch {
                            when (saves.toggleSave(card)) {
                                ToggleSaveResult.Saved -> {
                                    Toast.makeText(context, context.getString(R.string.card_saved), Toast.LENGTH_SHORT).show()
                                }
                                ToggleSaveResult.Unsaved -> {
                                    Toast.makeText(context, context.getString(R.string.card_unsaved), Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    Toast.makeText(context, context.getString(R.string.didnt_work), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        SavedIcon(card)
                    }
                }

                if ((card.cardCount ?: 0) > 0) {
                    Text(
                        pluralStringResource(R.plurals.number_of_cards, card.cardCount ?: 0, card.cardCount ?: 0),
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

            ConstraintLayout(
                modifier = Modifier
                    .alpha(alpha)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = .8f))
                    .padding(PaddingDefault * 2)
                    .minAspectRatio(1.5f)
                    .animateContentSize(
                        spring(
                            stiffness = Spring.StiffnessMediumLow,
                            visibilityThreshold = IntSize.VisibilityThreshold
                        )
                    )
            ) {
                val (topRef, bottomRef) = createRefs()
                var viewport by remember { mutableStateOf(Size(0f, 0f)) }

                CardConversation(
                    card,
                    interactable = !isChoosing,
                    onReply = onReply,
                    isMine = isMine,
                    selectingText = {
                        isSelectingText = it
                    },
                    modifier = Modifier
                        .constrainAs(topRef) {
                            bottom.linkTo(bottomRef.top)
                            top.linkTo(parent.top)
                            height = Dimension.preferredWrapContent
                        }
                        .verticalScroll(conversationScrollState)
                        .graphicsLayer(alpha = 0.99f)
                        .onPlaced { viewport = it.boundsInParent().size }
                        .fadingEdge(viewport, conversationScrollState)
                )

                if (isMine) {
                    CardToolbar(activity, onChange, card, edit, modifier = Modifier.constrainAs(bottomRef) {
                        top.linkTo(topRef.bottom)
                        bottom.linkTo(parent.bottom)
                    })
                }
            }
        }
    }
}

private fun Modifier.fadingEdge(viewport: Size, scrollState: ScrollState) = then(
    Modifier.drawWithContent {
        drawContent()

        val h = scrollState.value.toFloat().coerceAtMost(viewport.height / 3f)
        val h2 = (
                scrollState.maxValue.toFloat() - scrollState.value.toFloat()
                ).coerceAtMost(viewport.height / 3f)

        if (scrollState.value != 0) {
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = h + scrollState.value,
                    endY = 0.0f + scrollState.value
                ),
                blendMode = BlendMode.DstIn
            )
        }

        if (scrollState.value != scrollState.maxValue) {
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = viewport.height - h2 + scrollState.value,
                    endY = viewport.height + scrollState.value
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }
)

private fun Modifier.minAspectRatio(ratio: Float) = then(
    MaxAspectRatioModifier(ratio)
)

class MaxAspectRatioModifier(
    private val aspectRatio: Float
) : LayoutModifier {
    init {
        require(aspectRatio > 0f) { "aspectRatio $aspectRatio must be > 0" }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(
            constraints.copy(
                maxHeight = (constraints.maxWidth.toFloat() / aspectRatio).toInt()
            )
        )
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(IntOffset.Zero)
        }
    }
}

@SuppressLint("MissingPermission", "UnrememberedMutableState")
@Composable
private fun CardToolbar(
    activity: Activity,
    onChange: () -> Unit,
    card: Card,
    edit: Boolean,
    modifier: Modifier = Modifier
) {
    var openDeleteDialog by remember { mutableStateOf(false) }
    var openEditDialog by remember { mutableStateOf(false) }
    var openLocationDialog by remember { mutableStateOf(edit) }
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
                    try {
                        val update = api.updateCard(card.id!!, Card(active = active))
                        card.active = update.active
                        activeCommitted = update.active ?: false
                        active = activeCommitted
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            },
            enabled = card.photo != null
        )
        Text(
            if (activeCommitted) stringResource(R.string.card_active) else stringResource(R.string.card_inactive),
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

data class ConversationItem(
    var title: String = "",
    var message: String = "",
    var items: MutableList<ConversationItem> = mutableListOf()
)

fun LatLng.toList() = listOf(latitude, longitude)

enum class CardParentType {
    Map,
    Card,
    Person
}

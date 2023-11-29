package com.queatz.ailaai.ui.components

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.services.SavedIcon
import com.queatz.ailaai.services.ToggleSaveResult
import com.queatz.ailaai.services.saves
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardItem(
    onClick: (() -> Unit)?,
    onCategoryClick: (String) -> Unit = {},
    onReply: (List<String>) -> Unit = {},
    card: Card?,
    modifier: Modifier = Modifier,
    isChoosing: Boolean = false,
    playVideo: Boolean = true
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
    ) {
        var isSelectingText by rememberStateOf(false)
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
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
                        modifier = Modifier.matchParentSize().clip(MaterialTheme.shapes.large),
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
                            modifier = Modifier.matchParentSize()
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
                    horizontalArrangement = Arrangement.spacedBy(1.pad),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(1.pad)
                        .align(Alignment.TopEnd)
                ) {
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

                val conversationScrollState = rememberScrollState()
                var conversation by remember { mutableStateOf(emptyList<ConversationItem>()) }
                val recomposeScope = currentRecomposeScope

                LaunchedEffect(conversation) {
                    recomposeScope.invalidate()
                }

                Column(
                    modifier = Modifier
                        .padding(1.pad)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = .96f))
                        .animateContentSize(
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
//                                dampingRatio = Spring.DampingRatioLowBouncy,
                                visibilityThreshold = IntSize.VisibilityThreshold
                            )
                        )
                        .let {
                            if (isChoosing) {
                                it.minAspectRatio(1f)
                            } else {
                                it.minAspectRatio(if (conversation.isNotEmpty()) .75f else 1.5f)
                            }
                        }
                        .padding(1.5f.pad)
                ) {
                    var viewport by remember { mutableStateOf(Size(0f, 0f)) }
                    CardConversation(
                        card,
                        interactable = !isChoosing,
                        onReply = onReply,
                        selectingText = {
                            isSelectingText = it
                        },
                        conversationChange = {
                            conversation = it
                        },
                        onCategoryClick = {
                            onCategoryClick(it)
                        },
                        modifier = Modifier
                            .wrapContentHeight()
                            .weight(1f, fill = false)
                            .verticalScroll(conversationScrollState)
                            .onPlaced { viewport = it.boundsInParent().size }
                            .fadingEdge(viewport, conversationScrollState)
                    )
                }
            }
        }
    }
}

@Serializable
data class ConversationItem(
    var title: String = "",
    var message: String = "",
    var action: ConversationAction? = null,
    var items: MutableList<ConversationItem> = mutableListOf(),
)

@Serializable
data class CardOptions(
    var enableReplies: Boolean? = null,
    var enableAnonymousReplies: Boolean? = null
)

enum class ConversationAction {
    Message
}

enum class CardParentType {
    Map,
    Card,
    Group,
    Person,
    Offline
}

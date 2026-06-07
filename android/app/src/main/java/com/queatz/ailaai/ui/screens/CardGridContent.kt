package com.queatz.ailaai.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.nav
import com.queatz.ailaai.slideshow.slideshow
import com.queatz.ailaai.ui.components.CardLayout
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.dialogs.buildQrBitmap
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card

@Composable
fun CardGridContent(state: CardScreenState, showInFullscreen: Boolean) {
    val card = state.card
    val cards = state.cards
    val isMine = state.isMine
    val isMineOrIAmACollaborator = state.isMineOrIAmACollaborator
    val scope = state.scope
    val nav = nav
    val context = state.context
    val gridState = state.gridState
    val gridStateLandscape = state.gridStateLandscape
    val slideshow = slideshow
    val userIsInactive by slideshow.userIsInactive.collectAsState()
    val showScanMe = state.showScanMe

    if (state.isLoading) {
        Loading(
            modifier = Modifier.padding(vertical = 1.pad)
        )
    } else if (state.notFound) {
        Text(
            stringResource(R.string.card_not_found),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.pad)
        )
    } else {
        val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
        var playingVideo by remember(card) { mutableStateOf(card) }
        val isAtTop by gridState.isAtTop()
        val autoplayIndex by gridState.rememberAutoplayIndex()

        LaunchedEffect(autoplayIndex, isLandscape) {
            playingVideo = cards.getOrNull(
                (autoplayIndex - (if (isLandscape) 0 else 1)).coerceAtLeast(0)
            )
        }

        LaunchedEffect(card, isAtTop) {
            if (isAtTop) {
                playingVideo = card
            }
        }

        Box {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLandscape) {
                    LazyVerticalGrid(
                        state = gridStateLandscape,
                        contentPadding = PaddingValues(1.pad),
                        horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.Top),
                        columns = GridCells.Fixed(1),
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                    ) {
                        cardHeaderItem(
                            state = state,
                            aspect = 1.5f,
                            showInFullscreen = showInFullscreen,
                            elevation = 2,
                            playVideo = playingVideo == card && isAtTop,
                            onVideoClick = {
                                playingVideo = null
                            }
                        )
                    }
                }

                LazyVerticalGrid(
                    state = gridState,
                    contentPadding = PaddingValues(1.pad),
                    horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.Top),
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(240.dp)
                ) {
                    if (!isLandscape) {
                        cardHeaderItem(
                            state = state,
                            aspect = 1.5f,
                            showInFullscreen = showInFullscreen,
                            playVideo = playingVideo == card && isAtTop,
                            onVideoClick = {
                                playingVideo = null
                            }
                        )
                    }
                    if (cards.isNotEmpty()) {
                        items(cards, key = { it.id!! }) {
                            CardLayout(
                                card = it,
                                showTitle = true,
                                hideCreators = card?.person?.inList()
                                    ?.let { creators -> creators + (card.collaborators ?: emptyList()) },
                                onClick = {
                                    nav.appNavigate(AppNav.Page(it.id!!))
                                },
                                scope = scope,
                                playVideo = playingVideo == it && !isAtTop,
                            )
                        }
                    }
                }
            }
            if (isMineOrIAmACollaborator) {
                FloatingActionButton(
                    onClick = {
                        state.newCard = Card(parent = state.cardId)
                    },
                    modifier = Modifier
                        .padding(2.pad)
                        .align(Alignment.BottomEnd)
                ) {
                    Icon(Icons.Outlined.Add, stringResource(R.string.add_a_card))
                }
            }

            if (showInFullscreen) {
                val logo = bitmapResource(R.drawable.ic_notification)
                val size = 220.dp.px
                val qrCode = remember(state.cardId) {
                    cardUrl(state.cardId).buildQrBitmap(logo, size)
                }

                Crossfade(
                    targetState = userIsInactive || showScanMe,
                    modifier = Modifier
                        .align(if (isLandscape) Alignment.BottomEnd else Alignment.BottomStart)
                        .padding(1.pad)
                ) { targetState ->
                    if (targetState) {
                        Box(
                            modifier = Modifier
                                .shadow(16.dp, MaterialTheme.shapes.medium)
                                .clip(MaterialTheme.shapes.medium)
                                .background(Color.White)
                                .padding(.5f.pad)
                        ) {
                            Image(
                                qrCode.asImageBitmap(),
                                contentDescription = null
                            )

                            if (showScanMe) {
                                Text(
                                    text = stringResource(R.string.scan_me),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Black
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .rotate(-45f)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(Color.White)
                                        .padding(.5f.pad)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

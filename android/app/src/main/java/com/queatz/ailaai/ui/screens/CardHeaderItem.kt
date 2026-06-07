package com.queatz.ailaai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.ailaai.api.updateCard
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.CardLayout
import com.queatz.ailaai.ui.components.Toolbar
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import kotlinx.coroutines.launch

fun LazyGridScope.cardHeaderItem(
    state: CardScreenState,
    aspect: Float,
    showInFullscreen: Boolean,
    elevation: Int = 1,
    playVideo: Boolean = false,
    onVideoClick: () -> Unit
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        CardHeaderItemContent(state, aspect, showInFullscreen, elevation, playVideo, onVideoClick)
    }
}

@Composable
private fun CardHeaderItemContent(
    state: CardScreenState,
    aspect: Float,
    showInFullscreen: Boolean,
    elevation: Int = 1,
    playVideo: Boolean = false,
    onVideoClick: () -> Unit
) {
    val card = state.card
    val isMine = state.isMine
    val scope = state.scope
    val nav = nav

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (card != null && isMine) {
            var active by remember { mutableStateOf(card.active ?: false) }
            var activeCommitted by remember { mutableStateOf(active) }

            AnimatedVisibility(state.showTools) {
                Toolbar(
                    modifier = Modifier.padding(bottom = 1.pad)
                ) {
                    item(
                        icon = if (active) Icons.Outlined.ToggleOn else Icons.Outlined.ToggleOff,
                        name = if (activeCommitted) stringResource(R.string.posted) else stringResource(R.string.not_posted),
                        selected = active
                    ) {
                        active = !active
                        scope.launch {
                            api.updateCard(card.id!!, Card(active = active)) {
                                card.active = it.active
                                activeCommitted = it.active ?: false
                                active = activeCommitted
                            }
                        }
                    }

                    item(
                        icon = Icons.Outlined.Place,
                        name = when {
                            card.parent != null -> stringResource(R.string.inside_another_card)
                            card.group != null -> stringResource(R.string.in_a_group)
                            card.equipped == true -> stringResource(R.string.on_profile)
                            card.geo != null -> stringResource(R.string.at_a_location)
                            card.offline == true -> stringResource(R.string.offline)
                            else -> stringResource(R.string.none)
                        },
                        selected = when {
                            card.parent != null -> true
                            card.group != null -> true
                            card.equipped == true -> true
                            card.geo != null -> true
                            card.offline == true -> true
                            else -> false
                        }
                    ) {
                        state.openLocationDialog = true
                    }

                    item(
                        icon = Icons.Outlined.Edit,
                        name = stringResource(R.string.edit)
                    ) {
                        state.openEditDialog = true
                    }

                    item(
                        icon = Icons.Outlined.CameraAlt,
                        name = stringResource(R.string.set_photo),
                        isLoading = state.isGeneratingPhoto
                    ) {
                        state.showPhotoDialog = true
                    }

                    if (card.photo.isNullOrEmpty() && card.video.isNullOrEmpty()) {
                        item(
                            icon = Icons.Outlined.AutoAwesome,
                            name = stringResource(R.string.generate_photo),
                            isLoading = state.isRegeneratingPhoto
                        ) {
                            state.regeneratePhoto()
                            state.showMenu = false
                        }
                    }

                    item(
                        icon = Icons.Outlined.Wallpaper,
                        name = stringResource(R.string.background),
                        selected = !card.background.isNullOrBlank(),
                        isLoading = state.isGeneratingBackground
                    ) {
                        state.showBackgroundDialog = true
                    }

                    val category = card.categories?.firstOrNull()
                    item(
                        icon = Icons.Outlined.Category,
                        name = category ?: stringResource(R.string.set_category),
                        selected = category != null
                    ) {
                        state.showSetCategory = true
                        state.showMenu = false
                    }

                    item(
                        icon = Icons.Outlined.Payments,
                        name = stringResource(if (card.pay == null) R.string.add_pay else R.string.change_pay),
                        selected = card.pay != null
                    ) {
                        state.showPay = true
                        state.showMenu = false
                    }

                    item(
                        icon = Icons.Outlined.Man,
                        name = stringResource(if (card.npc == null) R.string.add_npc else R.string.npc),
                        selected = card.npc != null
                    ) {
                        state.showNpc = true
                        state.showMenu = false
                    }

                    item(
                        icon = Icons.Outlined.AddBox,
                        name = stringResource(if (card.content?.notBlank == null) R.string.add_content else R.string.content),
                        selected = card.content?.notBlank != null
                    ) {
                        nav.appNavigate(AppNav.EditCard(card.id!!))
                    }

                    val level = card.level ?: 0

                    item(
                        icon = Icons.Outlined.Castle,
                        name = if (level == 0) stringResource(R.string.upgrade) else pluralStringResource(
                            id = R.plurals.level_x,
                            count = level,
                            level
                        ),
                        selected = level > 0
                    ) {
                        state.showUpgradeDialog = true
                    }

                    val sizeInKm = card.size ?: 0.0

                    item(
                        icon = Icons.Outlined.Adjust,
                        name = if (sizeInKm == 0.0) stringResource(R.string.size) else {
                            pluralStringResource(
                                id = R.plurals.x_km,
                                count = sizeInKm.toInt(),
                                sizeInKm.format()
                            )
                        },
                        selected = sizeInKm > 0.0
                    ) {
                        state.showPageSizeDialog = true
                    }

                    item(
                        icon = Icons.Outlined.AddBox,
                        name = stringResource(R.string.add_to_homescreen)
                    ) {
                        state.addToHomescreen()
                    }
                }
            }
        }

        if (card != null) {
            CardLayout(
                card = card,
                showTitle = showInFullscreen,
                largeTitle = showInFullscreen,
                showAuthors = !showInFullscreen,
                aspect = aspect,
                scope = scope,
                elevation = elevation,
                playVideo = playVideo,
                onClick = {
                    if (!card.video.isNullOrBlank()) {
                        onVideoClick()
                    }
                },
                onReply = if (showInFullscreen) {
                    { conversation ->
                        state.showScanMe = true
                    }
                } else {
                    null
                }
            )
        }
    }
}

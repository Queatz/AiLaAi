package com.queatz.ailaai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.services.SavedIcon
import com.queatz.ailaai.services.ToggleSaveResult
import com.queatz.ailaai.services.saves
import com.queatz.ailaai.slideshow.slideshow
import com.queatz.ailaai.ui.components.AppBar
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.Dropdown
import kotlinx.coroutines.launch

@Composable
fun CardAppBar(state: CardScreenState, showInFullscreen: Boolean) {
    val card = state.card
    val isMine = state.isMine
    val context = state.context
    val scope = state.scope
    val nav = nav
    val me = me
    val slideshow = slideshow
    val fullscreen by slideshow.fullscreen.collectAsState()
    val userIsInactive by slideshow.userIsInactive.collectAsState()
    val userIsActive = !fullscreen && !userIsInactive
    val showScanMe = state.showScanMe

    val showAppBar = (!showInFullscreen || userIsActive) && !showScanMe

    AnimatedVisibility(showAppBar) {
        AppBar(
            title = {
                if (!showInFullscreen) {
                    Column {
                        Text(
                            card?.name ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        card?.hint?.notBlank?.let {
                            Text(
                                text = it,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                if (!showInFullscreen) {
                    BackButton()
                }
            },
            actions = {
                card?.let { card ->
                    if (!showInFullscreen) {
                        if (isMine) {
                            IconButton({
                                state.toggleShowTools()
                            }) {
                                Icon(
                                    if (state.showTools) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    stringResource(R.string.tools)
                                )
                            }
                        }
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
                }

                IconButton({
                    state.showMenu = !state.showMenu
                }) {
                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.more))
                }

                val cardString = stringResource(R.string.card)

                Dropdown(state.showMenu, { state.showMenu = false }) {
                    card?.let { card ->
                        DropdownMenuItem({
                            Text(stringResource(R.string.view_profile))
                        }, {
                            nav.appNavigate(AppNav.Profile(card.person!!))
                            state.showMenu = false
                        })
                        DropdownMenuItem({
                            Text(stringResource(R.string.toggle_fullscreen))
                        }, {
                            slideshow.setFullscreen(!fullscreen)
                            state.showMenu = false
                        })
                        if (isMine || card.collaborators?.contains(me?.id) == true) {
                            DropdownMenuItem({
                                Text(stringResource(R.string.manage))
                            }, {
                                state.showManageMenu = true
                                state.showMenu = false
                            })
                            DropdownMenuItem({
                                Text(stringResource(R.string.additional_photos))
                            }, {
                                state.showAdditionalPhotosDialog = true
                                state.showMenu = false
                            })
                            DropdownMenuItem({
                                Text(stringResource(R.string.statistics))
                            }, {
                                state.showStatisticsDialog = true
                                state.showMenu = false
                            })
                            DropdownMenuItem({
                                Text(stringResource(R.string.qr_code))
                            }, {
                                state.showQrCode = true
                                state.showMenu = false
                            })
                            DropdownMenuItem({
                                Text(stringResource(R.string.send))
                            }, {
                                state.showSendDialog = true
                                state.showMenu = false
                            })
                            DropdownMenuItem({
                                Text(stringResource(R.string.collaborators))
                            }, {
                                state.fetchCollaborators()
                                state.openCollaboratorsDialog = true
                                state.showMenu = false
                            })
                        }
                        if (isMine) {
                            DropdownMenuItem({
                                Text(stringResource(R.string.upgrade))
                            }, {
                                state.showUpgradeDialog = true
                                state.showMenu = false
                            })
                        }
                        if (card.person != me?.id && card.collaborators?.contains(me?.id) == true) {
                            DropdownMenuItem({
                                Text(stringResource(R.string.leave))
                            }, {
                                state.openLeaveCollaboratorsDialog = true
                                state.showMenu = false
                            })
                        }
                        DropdownMenuItem({
                            Text(stringResource(R.string.share))
                        }, {
                            card.idOrUrl.shareAsUrl(context, card.name ?: cardString)
                            state.showMenu = false
                        })
                        DropdownMenuItem({
                            Text(stringResource(R.string.copy_link))
                        }, {
                            card.idOrUrl.copyToClipboard(context, card.name ?: cardString)
                            context.toast(context.getString(R.string.copied))
                            state.showMenu = false
                        })
                        DropdownMenuItem({
                            Text(stringResource(R.string.view_source))
                        }, {
                            state.showMenu = false
                            state.showSourceDialog = true
                        })
                        if (androidx.core.content.pm.ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                            DropdownMenuItem({
                                Text(stringResource(R.string.add_to_homescreen))
                            }, {
                                state.showMenu = false
                                state.addToHomescreen()
                            })
                        }
                        DropdownMenuItem({
                            Text(stringResource(R.string.report))
                        }, {
                            state.showReportDialog = true
                            state.showMenu = false
                        })
                    }
                }
            },
            modifier = Modifier.zIndex(1f)
        )
    }
}

package com.queatz.ailaai.services

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.api.saveCard
import com.queatz.ailaai.api.savedCards
import com.queatz.ailaai.api.unsaveCard
import com.queatz.ailaai.data.Card
import com.queatz.ailaai.data.api
import kotlinx.coroutines.flow.MutableSharedFlow

val saves by lazy {
    Saves()
}

class Saves {

    val changes = MutableSharedFlow<Unit>()

    private val savedIds = mutableSetOf<String>()

    fun isSaved(card: Card) = savedIds.contains(card.id!!)

    suspend fun reload() {
        api.savedCards {
            savedIds.clear()
            savedIds.addAll(it.mapNotNull { it.card?.id })
            changes.emit(Unit)
        }
    }

    suspend fun toggleSave(card: Card): ToggleSaveResult {
        var hasError = false
        if (!isSaved(card)) {
            api.saveCard(card.id!!, onError = { hasError = true })
            savedIds.add(card.id!!)
            changes.emit(Unit)
            if (!hasError) {
                return ToggleSaveResult.Saved
            }
        } else {
            api.unsaveCard(card.id!!, onError = { hasError = true })
            savedIds.remove(card.id!!)
            changes.emit(Unit)
            if (!hasError) {
                return ToggleSaveResult.Unsaved
            }
        }

        return ToggleSaveResult.Error
    }
}


@Composable
fun SavedIcon(card: Card) {
    val recomposeScope = currentRecomposeScope
    LaunchedEffect(Unit) {
        saves.changes.collect {
            recomposeScope.invalidate()
        }
    }

    if (saves.isSaved(card)) {
        Icon(
            Icons.Outlined.Favorite,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = stringResource(id = R.string.unsave_card),
        )
    } else {
        Icon(
            Icons.Outlined.FavoriteBorder,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = stringResource(id = R.string.save_card),
        )
    }
}

enum class ToggleSaveResult {
    Saved,
    Unsaved,
    Error
}

package com.queatz.ailaai

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.MutableSharedFlow

val saves = Saves()

class Saves {

    val changes = MutableSharedFlow<Unit>()

    private val savedIds = mutableSetOf<String>()

    fun isSaved(card: Card) = savedIds.contains(card.id!!)

    suspend fun reload() {
        try {
            val saves = api.savedCards().mapNotNull { it.card?.id }
            savedIds.clear()
            savedIds.addAll(saves)
            changes.emit(Unit)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    suspend fun toggleSave(card: Card): ToggleSaveResult {
        return try {
            if (!isSaved(card)) {
                api.saveCard(card.id!!)
                savedIds.add(card.id!!)
                changes.emit(Unit)
                ToggleSaveResult.Saved
            } else {
                api.unsaveCard(card.id!!)
                savedIds.remove(card.id!!)
                changes.emit(Unit)
                ToggleSaveResult.Unsaved
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            ToggleSaveResult.Error
        }
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

package com.queatz.ailaai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.queatz.ailaai.api.stickerPacks
import kotlinx.coroutines.flow.MutableStateFlow

val stickers = Stickers()

class Stickers {

    private val stickerPacks = MutableStateFlow<List<StickerPack>?>(null)

    @Composable
    fun rememberStickerPacks(): State<List<StickerPack>?> = stickerPacks.collectAsState()

    suspend fun reload() {
        try {
            stickerPacks.value = api.stickerPacks()
        } catch (e: Exception) {
            e.printStackTrace()
            // todo retry
        }
    }

    @Composable
    fun has(stickerPackId: String): Boolean {
        return rememberStickerPacks().value?.any { it.id == stickerPackId } ?: false
    }
}

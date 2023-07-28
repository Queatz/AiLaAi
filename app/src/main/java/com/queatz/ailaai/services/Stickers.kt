package com.queatz.ailaai.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.queatz.ailaai.api.stickerPacks
import com.queatz.ailaai.data.StickerPack
import com.queatz.ailaai.data.api
import kotlinx.coroutines.flow.MutableStateFlow

val stickers by lazy {
    Stickers()
}

class Stickers {

    private val stickerPacks = MutableStateFlow<List<StickerPack>?>(null)

    @Composable
    fun rememberStickerPacks(): State<List<StickerPack>?> = stickerPacks.collectAsState()

    suspend fun reload() {
        api.stickerPacks {
            stickerPacks.value = it
        }
    }

    @Composable
    fun has(stickerPackId: String): Boolean {
        return rememberStickerPacks().value?.any { it.id == stickerPackId } ?: false
    }
}

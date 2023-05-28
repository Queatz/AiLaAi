package com.queatz.ailaai.ui.stickers

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.StickerPack
import com.queatz.ailaai.api
import com.queatz.ailaai.api.saveStickerPack
import com.queatz.ailaai.api.unsaveStickerPack
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.stickers
import kotlinx.coroutines.launch

@Composable
fun UseStickerPackButton(stickerPack: StickerPack, onChange: (saved: Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isLoading by rememberStateOf(false)
    if (stickers.has(stickerPack.id!!)) {
        OutlinedButton(
            {
                scope.launch {
                    try {
                        api.unsaveStickerPack(stickerPack.id!!)
                        onChange(false)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        context.showDidntWork()
                    }
                }
            },
            enabled = !isLoading
        ) {
            Text(stringResource(R.string.remove))
        }
    } else {
        Button(
            {
                scope.launch {
                    try {
                        api.saveStickerPack(stickerPack.id!!)
                        onChange(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        context.showDidntWork()
                    }
                }
            },
            enabled = !isLoading && !stickerPack.stickers.isNullOrEmpty()
        ) {
            Text(stringResource(R.string.use_stickers))
        }
    }
}

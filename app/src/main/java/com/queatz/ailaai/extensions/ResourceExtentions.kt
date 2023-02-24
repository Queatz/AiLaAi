package com.queatz.ailaai.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun bitmapResource(drawableResId: Int): Bitmap? {
    val option = BitmapFactory.Options()
    option.inPreferredConfig = Bitmap.Config.ARGB_8888
    return BitmapFactory.decodeResource(
        LocalContext.current.resources,
        drawableResId,
        option
    )
}

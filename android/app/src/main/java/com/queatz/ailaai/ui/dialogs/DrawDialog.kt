package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.draw.DrawBox
import com.queatz.ailaai.draw.rememberDrawController
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.theme.pad
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeApi::class)
@Composable
fun DrawDialog(onDismissRequest: () -> Unit, onSubmit: (ImageBitmap) -> Unit) {
    val scope = rememberCoroutineScope()
    val drawController = rememberDrawController()
    val captureController = rememberCaptureController()
    val context = LocalContext.current

    fun done() {
        scope.launch {
            val bitmapAsync = captureController.captureAsync()
            try {
                onSubmit(bitmapAsync.await())
            } catch (error: Throwable) {
                error.printStackTrace()
                context.showDidntWork()
            }
        }
    }

    LaunchedEffect(Unit) {
        drawController.changeColor(Black)
    }

    DialogBase(
        onDismissRequest
    ) {
        DialogLayout(
            scrollable = false,
            padding = 1.pad,
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    DrawBox(
                        drawController = drawController,
                        backgroundColor = White,
                        modifier = Modifier
                            .capturable(captureController)
                            .clip(MaterialTheme.shapes.medium)
                            .fillMaxSize()
                    )
                }
            },
            actions = {
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
                TextButton(
                    {
                        done()
                    }
                ) {
                    Text(stringResource(R.string.send))
                }
            }
        )
    }
}

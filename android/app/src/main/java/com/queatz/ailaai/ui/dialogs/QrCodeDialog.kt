package com.queatz.ailaai.ui.dialogs

import android.graphics.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsBuildBitmapOption
import com.huawei.hms.ml.scan.HmsScanBase.QRCODE_SCAN_TYPE
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.bitmapResource
import com.queatz.ailaai.extensions.share
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.theme.pad
import kotlinx.coroutines.launch

fun String.buildQrBitmap(logo: Bitmap? = null, size: Int = 500) = ScanUtil.buildBitmap(
    this,
    QRCODE_SCAN_TYPE,
    size,
    size,
    HmsBuildBitmapOption.Creator().setQRLogoBitmap(logo?.tint(android.graphics.Color.BLACK)).create()
)!!

@Composable
fun QrCodeDialog(
    onDismissRequest: () -> Unit,
    url: String,
    name: String?,
    title: String? = null,
) {
    val scope = rememberCoroutineScope()
    val logo = bitmapResource(R.drawable.ic_notification)
    val qrCode = remember {
        url.buildQrBitmap(logo)
    }

    DialogBase(onDismissRequest) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(
                    3.pad,
                    3.pad,
                    3.pad,
                    1.pad
                )
        ) {
            title?.let { title ->
                Text(
                    text = title,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 1.pad)
                )
            }
            Image(qrCode.asImageBitmap(), contentDescription = null, modifier = Modifier.background(Color.White))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                val context = LocalContext.current
                TextButton(
                    {
                        scope.launch {
                            qrCode.share(context, name)
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(stringResource(R.string.share), textAlign = TextAlign.End)
                }
            }
        }
    }
}

fun Bitmap.tint(color: Int): Bitmap {
    val paint = Paint()
    paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    val bitmapResult: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmapResult)
    canvas.drawBitmap(this, 0f, 0f, paint)
    return bitmapResult
}

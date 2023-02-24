package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsBuildBitmapOption
import com.huawei.hms.ml.scan.HmsScanBase.QRCODE_SCAN_TYPE
import com.queatz.ailaai.Card
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.bitmapResource
import com.queatz.ailaai.extensions.url
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun ShareCardQrCodeDialog(onDismissRequest: () -> Unit, card: Card) {
    val logo = bitmapResource(R.drawable.ic_notification)
    val qrCode = remember { ScanUtil.buildBitmap(
        card.url,
        QRCODE_SCAN_TYPE,
        500,
        500,
        HmsBuildBitmapOption.Creator().setQRLogoBitmap(logo).create()
    ) }

    Dialog(onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .padding(PaddingDefault * 2)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(PaddingDefault * 3)
            ) {
                Image(qrCode.asImageBitmap(), contentDescription = null)
            }
        }
    }
}

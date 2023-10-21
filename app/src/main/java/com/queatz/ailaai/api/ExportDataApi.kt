package com.queatz.ailaai.api

import com.queatz.ailaai.data.Api
import com.queatz.ailaai.data.ErrorBlock
import com.queatz.ailaai.data.SuccessBlock
import com.queatz.db.ExportDataResponse

suspend fun Api.exportData(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<ExportDataResponse> = {}
) = get("export-data", onError = onError, onSuccess = onSuccess)

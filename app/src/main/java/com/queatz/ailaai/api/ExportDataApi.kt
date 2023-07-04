package com.queatz.ailaai.api

import com.queatz.ailaai.*

suspend fun Api.exportData(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<ExportDataResponse> = {}
) = get("export-data", onError = onError, onSuccess = onSuccess)

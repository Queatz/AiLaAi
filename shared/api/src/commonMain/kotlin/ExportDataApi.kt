package app.ailaai.api

import com.queatz.db.ExportDataResponse

suspend fun Api.exportData(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<ExportDataResponse> = {}
) = get("export-data", onError = onError, onSuccess = onSuccess)

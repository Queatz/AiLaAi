package app.ailaai.api

import com.queatz.db.ChipPreviewRequest
import com.queatz.db.ChipPreviewResponse
import com.queatz.db.ImportChipsBody
import com.queatz.db.PersonChip
import com.queatz.db.PersonChipExtended
import com.queatz.db.ReorderChipsBody
import io.ktor.http.HttpStatusCode

suspend fun Api.chips(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<PersonChipExtended>> = {},
) = get(
    url = "chips",
    errorContext = "load chips",
    onError = onError,
    onSuccess = onSuccess,
)

suspend fun Api.createChip(
    chip: PersonChip,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<PersonChipExtended> = {},
) = post(
    url = "chips",
    body = chip,
    errorContext = "create chip",
    onError = onError,
    onSuccess = onSuccess,
)

suspend fun Api.importChips(
    body: ImportChipsBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<PersonChipExtended>> = {},
) = post(
    url = "chips/import",
    body = body,
    errorContext = "import chips",
    onError = onError,
    onSuccess = onSuccess,
)

suspend fun Api.updateChip(
    id: String,
    chip: PersonChip,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<PersonChipExtended> = {},
) = post(
    url = "chips/$id",
    body = chip,
    errorContext = "update chip",
    onError = onError,
    onSuccess = onSuccess,
)

suspend fun Api.deleteChip(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post(
    url = "chips/$id/delete",
    errorContext = "delete chip",
    onError = onError,
    onSuccess = onSuccess,
)

suspend fun Api.syncChip(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<PersonChipExtended> = {},
) = post(
    url = "chips/$id/sync",
    errorContext = "sync chip",
    onError = onError,
    onSuccess = onSuccess,
)

suspend fun Api.reorderChips(
    body: ReorderChipsBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<PersonChipExtended>> = {},
) = post(
    url = "chips/reorder",
    body = body,
    errorContext = "reorder chips",
    onError = onError,
    onSuccess = onSuccess,
)

suspend fun Api.previewChip(
    url: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<ChipPreviewResponse> = {},
) = post(
    url = "chips/preview",
    body = ChipPreviewRequest(url = url),
    errorContext = "preview chip",
    onError = onError,
    onSuccess = onSuccess,
)

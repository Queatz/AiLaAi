package app.ailaai.api

import com.queatz.db.RunScriptBody
import com.queatz.db.Script
import com.queatz.db.ScriptData
import com.queatz.db.ScriptResult
import com.queatz.db.ScriptStats
import io.ktor.http.HttpStatusCode

suspend fun Api.scripts(
    search: String? = null,
    offset: Int = 0,
    limit: Int = 20,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Script>>,
) = get(
    "scripts",
    mapOf(
        "search" to search,
        "offset" to offset.toString(),
        "limit" to limit.toString(),
    ),
    errorContext = "load scripts",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.myScripts(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Script>>,
) = get(
    "me/scripts",
    errorContext = "load my scripts",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.script(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Script>,
) = get(
    "scripts/$id",
    errorContext = "load script",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.createScript(
    script: Script,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Script>,
) = post(
    "scripts",
    body = script,
    errorContext = "create script",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateScript(
    id: String,
    script: Script,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Script>,
) = post(
    "scripts/$id",
    body = script,
    errorContext = "update script",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.runScript(
    id: String,
    data: RunScriptBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<ScriptResult>,
) = post(
    "scripts/$id/run",
    body = data,
    errorContext = "run script",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteScript(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post(
    "scripts/$id/delete",
    errorContext = "delete script",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.scriptData(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<ScriptData>,
) = get(
    "scripts/$id/data",
    errorContext = "load script data",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateScriptData(
    id: String,
    scriptData: ScriptData,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<ScriptData>,
) = post(
    "scripts/$id/data",
    body = scriptData,
    errorContext = "update script data",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.pinScript(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post("scripts/$id/pin", errorContext = "pin script", onError = onError, onSuccess = onSuccess)

suspend fun Api.unpinScript(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post("scripts/$id/unpin", errorContext = "unpin script", onError = onError, onSuccess = onSuccess)

suspend fun Api.scriptStats(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<ScriptStats>,
) = get(
    "scripts/$id/stats",
    errorContext = "load script stats",
    onError = onError,
    onSuccess = onSuccess
)

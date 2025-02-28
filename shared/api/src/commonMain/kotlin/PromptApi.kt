package app.ailaai.api

import com.queatz.db.Prompt

suspend fun Api.prompts(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Prompt>>,
) = get("prompts", onError = onError, onSuccess = onSuccess)

suspend fun Api.addPrompt(
    prompt: Prompt,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Prompt>,
) = post("prompts", body = prompt, onError = onError, onSuccess = onSuccess)

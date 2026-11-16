package app.ailaai.api

import com.queatz.db.Prompt
import com.queatz.db.PromptContext

suspend fun Api.prompts(
    context: PromptContext? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Prompt>>,
) = get(
    url = "prompts",
    parameters = context?.let {
        mapOf("context" to it.toString())
    },
    errorContext = "load prompts",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.addPrompt(
    prompt: Prompt,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Prompt>,
) = post(
    "prompts",
    body = prompt,
    errorContext = "save prompt",
    onError = onError,
    onSuccess = onSuccess
)

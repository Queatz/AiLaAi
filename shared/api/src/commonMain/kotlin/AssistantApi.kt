package app.ailaai.api

import com.queatz.db.AiTranscribeResponse
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

suspend fun Api.aiAssistantTranscribe(
    audio: ByteArray,
    language: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<AiTranscribeResponse>,
) = post(
    url = "ai/assistant/transcribe?language=$language",
    body = MultiPartFormDataContent(
        formData {
            append(
                key = "audio",
                value = audio,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, "audio/wav")
                    append(HttpHeaders.ContentDisposition, "filename=assistant-recording.wav")
                }
            )
        }
    ),
    onError = onError,
    onSuccess = onSuccess
)
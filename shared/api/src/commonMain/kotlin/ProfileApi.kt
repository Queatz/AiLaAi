import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import io.ktor.client.request.forms.*
import io.ktor.http.*

suspend fun Api.uploadProfileContentPhotos(
    profile: String,
    media: List<ByteArray>,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<String>>
) {
    return post(
        "profile/$profile/content/photos",
        MultiPartFormDataContent(
            formData {
                media.forEachIndexed { index, photo ->
                    append(
                        "photo[$index]",
                        photo,
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
                        }
                    )
                }
            }
        ),
        client = httpDataClient,
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.uploadProfileContentAudio(
    profile: String,
    audio: InputProvider,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<String>
) {
    return post(
        "profile/$profile/content/audio",
        MultiPartFormDataContent(
            formData {
                append(
                    "audio",
                    audio,
                    Headers.build {
                        append(HttpHeaders.ContentType, "audio/mp4")
                        append(HttpHeaders.ContentDisposition, "filename=audio.m4a")
                    }
                )
            }
        ),
        client = httpDataClient,
        onError = onError,
        onSuccess = onSuccess
    )
}

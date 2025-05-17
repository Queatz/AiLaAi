package app.ailaai.api

import com.queatz.db.UploadResponse
import io.ktor.client.request.forms.*
import io.ktor.http.*

suspend fun Api.uploadPhotos(
    photos: List<ByteArray>,
    removeBackground: Boolean = false,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<UploadResponse>,
) {
    return post(
        "upload/photos", MultiPartFormDataContent(
        formData {
            if (removeBackground) {
                append("removeBackground", true)
            }

            photos.forEachIndexed { index, photo ->
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
    ), client = httpDataClient, onError = onError, onSuccess = onSuccess)
}

suspend fun Api.uploadVideo(
    video: InputProvider,
    contentType: String,
    filename: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<UploadResponse>,
) {
    return post(
        "upload/video", MultiPartFormDataContent(
        formData {
            append(
                "video",
                video,
                Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "filename=${filename}")
                }
            )
        }
    ), client = httpDataClient, onError = onError, onSuccess = onSuccess)
}

suspend fun Api.uploadAudio(
    audio: InputProvider,
    contentType: String = "audio/mp4",
    filename: String = "audio.m4a",
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<UploadResponse>,
) {
    return post(
        "upload/audio", MultiPartFormDataContent(
        formData {
            append(
                "audio",
                audio,
                Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "filename=${filename}")
                }
            )
        }
    ), client = httpDataClient, onError = onError, onSuccess = onSuccess)
}

suspend fun Api.uploadAudio(
    audio: ByteArray,
    contentType: String = "audio/mp4",
    filename: String = "audio.m4a",
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<UploadResponse>,
) {
    return post(
        "upload/audio", MultiPartFormDataContent(
        formData {
            append(
                "audio",
                audio,
                Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "filename=${filename}")
                }
            )
        }
    ), client = httpDataClient, onError = onError, onSuccess = onSuccess)
}

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
    return post("upload/photos", MultiPartFormDataContent(
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

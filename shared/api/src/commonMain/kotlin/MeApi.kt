package app.ailaai.api

import com.queatz.db.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

suspend fun Api.myTopReactions(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<ReactionCount>>,
) = get("me/reactions/top", errorContext = "load reactions", onError = onError, onSuccess = onSuccess)

suspend fun Api.me(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Person>,
) = get("me", errorContext = "load profile", onError = onError, onSuccess = onSuccess)

suspend fun Api.transferCode(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Transfer>,
) = get("me/transfer", errorContext = "load transfer code", onError = onError, onSuccess = onSuccess)

suspend fun Api.refreshTransferCode(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Transfer>,
) = post("me/transfer/refresh", errorContext = "refresh transfer code", onError = onError, onSuccess = onSuccess)

suspend fun Api.myGeo(
    geo: Geo,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post("me/geo", geo.toList(), errorContext = "update location", onError = onError, onSuccess = onSuccess)

suspend fun Api.myDevice(
    deviceType: DeviceType,
    deviceToken: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post("me/device", MyDevice(deviceType, deviceToken), errorContext = "register device", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateMe(
    person: Person,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Person> = {},
) = post("me", person, errorContext = "update profile", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateProfile(
    profile: Profile,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Profile> = {},
) = post("me/profile", profile, errorContext = "update profile", onError = onError, onSuccess = onSuccess)

suspend fun Api.hiddenGroups(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GroupExtended>> = {},
) = get("me/groups/hidden", errorContext = "load hidden groups", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateProfilePhoto(
    photo: ByteArray,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    return post("me/profile/photo", MultiPartFormDataContent(
        formData {
            append("photo", photo, Headers.build {
                append(HttpHeaders.ContentType, "image/jpeg")
                append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
            })
        }
    ), client = httpDataClient, errorContext = "upload profile image", onError = onError, onSuccess = onSuccess)
}

suspend fun Api.updateProfileVideo(
    video: InputProvider,
    contentType: String,
    filename: String,
    uploadCallback: (Float) -> Unit,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    return post(
        "me/profile/video",
        MultiPartFormDataContent(
            formData {
                append(
                    "photo",
                    video,
                    Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "filename=${filename}")
                    }
                )
            }
        ),
        progressCallback = uploadCallback,
        client = httpDataClient,
        errorContext = "upload profile video",
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.updateMyPhoto(
    photo: ByteArray,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    return post(
        "me/photo",
        MultiPartFormDataContent(
            formData {
                append("photo", photo, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
                })
            }
        ),
        client = httpDataClient,
        errorContext = "upload profile image",
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.deleteMe(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    return post(
        "me/delete",
        errorContext = "delete account",
        onError = onError,
        onSuccess = onSuccess
    )
}

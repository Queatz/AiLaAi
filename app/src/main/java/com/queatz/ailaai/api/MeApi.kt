package com.queatz.ailaai.api

import android.net.Uri
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.*
import com.queatz.ailaai.data.*
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.ailaai.extensions.asScaledJpeg
import com.queatz.ailaai.extensions.asScaledVideo
import com.queatz.ailaai.extensions.toList
import io.ktor.client.request.forms.*
import io.ktor.http.*

suspend fun Api.me(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Person>,
) = get("me", onError = onError, onSuccess = onSuccess)

suspend fun Api.transferCode(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Transfer>,
) = get("me/transfer", onError = onError, onSuccess = onSuccess)

suspend fun Api.refreshTransferCode(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Transfer>,
) = post("me/transfer/refresh", onError = onError, onSuccess = onSuccess)

suspend fun Api.myGeo(
    geo: LatLng,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post("me/geo", geo.toList(), onError = onError, onSuccess = onSuccess)

suspend fun Api.myDevice(
    deviceType: DeviceType,
    deviceToken: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post("me/device", Device(deviceType, deviceToken), onError = onError, onSuccess = onSuccess)

suspend fun Api.updateMe(
    person: Person,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Person> = {},
) = post("me", person, onError = onError, onSuccess = onSuccess)

suspend fun Api.updateProfile(
    profile: Profile,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Profile> = {},
) = post("me/profile", profile, onError = onError, onSuccess = onSuccess)

suspend fun Api.hiddenGroups(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GroupExtended>> = {},
) = get("me/groups/hidden", onError = onError, onSuccess = onSuccess)

suspend fun Api.updateProfilePhoto(
    photo: Uri,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    val scaledPhoto = photo.asScaledJpeg(context)
    return post("me/profile/photo", MultiPartFormDataContent(
        formData {
            append("photo", scaledPhoto, Headers.build {
                append(HttpHeaders.ContentType, "image/jpg")
                append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
            })
        }
    ), client = dataClient(), onError = onError, onSuccess = onSuccess)
}

suspend fun Api.updateProfileVideo(
    video: Uri,
    contentType: String,
    filename: String,
    processingCallback: (Float) -> Unit,
    uploadCallback: (Float) -> Unit,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    val scaledVideo = try {
        video.asScaledVideo(context, progressCallback = processingCallback)
    } catch (e: Exception) {
        e.printStackTrace()
        onError?.invoke(e)
        return
    }
    return post(
        "me/profile/video",
        MultiPartFormDataContent(
            formData {
                append(
                    "photo",
                    scaledVideo.asInputProvider(),
                    Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "filename=${filename}")
                    }
                )
            }
        ),
        progressCallback = uploadCallback,
        client = dataClient(),
        onError = onError,
        onSuccess = onSuccess
    )
}

suspend fun Api.updateMyPhoto(
    photo: Uri,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) {
    val scaledPhoto = photo.asScaledJpeg(context)
    return post(
        "me/photo",
        MultiPartFormDataContent(
            formData {
                append("photo", scaledPhoto, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpg")
                    append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
                })
            }
        ),
        client = dataClient(),
        onError = onError,
        onSuccess = onSuccess
    )
}


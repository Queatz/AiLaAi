package app.ailaai.api

import com.queatz.db.*
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock

suspend fun Api.signals(
    localHour: Int? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Signal>> = {}
) = get("signals", localHour?.let { mapOf("localHour" to it.toString()) }, onError = onError, onSuccess = onSuccess)

suspend fun Api.createSignal(
    signal: Signal,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Signal> = {}
) = post("signals", signal, onError = onError, onSuccess = onSuccess)

suspend fun Api.mySignals(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<PersonSignal>> = {}
) = get("signals/me", onError = onError, onSuccess = onSuccess)

suspend fun Api.toggleSignal(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<PersonSignal> = {}
) = post("signals/$id/toggle", onError = onError, onSuccess = onSuccess)

suspend fun Api.sendSignal(
    body: SendSignalBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<SignalSend> = {}
) = post("signals/send", body, onError = onError, onSuccess = onSuccess)

suspend fun Api.cancelSignal(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<SignalSend> = {}
) = post("signals/send/$id/cancel", onError = onError, onSuccess = onSuccess)

suspend fun Api.replySignal(
    id: String,
    body: SignalReplyBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<SignalReply> = {}
) = post("signals/send/$id/reply", body, onError = onError, onSuccess = onSuccess)

suspend fun Api.activeSignals(
    geo: List<Double>? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<ActiveSignalsResponse> = {}
) = get("signals/active", geo?.let { mapOf("geo" to it.joinToString(",")) }, onError = onError, onSuccess = onSuccess)

suspend fun Api.createSignalGroup(
    id: String,
    people: List<String>,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Group> = {}
) = post("signals/send/$id/create-group", CreateGroupBody(people, reuse = false), onError = onError, onSuccess = onSuccess)

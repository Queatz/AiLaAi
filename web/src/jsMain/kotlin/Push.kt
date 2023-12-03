import app.ailaai.api.myDevice
import com.queatz.db.DeviceType
import com.queatz.push.PushData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import org.w3c.dom.EventSource
import kotlin.time.Duration.Companion.seconds

val push = Push()

class Push {

    private lateinit var job: Job

    val events = MutableSharedFlow<PushData>()
    val reconnect = MutableSharedFlow<Unit>()

    fun start(scope: CoroutineScope) {
        job = scope.launch {
            while (true) {
                application.bearerToken.first { it != null }

                var error = false
                api.myDevice(
                    DeviceType.Web,
                    api.device,
                    onError = {
                        error = true
                    }
                )

                if (error) {
                    delay(3.seconds)
                    continue
                }

                val deferred = CompletableDeferred<Unit>()
                val sse = EventSource("$baseUrl/push/${api.device}")
                val jobScope = this
                sse.onmessage = {
                    jobScope.launch {
                        (it.data as? String)?.let { data ->
                            try {
                                events.emit(json.decodeFromString(data))
                            } catch (e: Throwable) {
                                console.log(e)
                                null
                            }
                        } ?: console.log("Not a string:", it.data)
                    }
                }
                sse.onerror = {
                    console.log(it)
                    if (it.eventPhase == EventSource.CLOSED) {
                        deferred.complete(Unit)
                    }
                }
                sse.onopen = {
                    jobScope.launch {
                        reconnect.emit(Unit)
                    }
                }
                deferred.await()
                delay(3.seconds)
            }
        }
    }

    fun stop() {
        job.cancel()
    }
}

package com.queatz.api

import com.queatz.db.DeviceType
import com.queatz.db.deleteDevice
import com.queatz.plugins.db
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// https://developer.huawei.com/consumer/en/doc/development/HMSCore-Guides/msg-receipt-guide-0000001050040176#EN-US_TOPIC_0000001554718349__p121151147184318
private val unregisteredStatuses = setOf(2, 5)

fun Route.hmsRoutes() {
    post("/hms/receipt") {
        respond {
            launch {
                call.receive<HmsMessageReceipt>().statuses.filter {
                    it.status in unregisteredStatuses
                }.forEach { status ->
                    db.deleteDevice(DeviceType.Hms, status.token)
                }

            }
            HttpStatusCode.OK
        }
    }
}

@Serializable
data class HmsMessageReceipt(
    val statuses: List<HmsMessageReceiptStatus>
)

@Serializable
data class HmsMessageReceiptStatus(
    val token: String,
    val status: Int
)

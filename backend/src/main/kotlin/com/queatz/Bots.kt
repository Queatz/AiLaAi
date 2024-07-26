package com.queatz

import com.queatz.api.ellipsize
import com.queatz.db.Bot
import com.queatz.db.BotConfigField
import com.queatz.db.BotConfigValue
import com.queatz.db.BotMessageStatus
import com.queatz.db.Group
import com.queatz.db.Message
import com.queatz.db.groupBotData
import com.queatz.db.groupBotsOfGroup
import com.queatz.plugins.db
import com.queatz.plugins.json
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

@Serializable
data class BotDetails(
    val name: String? = null,
    val description: String? = null,
    val keywords: List<String>? = null,
    val config: List<BotConfigField>? = null,
)

@Serializable
data class InstallBotResponse(
    val token: String
)

@Serializable
data class InstallBotBody(
    val groupId: String,
    val groupName: String,
    val webhook: String,
    val config: List<BotConfigValue>? = null,
    val secret: String? = null
)

@Serializable
data class ReinstallBotBody(
    val config: List<BotConfigValue>? = null,
)

@Serializable
data class MessageBotResponse(
    val success: Boolean? = null,
    val note: String? = null,
    val actions: List<BotAction>? = null
)

@Serializable
data class BotAction(
    val message: String? = null
)

@Serializable
data class MessageBotBody(
    val message: String? = null
)

class Bots {

    private lateinit var coroutineScope: CoroutineScope
    private val http = HttpClient(CIO) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }

        engine {
            requestTimeout = 2.minutes.inWholeMilliseconds
        }
    }

    fun start(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
    }

    suspend fun details(url: String): BotDetails = http.get(url).body()

    suspend fun install(url: String, body: InstallBotBody): InstallBotResponse =
        http.post("$url/install") {
            setBody(body)
        }.body()

    suspend fun reinstall(url: String, authToken: String, body: ReinstallBotBody): HttpStatusCode =
        http.post("$url/reinstall") {
            bearerAuth(authToken)
            setBody(body)
        }.body()

    suspend fun uninstall(url: String, authToken: String): HttpStatusCode =
        http.post("$url/uninstall") {
            bearerAuth(authToken)
        }.body()

    suspend fun message(url: String, authToken: String, body: MessageBotBody): MessageBotResponse =
        http.post("$url/message") {
            bearerAuth(authToken)
            setBody(body)
        }.body()

    fun notify(message: Message) {
        coroutineScope.launch {
            db.groupBotsOfGroup(message.group!!).forEach { groupBot ->
                val bot = db.document(Bot::class, groupBot.bot!!) ?: let {
                    print("Bot not found: GroupBot(id = ${groupBot.id!!})")
                    return@forEach
                }

                val authToken = db.groupBotData(groupBot.id!!)?.authToken ?: let {
                    print("GroupBotData not found: GroupBot(id = ${groupBot.id!!})")
                    return@forEach
                }

                val group = db.document(Group::class, message.group!!)
                    ?: let {
                        println("Group not found")
                        return@forEach
                    }

                val text = message.text.orEmpty().lowercase()

                val matches = bot.keywords.isNullOrEmpty() || bot.keywords?.any { keyword ->
                    text.contains(keyword, ignoreCase = true)
                } == true

                if (!matches) {
                    return@forEach
                }

                val response = message(
                    url = bot.url!!,
                    authToken = authToken,
                    body = MessageBotBody(message = message.text.orEmpty())
                )

                db.document(Message::class, message.id!!)?.let {
                    db.update(
                        it.apply {
                            bots = (bots ?: emptyList()) + BotMessageStatus(
                                bot = bot.id!!,
                                success = response.success ?: true,
                                note = response.note
                            )
                        }
                    )
                }

                response.actions?.forEach { action ->
                    handle(action, group, bot)
                }
            }
        }
    }

    private fun handle(action: BotAction, group: Group, bot: Bot) {
        action.message?.let { actionMessage ->
            val botMessage = db.insert(
                Message(
                    group = group.id!!,
                    bot = bot.id!!,
                    text = actionMessage
                )
            )
            com.queatz.plugins.notify.message(
                group = group,
                bot = bot,
                message = Message(text = botMessage.text?.ellipsize())
            )
        }
    }
}

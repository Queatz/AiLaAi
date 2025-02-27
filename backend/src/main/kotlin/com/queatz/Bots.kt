package com.queatz

import com.queatz.api.ellipsize
import com.queatz.db.Bot
import com.queatz.db.BotConfigField
import com.queatz.db.BotConfigValue
import com.queatz.db.BotMessageStatus
import com.queatz.db.Group
import com.queatz.db.GroupBot
import com.queatz.db.Message
import com.queatz.db.Person
import com.queatz.db.asKey
import com.queatz.db.groupBotData
import com.queatz.db.groupBotsOfGroup
import com.queatz.plugins.bots
import com.queatz.plugins.db
import com.queatz.plugins.json
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.text.Charsets.UTF_8
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
    val message: String? = null,
    val person: Person? = null,
    val bot: Bot? = null
)

class Bots {

    private lateinit var coroutineScope: CoroutineScope
    private val http = HttpClient(CIO) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }

        engine {
            requestTimeout = 5.minutes.inWholeMilliseconds
        }
    }

    fun start(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
    }

    suspend fun details(url: String): BotDetails = http.get(url).body()

    suspend fun install(url: String, body: InstallBotBody): InstallBotResponse =
        http.post("$url/install") {
            json()
            setBody(body)
        }.body()

    suspend fun reinstall(url: String, authToken: String, body: ReinstallBotBody): HttpStatusCode =
        http.post("$url/reinstall") {
            bearerAuth(authToken)
            json()
            setBody(body)
        }.body()

    suspend fun uninstall(url: String, authToken: String): HttpStatusCode =
        http.post("$url/uninstall") {
            bearerAuth(authToken)
        }.body()

    suspend fun pause(url: String, authToken: String): HttpStatusCode =
        http.post("$url/pause") {
            bearerAuth(authToken)
        }.body()

    suspend fun resume(url: String, authToken: String): HttpStatusCode =
        http.post("$url/resume") {
            bearerAuth(authToken)
        }.body()

    suspend fun message(url: String, authToken: String, body: MessageBotBody): MessageBotResponse =
        http.post("$url/message") {
            bearerAuth(authToken)
            json()
            setBody(body)
        }.body()

    fun notify(message: Message, person: Person? = null, messageBot: Bot? = null) {
        coroutineScope.launch {
            db.groupBotsOfGroup(message.group!!).forEach { groupBot ->
                // Bots cannot send messages to themselves
                if (messageBot?.id == groupBot.bot) {
                    return@forEach
                }

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
                    body = MessageBotBody(
                        message = message.text.orEmpty(),
                        person = person?.let { person ->
                            Person().apply {
                                id = person.id
                                name = person.name
                            }
                        },
                        bot = messageBot?.let { bot ->
                            Bot().apply {
                                id = bot.id
                                name = bot.name
                            }
                        }
                    )
                )

                // Save the bot response on the message if a success value is given
                response.success?.let { success ->
                    db.document(Message::class, message.id!!)?.let {
                        db.update(
                            it.apply {
                                bots = (bots ?: emptyList()) + BotMessageStatus(
                                    bot = bot.id!!,
                                    success = success,
                                    note = response.note
                                )
                            }
                        )
                    }
                }

                response.actions?.forEach { action ->
                    handle(action, group, bot)
                }
            }
        }
    }

    fun action(groupBot: GroupBot, action: BotAction) {
        val group = db.document(Group::class, groupBot.group!!) ?: return
        val bot = db.document(Bot::class, groupBot.bot!!) ?: return
        handle(action, group, bot)
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

            notify(
                message = botMessage,
                messageBot = bot
            )

            group.seen = Clock.System.now()
            db.update(group)
        }
    }
}

private fun HttpRequestBuilder.json() {
    contentType(ContentType.Application.Json.withCharset(UTF_8))
}

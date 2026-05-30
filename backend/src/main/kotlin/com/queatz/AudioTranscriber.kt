package com.queatz

import com.queatz.db.AudioAttachment
import com.queatz.db.Bot
import com.queatz.db.Group
import com.queatz.db.Member
import com.queatz.db.Message
import com.queatz.db.Person
import com.queatz.plugins.db
import com.queatz.plugins.json
import com.queatz.plugins.notify
import com.queatz.plugins.openAi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

val audioTranscriber = AudioTranscriber()

class AudioTranscriber {
    private lateinit var scope: CoroutineScope
    private val inProgress = ConcurrentHashMap.newKeySet<String>()

    fun start(scope: CoroutineScope) {
        this.scope = scope
    }

    fun handle(message: Message) {
        if (!message.text.isNullOrBlank()) return
        val id = message.id ?: return
        if (!inProgress.add(id)) return

        val attachmentJson = message.attachment ?: return
        
        val isAudio = try {
            val element = json.parseToJsonElement(attachmentJson)
            element.jsonObject["audio"] != null
        } catch (e: Exception) {
            false
        }
        
        if (!isAudio) {
            inProgress.remove(id)
            return
        }

        scope.launch {
            try {
                val attachment = try {
                    json.decodeFromString<AudioAttachment>(attachmentJson)
                } catch (e: Exception) {
                    return@launch
                }

                val audioPath = attachment.audio ?: return@launch
                val audioFile = File(".$audioPath")
                if (!audioFile.exists()) return@launch

                val bytes = audioFile.readBytes()
                val transcription = openAi.transcribe(bytes)

                if (!transcription.isNullOrBlank()) {
                    val latestMessage = db.document(Message::class, id) ?: return@launch
                    latestMessage.text = (latestMessage.text?.let { "$it\n\n" } ?: "") + transcription
                    db.update(latestMessage)

                    val group = latestMessage.group?.let { db.document(Group::class, it) }
                    val member = latestMessage.member?.let { db.document(Member::class, it) }
                    val person = member?.from?.let { db.document(Person::class, it) }
                    val bot = latestMessage.bot?.let { db.document(Bot::class, it) }

                    if (group != null) {
                        notify.message(
                            group = group,
                            person = person,
                            bot = bot,
                            message = latestMessage,
                            show = false
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                inProgress.remove(id)
            }
        }
    }
}

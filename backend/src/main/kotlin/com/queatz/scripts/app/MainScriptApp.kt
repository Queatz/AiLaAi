package com.queatz.scripts.app

import com.queatz.api.ellipsize
import com.queatz.db.Group
import com.queatz.db.Message
import com.queatz.db.Person
import com.queatz.db.member
import com.queatz.plugins.db
import com.queatz.plugins.notify
import com.queatz.save
import com.queatz.scripts.ScriptApp
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlin.time.Clock

class MainScriptApp(
    private val script: String,
    private val me: String?
) : ScriptApp {

    private val http = HttpClient(Java) {
        expectSuccess = true

        engine {
            protocolVersion = java.net.http.HttpClient.Version.HTTP_2
        }
    }

    override suspend fun download(
        url: String,
        name: String,
    ): String {
        return http
            .get(url)
            .bodyAsBytes()
            .save("script/$script", name)
    }

    override suspend fun message(
        groupId: String,
        text: String,
    ): Message {
        val group = db.document(Group::class, groupId)
            ?: error("Group $groupId not found")

        val member = me?.let {
            db.member(person = it, group = groupId)
        }

        val person = me?.let {
            db.document(Person::class, it)
        }

        val message = db.insert(
            Message(
                group = group.id,
                member = member?.id,
                text = text,
                script = script
            )
        )

        group.seen = Clock.System.now()
        db.update(group)

        notify.message(
            group = group,
            person = person,
            message = Message(
                text = text.ellipsize()
            )
        )

        return message
    }
}

package com.queatz

import com.queatz.db.Group
import com.queatz.db.Member
import com.queatz.db.Person
import com.queatz.db.asId
import com.queatz.plugins.db
import kotlin.time.Clock

class App {
    fun createGroup(
        people: List<String>,
        hosts: List<String> = emptyList(),
    ): Group =
        db.insert(Group(seen = Clock.System.now()))
            .also {
                val group = it.id!!.asId(Group::class)

                people.distinct().forEach {
                    createMember(it, group, host = if (hosts.isEmpty()) true else hosts.contains(it).takeIf { it })
                }
            }

    fun createMember(
        person: String,
        group: String,
        host: Boolean? = null,
    ) = db.insert(
        Member(
            host = host?.takeIf { it }
        ).also {
            it.from = person.asId(Person::class)
            it.to = group.asId(Group::class)
        }
    )
}

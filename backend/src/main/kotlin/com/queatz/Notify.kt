package com.queatz

import com.queatz.db.*
import com.queatz.db.Call
import com.queatz.plugins.db
import com.queatz.plugins.push
import com.queatz.push.*
import kotlinx.datetime.Clock

class Notify {
    fun callStatus(group: Group, call: Call) {
        val pushData = PushData(
            PushAction.CallStatus,
            CallStatusPushData(
                Call().apply {
                    id = call.id
                    this.group = call.group
                    participants = call.participants
                }
            )
        )

        notifyGroupMembers(null, group, pushData)
    }

    fun call(group: Group, from: Person) {
        val pushData = PushData(
            PushAction.Call,
            CallPushData(
                Group().apply {
                    id = group.id
                    name = group.name
                },
                Person().apply {
                    name = from.name
                    id = from.id
                }
            )
        )

        notifyGroupMembers(from, group, pushData)
    }

    fun message(group: Group, from: Person, message: Message) {
        val pushData = PushData(
            PushAction.Message,
            MessagePushData(
                Group().apply {
                    id = group.id
                    name = group.name
                },
                Person().apply {
                    name = from.name
                    id = from.id
                },
                message
            )
        )

        notifyGroupMembers(from, group, pushData)
    }

    fun newJoinRequest(person: Person, joinRequest: JoinRequest, group: Group) {
        val pushData = PushData(
            PushAction.JoinRequest,
            JoinRequestPushData(
                Person().apply {
                    name = person.name
                    id = person.id
                },
                Group().apply {
                    name = group.name
                    id = group.id!!
                },
                JoinRequest().apply {
                    id = joinRequest.id
                    message = joinRequest.message
                },
                JoinRequestEvent.Request
            )
        )

        notifyGroupMembers(person, group, pushData)
    }

    fun newMember(invitor: Person, person: Person, group: Group) {
        val pushData = PushData(
            PushAction.Group,
            GroupPushData(
                Person().apply {
                    name = person.name
                    id = person.id
                },
                Group().apply {
                    id = group.id
                    name = group.name
                },
                GroupEvent.Join,
                GroupEventData(
                    invitor = Person().apply {
                        name = invitor.name
                        id = invitor.id
                    },
                )
            )
        )

        notifyGroupMembers(invitor, group, pushData)
    }

    private fun notifyGroupMembers(from: Person?, group: Group, pushData: PushData) {
        db.memberDevices(group.id!!).apply {
            unhide()

            // Send push
            forEach {
                it.devices?.forEach { device ->
                    push.sendPush(device, pushData.show(it.member?.from != from?.id?.asId(Person::class) && it.member?.isSnoozedNow != true))
                }
            }
        }
    }

    // Un-hide any groups
    private fun List<MemberDevice>.unhide() {
        filter { it.member?.hide == true }.forEach {
            it.member!!.hide = false
            db.update(it.member!!)
        }
    }
}

fun PushData.show(show: Boolean) = PushData(
    action,
    data?.let {
        when (it) {
            is MessagePushData -> it.copy(show = show)
            is CallPushData -> it.copy(show = show)
            else -> data
        }
    }
)

private val Member.isSnoozedNow get() = snoozed == true || snoozedUntil?.let { it > Clock.System.now() } == true

package com.queatz.db

/**
 * @group The group to fetch devices for
 */
fun Db.memberDevices(group: String, onlyHosts: Boolean = false) = query(
    MemberDevice::class,
    """
        for member in `${Member::class.collection()}`
            filter member._to == @group and member.${f(Member::gone)} != true
                and (@onlyHosts != true or member.${f(Member::host)} == true)
            return {
                member,
                devices: (
                    for device in `${Device::class.collection()}`
                        filter device.${f(Device::person)} == member._from
                        return device
                )
            }
    """.trimIndent(),
    mapOf(
        "group" to group.asId(Group::class),
        "onlyHosts" to onlyHosts
    )
)

/**
 * @people The people to fetch devices for
 */
fun Db.peopleDevices(people: List<String>) = list(
    Device::class,
    """
        for device in @@collection
            filter device.${f(Device::person)} in @people
            return device
    """.trimIndent(),
    mapOf(
        "people" to people.map { it.asId(Person::class) }
    )
)

fun Db.updateDevice(person: String, type: DeviceType, token: String) = one(
    Device::class,
    """
            upsert { ${f(Device::type)}: @type, ${f(Device::token)}: @token }
                insert { ${f(Device::type)}: @type, ${f(Device::token)}: @token, ${f(Device::person)}: @person, ${f(Person::createdAt)}: DATE_ISO8601(DATE_NOW()) }
                update { ${f(Device::type)}: @type, ${f(Device::token)}: @token, ${f(Device::person)}: @person }
                in @@collection
                return NEW || OLD
        """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "type" to type,
        "token" to token
    )
)

fun Db.deleteDevice(type: DeviceType, token: String) = query(
    Device::class,
    """
        for x in ${Device::class.collection()}
            filter x.${f(Device::type)} == @type
                and x.${f(Device::token)} == @token
            remove x in ${Device::class.collection()}
    """.trimIndent(),
    mapOf(
        "type" to type,
        "token" to token
    )
)

fun Db.device(type: DeviceType, token: String) = one(
    Device::class,
    """
            upsert { ${f(Device::type)}: @type, ${f(Device::token)}: @token }
                insert { ${f(Device::type)}: @type, ${f(Device::token)}: @token, ${f(Person::createdAt)}: DATE_ISO8601(DATE_NOW()) }
                update { }
                in @@collection
                return NEW || OLD
        """,
    mapOf(
        "type" to type,
        "token" to token
    )
)!!

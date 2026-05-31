package com.queatz.db

import kotlin.time.Instant

fun Db.signals(hour: Int, offset: Int = 0) = list(
    Signal::class,
    """
        for x in `${Signal::class.collection()}`
            let stats = (
                for s in `${SignalStats::class.collection()}`
                    filter s.${f(SignalStats::signal)} == x._key and s.${f(SignalStats::hour)} == @hour
                    return s.${f(SignalStats::count)}
            )[0]
            sort stats desc, x.${f(Signal::name)} asc
            limit @offset, 20
            return x
    """.trimIndent(),
    mapOf("hour" to hour, "offset" to offset)
)

fun Db.incrementSignalStats(signal: String, hour: Int) = query(
    SignalStats::class,
    """
        upsert { ${f(SignalStats::signal)}: @signal, ${f(SignalStats::hour)}: @hour }
            insert { ${f(SignalStats::signal)}: @signal, ${f(SignalStats::hour)}: @hour, ${f(SignalStats::count)}: 1, ${f(SignalStats::createdAt)}: DATE_ISO8601(DATE_NOW()) }
            update { ${f(SignalStats::count)}: OLD.${f(SignalStats::count)} + 1 }
            in `${SignalStats::class.collection()}`
            return NEW
    """.trimIndent(),
    mapOf("signal" to signal, "hour" to hour)
)

fun Db.personSignals(person: String) = list(
    PersonSignal::class,
    "for x in @@collection filter x.${f(PersonSignal::person)} == @person return x",
    mapOf("person" to person)
)

fun Db.toggleSignal(person: String, signal: String) = one(
    PersonSignal::class,
    """
        upsert { ${f(PersonSignal::person)}: @person, ${f(PersonSignal::signal)}: @signal }
            insert { ${f(PersonSignal::person)}: @person, ${f(PersonSignal::signal)}: @signal, ${f(PersonSignal::turnedOn)}: true, ${f(PersonSignal::createdAt)}: DATE_ISO8601(DATE_NOW()) }
            update { ${f(PersonSignal::turnedOn)}: !OLD.${f(PersonSignal::turnedOn)} }
            in @@collection
            return NEW
    """.trimIndent(),
    mapOf("person" to person, "signal" to signal)
)

fun Db.activeSignals(person: String, geo: List<Double>?) = query(
    SignalSendExtended::class,
    """
        let now = DATE_ISO8601(DATE_NOW())
        let myFriends = (
            for group, myMember in outbound @personId graph `${Member::class.graph()}`
                filter myMember.${f(Member::gone)} != true
                for friend, member in inbound group graph `${Member::class.graph()}`
                    filter member.${f(Member::gone)} != true
                        and friend._key != @person
                    return friend._key
        )
        
        for x in `${SignalSend::class.collection()}`
            filter x.${f(SignalSend::expiry)} > now
                and (
                    x.${f(SignalSend::person)} == @person
                    or (x.${f(SignalSend::audience)} == 'Friends' and x.${f(SignalSend::person)} in myFriends)
                    or (x.${f(SignalSend::audience)} == 'Nearby' and (x.${f(SignalSend::person)} in myFriends or (x.${f(SignalSend::geo)} != null and @geo != null and distance(x.${f(SignalSend::geo)}[0], x.${f(SignalSend::geo)}[1], @geo[0], @geo[1]) <= x.${f(SignalSend::radius)} * 1000)))
                    or (x.${f(SignalSend::audience)} == 'Groups' and (x.${f(SignalSend::groups)} == null or count(for g in x.${f(SignalSend::groups)} for p, m in inbound (contains(g, '/') ? g : concat('${Group::class.collection()}/', g)) graph `${Member::class.graph()}` filter p._key == @person and m.${f(Member::gone)} != true return 1) > 0))
                    or (x.${f(SignalSend::audience)} == null and (x.${f(SignalSend::person)} in myFriends or (x.${f(SignalSend::geo)} != null and @geo != null and distance(x.${f(SignalSend::geo)}[0], x.${f(SignalSend::geo)}[1], @geo[0], @geo[1]) <= x.${f(SignalSend::radius)} * 1000)))
                    or count(for r in `${SignalReply::class.collection()}` filter r.${f(SignalReply::signalSend)} == x._key and r.${f(SignalReply::person)} == @person limit 1 return 1) > 0
                )
            let signal = document(x.${f(SignalSend::signal)} != null ? (contains(x.${f(SignalSend::signal)}, '/') ? x.${f(SignalSend::signal)} : concat('${Signal::class.collection()}/', x.${f(SignalSend::signal)})) : null)
            let person = document(x.${f(SignalSend::person)} != null ? (contains(x.${f(SignalSend::person)}, '/') ? x.${f(SignalSend::person)} : concat('${Person::class.collection()}/', x.${f(SignalSend::person)})) : null)
            let senderSignals = (for ps in `${PersonSignal::class.collection()}` filter ps.${f(PersonSignal::person)} == x.${f(SignalSend::person)} and ps.${f(PersonSignal::turnedOn)} == true return ps.${f(PersonSignal::signal)})
            let replies = (
                for r in `${SignalReply::class.collection()}`
                    filter r.${f(SignalReply::signalSend)} == x._key
                        and (
                            (x.${f(SignalSend::person)} == @person and r.${f(SignalReply::released)} == true)
                            or r.${f(SignalReply::person)} == @person
                        )
                    let rp = document(r.${f(SignalReply::person)} != null ? (contains(r.${f(SignalReply::person)}, '/') ? r.${f(SignalReply::person)} : concat('${Person::class.collection()}/', r.${f(SignalReply::person)})) : null)
                    let affinitySignals = (
                        for ps in `${PersonSignal::class.collection()}`
                            filter ps.${f(PersonSignal::person)} == r.${f(SignalReply::person)}
                                and ps.${f(PersonSignal::turnedOn)} == true
                                and ps.${f(PersonSignal::signal)} in senderSignals
                            let s = document(ps.${f(PersonSignal::signal)} != null ? (contains(ps.${f(PersonSignal::signal)}, '/') ? ps.${f(PersonSignal::signal)} : concat('${Signal::class.collection()}/', ps.${f(PersonSignal::signal)})) : null)
                            filter s != null
                            return s
                    )
                    return { signalReply: r, person: rp, affinitySignals: affinitySignals }
            )
            return {
                signalSend: x,
                signal: signal,
                person: person,
                replies: replies
            }
    """.trimIndent(),
    mapOf(
        "person" to person,
        "personId" to person.asId(Person::class),
        "geo" to geo
    )
)

fun Db.unreleasedSignalReplies(now: Instant) = list(
    SignalReply::class,
    """
        for x in @@collection
            filter x.${f(SignalReply::released)} != true
                and x.${f(SignalReply::releaseAt)} != null
                and x.${f(SignalReply::releaseAt)} <= @now
            return x
    """.trimIndent(),
    mapOf("now" to now)
)

fun Db.unprocessedSignalReplies(bucketEnd: Instant) = list(
    SignalReply::class,
    """
        for x in @@collection
            filter x.${f(SignalReply::releaseAt)} == null
                and x.${f(SignalReply::createdAt)} <= @bucketEnd
            return x
    """.trimIndent(),
    mapOf("bucketEnd" to bucketEnd)
)

fun Db.cancelSignalSend(id: String, person: String) = one(
    SignalSend::class,
    """
        for x in @@collection
            filter x._key == @id and x.${f(SignalSend::person)} == @person
            update x with { ${f(SignalSend::expiry)}: DATE_ISO8601(DATE_NOW()) } in @@collection
            return NEW
    """.trimIndent(),
    mapOf("id" to id, "person" to person)
)

fun Db.peopleWithSignalOn(people: List<String>, signal: String) = query(
    String::class,
    """
        for x in `${PersonSignal::class.collection()}`
            filter x.${f(PersonSignal::person)} in @people
                and x.${f(PersonSignal::signal)} == @signal
                and x.${f(PersonSignal::turnedOn)} == true
            return x.${f(PersonSignal::person)}
    """.trimIndent(),
    mapOf("people" to people, "signal" to signal)
)

fun Db.signalReply(person: String, signalSend: String) = one(
    SignalReply::class,
    "for x in @@collection filter x.${f(SignalReply::person)} == @person and x.${f(SignalReply::signalSend)} == @signalSend return x",
    mapOf("person" to person, "signalSend" to signalSend)
)

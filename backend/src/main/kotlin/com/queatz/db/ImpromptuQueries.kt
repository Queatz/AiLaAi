package com.queatz.db

import kotlinx.serialization.Serializable

/**
 * Database operations for the Impromptu feature
 */

/**
 * Get a person's impromptu settings
 */
fun Db.getImpromptu(person: String) = one(
    Impromptu::class,
    """
    for impromptu in @@collection
        filter impromptu.${f(Impromptu::person)} == @person
        sort impromptu.${f(Impromptu::createdAt)} desc
        limit 1
        return ${impromptuWithSeeks()}
    """,
    mapOf(
        "person" to person
    )
)

/**
 * Retrieves all active impromptu settings from the database.
 * An impromptu is considered active if it has a non-null updateLocation that is not set to Off.
 *
 * @return List of active Impromptu objects, each merged with their associated Person details.
 */
fun Db.allActiveImpromptu() = list(
    Impromptu::class,
    """
        for impromptu in @@collection
            filter impromptu.${f(Impromptu::updateLocation)} != null
                and impromptu.${f(Impromptu::updateLocation)} != ${v(ImpromptuLocationUpdates.Off)}
            let person = document(${Person::class.collection()}, impromptu.${f(Impromptu::person)})
            return merge(
                impromptu,
                { ${f(Impromptu::personDetails)}: person }
            )
    """.trimIndent()
)

@Serializable
data class ImpromptuProposal(
    val person: Person,
    val otherPerson: Person,
    val distance: Double,
    val everyone: Boolean,
    val seeks: List<ImpromptuSeek> = emptyList(),
    val notificationType: ImpromptuNotificationStyle? = ImpromptuNotificationStyle.Normal
)

/**
 * Process all active impromptu settings and find matching people based on distance and seek criteria
 */
fun Db.processAllImpromptu() = query(
    ImpromptuProposal::class,
    """
        // Find all active impromptu settings
        for impromptu in @@impromptuCollection
            filter impromptu.${f(Impromptu::mode)} != null
                and impromptu.${f(Impromptu::mode)} != ${v(ImpromptuMode.Off)}

            for otherImpromptu in @@impromptuCollection
                filter otherImpromptu.${f(Impromptu::mode)} != null
                    and otherImpromptu.${f(Impromptu::mode)} != ${v(ImpromptuMode.Off)}
                    and otherImpromptu.${f(Impromptu::person)} != impromptu.${f(Impromptu::person)}

                let person = document(@@personCollection, impromptu.${f(Impromptu::person)})
                let otherPerson = document(@@personCollection, otherImpromptu.${f(Impromptu::person)})

                // Only proceed if both people have geo coordinates
                filter person.${f(Person::geo)} != null and otherPerson.${f(Person::geo)} != null

                // Calculate distance between the two people
                let d = distance(
                    person.${f(Person::geo)}[0],
                    person.${f(Person::geo)}[1], 
                    otherPerson.${f(Person::geo)}[0],
                    otherPerson.${f(Person::geo)}[1]
                )
                
                // Ensure the local time for both people is between 7am and 7pm
                let personLocalTime = DATE_HOUR(DATE_ADD(DATE_NOW(), person.${f(Person::utcOffset)} || 0, "hours"))
                let otherPersonLocalTime = DATE_HOUR(DATE_ADD(DATE_NOW(), otherPerson.${f(Person::utcOffset)} || 0, "hours"))
                filter personLocalTime >= 7 and personLocalTime < 19
                    and otherPersonLocalTime >= 7 and otherPersonLocalTime < 19

                // Check if either person has a seek that matches the other's criteria
                for seek in @@impromptuSeekCollection
                    filter seek.${f(ImpromptuSeek::person)} == impromptu.${f(Impromptu::person)}
                        and seek.${f(ImpromptuSeek::offer)} == true
                        and d <= (seek.${f(ImpromptuSeek::radius)} || $maxImpromptuDistanceKm) * 1000 // Convert km to meters
                        and DATE_ISO8601(seek.${f(ImpromptuSeek::expiresAt)}) > DATE_ISO8601(DATE_NOW())
                    
                    for otherSeek in @@impromptuSeekCollection
                        filter otherSeek.${f(ImpromptuSeek::person)} == otherImpromptu.${f(Impromptu::person)}
                            and otherSeek.${f(ImpromptuSeek::offer)} != true
                            and d <= (otherSeek.${f(ImpromptuSeek::radius)} || $maxImpromptuDistanceKm) * 1000 // Convert km to meters
                            and DATE_ISO8601(otherSeek.${f(ImpromptuSeek::expiresAt)}) > DATE_ISO8601(DATE_NOW())
    
                        filter lower(seek.${f(ImpromptuSeek::name)}) == lower(otherSeek.${f(ImpromptuSeek::name)})
    
                        // TODO Ensure there is no ImpromptuHistory with the same person and otherPerson and ImpromptuSeeks
                        filter count(
                            for history in @@impromptuHistoryCollection
                                filter history.${f(ImpromptuHistory::person)} == person._key
                                    and history.${f(ImpromptuHistory::otherPerson)} == otherPerson._key
                                    and seek in history.${f(ImpromptuHistory::seeks)}
                                    and otherSeek in history.${f(ImpromptuHistory::seeks)}
                            return true
                        ) == 0
    
                        return {
                            ${f(ImpromptuProposal::person)}: person,
                            ${f(ImpromptuProposal::otherPerson)}: otherPerson,
                            ${f(ImpromptuProposal::distance)}: d,
                            ${f(ImpromptuProposal::everyone)}: impromptu.${f(Impromptu::mode)} == ${v(ImpromptuMode.Everyone)} && otherImpromptu.${f(Impromptu::mode)} == ${v(ImpromptuMode.Everyone)},
                            ${f(ImpromptuProposal::seeks)}: [seek, otherSeek],
                            ${f(ImpromptuProposal::notificationType)}: impromptu.${f(Impromptu::notificationType)}
                        }
    """.trimIndent(),
    mapOf(
        "@impromptuCollection" to Impromptu::class.collection(),
        "@impromptuSeekCollection" to ImpromptuSeek::class.collection(),
        "@impromptuHistoryCollection" to ImpromptuHistory::class.collection(),
        "@personCollection" to Person::class.collection(),
    )
)

private fun Db.impromptuWithSeeks(varName: String = "impromptu"): String {
    return """
    merge(
        $varName,
        {
            ${f(Impromptu::seek)}: (
                for seek in `${ImpromptuSeek::class.collection()}`
                    filter seek.${f(ImpromptuSeek::person)} == $varName.${f(Impromptu::person)}
                        and seek.${f(ImpromptuSeek::offer)} != true
                return seek
            ),
            ${f(Impromptu::offer)}: (
                for offer in `${ImpromptuSeek::class.collection()}`
                    filter offer.${f(ImpromptuSeek::person)} == $varName.${f(Impromptu::person)}
                        and offer.${f(ImpromptuSeek::offer)} == true
                return offer
            )
        }
    )
    """
}

const val maxImpromptuDistanceKm = 10.0

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
        return ${impromptuWithSeeks()}
    """,
    mapOf(
        "person" to person
    )
)

fun Db.allActiveImpromptu() = list(
    Impromptu::class,
    """
        for impromptu in @@collection
            filter impromptu.${f(Impromptu::updateLocation)} != null
                and impromptu.${f(Impromptu::updateLocation)} != ${v(ImpromptuLocationUpdates.Off)}
            return impromptu
    """.trimIndent()
)

@Serializable
data class ImpromptuProposal(
    val person: Person,
    val otherPerson: Person,
    val distance: Double,
    val seeks: List<ImpromptuSeek> = emptyList()
)

/**
 * Process all active impromptu settings and find matching people based on distance and seek criteria
 */
fun Db.processAllImpromptu() = query(
    ImpromptuProposal::class,
    """
        // Find all active impromptu settings
        for impromptu in `${Impromptu::class.collection()}`
            filter impromptu.${f(Impromptu::updateLocation)} != null
                and impromptu.${f(Impromptu::updateLocation)} != ${v(ImpromptuLocationUpdates.Off)}

            for otherImpromptu in `${Impromptu::class.collection()}`
                filter otherImpromptu.${f(Impromptu::updateLocation)} != null
                    and otherImpromptu.${f(Impromptu::updateLocation)} != ${v(ImpromptuLocationUpdates.Off)}
                    and otherImpromptu.${f(Impromptu::person)} != impromptu.${f(Impromptu::person)}

                let person = document(${Person::class.collection()}, impromptu.${f(Impromptu::person)})
                let otherPerson = document(${Person::class.collection()}, otherImpromptu.${f(Impromptu::person)})

                // Only proceed if both people have geo coordinates
                filter person.${f(Person::geo)} != null and otherPerson.${f(Person::geo)} != null

                // Calculate distance between the two people
                let d = distance(
                    person.${f(Person::geo)}[0],
                    person.${f(Person::geo)}[1], 
                    otherPerson.${f(Person::geo)}[0],
                    otherPerson.${f(Person::geo)}[1]
                )

                // Check if either person has a seek that matches the other's criteria
                for seek in `${ImpromptuSeek::class.collection()}`
                    filter seek.${f(ImpromptuSeek::person)} == impromptu.${f(Impromptu::person)}
                        and d <= (seek.${f(ImpromptuSeek::radius)} || $maxImpromptuDistanceKm) * 1000 // Convert km to meters
                    
                    for otherSeek in `${ImpromptuSeek::class.collection()}`
                        filter otherSeek.${f(ImpromptuSeek::person)} == otherImpromptu.${f(Impromptu::person)}
                            and d <= (otherSeek.${f(ImpromptuSeek::radius)} || $maxImpromptuDistanceKm) * 1000 // Convert km to meters
    
                        // Ensure either person is seeking what the other person is offering, or vice versa
                        filter (seek.${f(ImpromptuSeek::name)} == otherSeek.${f(ImpromptuSeek::name)})
                            and ((seek.${f(ImpromptuSeek::offer)} == true) != (otherSeek.${f(ImpromptuSeek::offer)} == true))
    
                        return {
                            ${f(ImpromptuProposal::person)}: person,
                            ${f(ImpromptuProposal::otherPerson)}: otherPerson,
                            ${f(ImpromptuProposal::distance)}: d,
                            ${f(ImpromptuProposal::seeks)}: [seek, otherSeek]
                        }
    """.trimIndent()
)

private fun Db.impromptuWithSeeks(varName: String = "impromptu"): String {
    return """
    merge(
        $varName,
        {
            ${f(Impromptu::seek)}: (
                for seek in ${ImpromptuSeek::class.collection()}
                    filter seek.${f(ImpromptuSeek::person)} == $varName.${f(Impromptu::person)}
                        and seek.${f(ImpromptuSeek::offer)} != true
                return seek
            ),
            ${f(Impromptu::offer)}: (
                for offer in ${ImpromptuSeek::class.collection()}
                    filter offer.${f(ImpromptuSeek::person)} == $varName.${f(Impromptu::person)}
                        and offer.${f(ImpromptuSeek::offer)} == true
                return offer
            )
        }
    )
    """
}

const val maxImpromptuDistanceKm = 10.0

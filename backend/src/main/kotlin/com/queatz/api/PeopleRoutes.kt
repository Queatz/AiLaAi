package com.queatz.api

import com.queatz.db.*
import com.queatz.notBlank
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.meOrNull
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.peopleRoutes() {
    authenticate(optional = true) {
        get("/people/{id}/profile") {
            respond {
                val person = db.document(Person::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                PersonProfile(
                    person = person,
                    profile = db.profile(person.id!!),
                    stats = person.stats(),
                    subscription = meOrNull?.id?.let { meId -> db.subscription(meId, person.id!!) }
                )
            }
        }

        get("/profile/url/{url}") {
            respond {
                val profile = db.profileByUrl(parameter("url"))
                    ?: return@respond HttpStatusCode.NotFound.description("Profile not found")
                val person = db.document(Person::class, profile.person!!)
                    ?: return@respond HttpStatusCode.NotFound.description("Person not found")

                PersonProfile(
                    person = person,
                    profile = profile,
                    stats = person.stats(),
                    subscription = meOrNull?.id?.let { meId -> db.subscription(meId, person.id!!) }
                )
            }
        }

        get("/people/{id}/profile/cards") {
            respond {
                val person = db.document(Person::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                val search = call.parameters["search"]

                if (search?.notBlank == null) {
                    db.equippedCardsOfPerson(person.id!!, meOrNull?.id)
                } else {
                    db.activeCardsOfPerson(person.id!!, search)
                }
            }
        }

        get("/people/{id}/items/equipped") {
            respond {
                val person = db.document(Person::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                // todo support search
                val search = call.parameters["search"]

                val inventory = db.inventoryOfPerson(person.id!!)
                db.equippedItemsOfInventory(inventory.id!!)
            }
        }

        get("/people/{id}/groups") {
            respond {
                val person = db.document(Person::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                // todo support search
                val search = call.parameters["search"]

                db.openGroupsOfPerson(person.id!!)
            }
        }

        get("/people/{id}/statuses") {
            respond {
                val person = db.document(Person::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                // todo support search
                val offset = call.parameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 20

                db.statusesOfPerson(
                    person = person.id!!,
                    offset = offset,
                    limit = limit
                )
            }
        }

        get("/people/{id}/stories") {
            respond {
                val person = db.document(Person::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                // todo support search
                val search = call.parameters["search"]

                db.storiesOfPerson(person.id!!, published = true)
            }
        }
    }

    authenticate {
        get("/people") {
            respond {
                val search = call.parameters["search"]?.notBlank?.lowercase()
                val geo = call.parameters["geo"]?.split(",")?.map { it.toDoubleOrNull() ?: 0.0 }?.takeIf { it.size == 2 } ?: me.geo
                val offset = call.parameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 20

                db.peopleNearby(
                    person = me.id!!,
                    geo = geo,
                    name = search,
                    offset = offset,
                    limit = limit
                ).personProfilesForApi()
            }
        }

        post("/people/{id}/subscribe") {
            respond {
                db.subscribe(me.id!!, parameter("id"))
            }
        }

        post("/people/{id}/unsubscribe") {
            respond {
                db.unsubscribe(me.id!!, parameter("id"))
                HttpStatusCode.NoContent
            }
        }
    }
}

private fun List<PersonProfile>.personProfilesForApi() = map {
    it.copy(person = it.person.apply { forApi() })
}

private fun Person.forApi() {
    geo = null
}

private fun Person.stats() = ProfileStats(
    friendsCount = db.friendsCount(id!!),
    cardCount = db.cardsCount(id!!),
    storiesCount = db.storiesCount(id!!),
    subscriberCount = db.subscriberCount(id!!)
)

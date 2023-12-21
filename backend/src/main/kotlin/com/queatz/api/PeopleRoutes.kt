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

    get("/people/{id}/profile") {
        respond {
            val person = db.document(Person::class, parameter("id"))
                ?: return@respond HttpStatusCode.NotFound

            PersonProfile(
                person,
                db.profile(person.id!!),
                ProfileStats(
                    friendsCount = db.friendsCount(person.id!!),
                    cardCount = db.cardsCount(person.id!!),
                )
            )
        }
    }

    get("/profile/url/{url}") {
        respond {
            val profile = db.profileByUrl(parameter("url"))
                ?: return@respond HttpStatusCode.NotFound.description("Profile not found")

            PersonProfile(
                db.document(Person::class, profile.person!!)
                    ?: return@respond HttpStatusCode.NotFound.description("Person not found"),
                profile,
                ProfileStats(
                    friendsCount = db.friendsCount(profile.person!!),
                    cardCount = db.cardsCount(profile.person!!),
                )
            )
        }
    }

    authenticate(optional = true) {
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

        get("/people/{id}/groups") {
            respond {
                val person = db.document(Person::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                // todo support search
                val search = call.parameters["search"]

                db.openGroupsOfPerson(person.id!!)
            }
        }
    }

    authenticate {
        get("/people") {
            respond {
                db.peopleWithName(
                    me.id!!,
                    call.parameters["search"]?.notBlank ?: return@respond HttpStatusCode.BadRequest.description("Missing 'search' parameter"),
                    me.geo
                ).forApi()
            }
        }
    }
}

private fun List<Person>.forApi() = onEach {
    it.forApi()
}

private fun Person.forApi() {
    geo = null
}

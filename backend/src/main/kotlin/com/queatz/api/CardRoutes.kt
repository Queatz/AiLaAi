package com.queatz.api

import com.queatz.*
import com.queatz.db.*
import com.queatz.plugins.*
import com.queatz.push.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlin.reflect.KMutableProperty1

fun Route.cardRoutes() {
    authenticate(optional = true) {
        get("/cards/{id}") {
            respond {
                val card = db.document(Card::class, parameter("id"))

                if (card == null) {
                    HttpStatusCode.NotFound
                } else if (card.isActiveOrMine(meOrNull)) {
                    card
                } else {
                    HttpStatusCode.NotFound
                }
            }
        }

        get("/cards/{id}/cards") {
            respond {
                val card = db.document(Card::class, parameter("id"))

                if (card == null) {
                    HttpStatusCode.NotFound
                } else if (card.isActiveOrMine(meOrNull)) {
                    db.cardsOfCard(card.id!!, meOrNull?.id)
                } else {
                    HttpStatusCode.NotFound
                }
            }
        }

        get("/cards") {
            respond {
                val geo = parameter("geo").split(",").map { it.toDouble() }

                if (geo.size != 2) {
                    return@respond HttpStatusCode.BadRequest.description("'geo' must be an array of size 2")
                }

                val person = meOrNull

                val search = call.parameters["search"]
                    ?.notBlank
                    ?.also { search ->
                        db.insert(
                            Search(
                                search = search,
                                source = if (person == null) SearchSource.Web else null
                            )
                        )
                    }

                val paid = call.parameters["paid"]?.toBoolean()

                db.explore(
                    person = person?.id,
                    geo = geo,
                    search = search,
                    paid = paid,
                    nearbyMaxDistance = defaultNearbyMaxDistanceInMeters,
                    offset = call.parameters["offset"]?.toInt() ?: 0,
                    limit = call.parameters["limit"]?.toInt() ?: 20,
                    public = (call.parameters["public"]?.toBoolean() ?: false) || (person == null)
                )
            }
        }
    }

    authenticate {
        post("/cards") {
            respond {
                val person = me
                val card = call.receive<Card>()

                val parentCard = card.parent?.let {
                    db.document(Card::class, it)
                }

                if (parentCard != null && card.equipped == true) {
                    return@respond HttpStatusCode.BadRequest.description("Card cannot be equipped and have a parent")
                }

                if (card.group != null && db.member(me.id!!, card.group!!) == null) {
                    HttpStatusCode.Forbidden.description("Not a member of this group")
                } else if (parentCard?.isMineOrIAmCollaborator(me) == false) {
                    HttpStatusCode.Forbidden.description("Not a collaborator on this parent")
                } else {
                    if (card.parent == null && card.equipped == null && card.geo == null && card.group == null) {
                        card.offline = true
                    }

                    db.insert(
                        Card(
                            person.id!!,
                            name = card.name,
                            location = card.location,
                            pay = card.pay,
                            conversation = card.conversation,
                            categories = card.categories,
                            options = card.options,
                            parent = parentCard?.id,
                            equipped = card.equipped,
                            offline = card.offline,
                            group = card.group,
                            geo = card.geo,
                            content = card.content,
                            active = card.active ?: false
                        )
                    )
                }
            }
        }

        get("/cards/{id}/group") {
            respond {
                val card = db.document(Card::class, parameter("id"))

                if (card == null) {
                    HttpStatusCode.NotFound
                } else {
                    val people = listOf(me.id!!, card.person!!).distinct()
                    db.group(people) ?: app.createGroup(people)
                }
            }
        }

        get("/cards/{id}/people") {
            respond {
                val card = db.document(Card::class, parameter("id"))
                val person = me

                if (card == null) {
                    HttpStatusCode.NotFound
                } else if (card.active != true && !card.isMineOrIAmCollaborator(person)) {
                    HttpStatusCode.NotFound
                } else {
                    db.people((card.collaborators ?: emptyList()) + card.person!!)
                }
            }
        }

        post("/cards/{id}") {
            respond {
                val card = db.document(Card::class, parameter("id"))
                val person = me

                if (card == null) {
                    HttpStatusCode.NotFound
                } else if (card.person != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    val update = call.receive<Card>()

                    if (update.parent != null) {
                        val parent = db.document(Card::class, update.parent!!)
                            ?: return@respond HttpStatusCode.NotFound.description("Parent card not found")

                        if (!parent.isMineOrIAmCollaborator(me)) {
                            return@respond HttpStatusCode.Forbidden.description("Not a collaborator on this card")
                        }
                    }

                    if (card.group != null && db.member(me.id!!, card.group!!) == null) {
                        return@respond HttpStatusCode.Forbidden.description("Not a member of this group")
                    }

                    if (update.parent != null && update.equipped == true) {
                        return@respond HttpStatusCode.BadRequest.description("Card cannot be equipped and have a parent")
                    }

                    val parentCard = card.parent?.let { parent -> db.document(Card::class, parent) }

                    // Change owner
                    if (update.person != null && update.person != card.person) {
                        if (parentCard != null) {
                            // Remove the card from a parent if it's not accessible to the new owner
                            if (parentCard.person != update.person && parentCard.collaborators?.contains(update.person!!) != true) {
                                card.parent = null
                                card.offline = true
                            }
                        }

                        val cardWasActive = card.active == true

                        // Deactivate the card
                        card.active = false

                        // Always unequip the card
                        card.equipped = null

                        // Card is always removed because it is deactivated
                        if (cardWasActive && parentCard != null) {
                            notifyCardRemovedFromCard(person, parentCard.people(), parentCard, card)
                        }

                        val collaborators = card.collaborators ?: emptyList()

                        // Add current owner as collaborator if they have inner cards
                        if (person.id !in collaborators && db.allCardsOfCard(card.id!!)
                                .any { it.person == person.id }
                        ) {
                            card.collaborators = collaborators + person.id!!
                        }

                        // Change the owner
                        card.person = update.person

                        // Finish to prevent anything else from changing
                        return@respond db.update(card)
                    }

                    // Remove any cards of added collaborators
                    if (update.collaborators != null) {
                        val removedCollaborators =
                            (card.collaborators ?: emptyList()).toSet() - update.collaborators!!.toSet()
                        removedCollaborators.forEach { removedPerson ->
                            notifyCollaboratorRemoved(person, card.people(), card, removedPerson)
                        }
                        if (removedCollaborators.isNotEmpty()) {
                            val childCards = db.allCardsOfCard(card.id!!)
                            childCards.forEach { childCard ->
                                if (childCard.person in removedCollaborators) {
                                    childCard.parent = null
                                    childCard.offline = true
                                    db.update(childCard)
                                }
                            }
                        }
                        val addedCollaborators =
                            update.collaborators!!.toSet() - (card.collaborators ?: emptyList()).toSet()
                        addedCollaborators.forEach { addedPerson ->
                            notifyCollaboratorAdded(person, card.people() + addedCollaborators, card, addedPerson)
                        }
                    }

                    // Collaboration notifications

                    if (update.parent != null && update.parent != card.parent) {
                        val newParentCard = db.document(Card::class, update.parent!!)!!

                        if (card.active == true) {
                            notifyCardAddedToCard(person, newParentCard.people(), newParentCard, card)
                        }
                    } else if (card.parent != null) {
                        val parentCard = db.document(Card::class, card.parent!!)!!

                        if (update.active != null && update.active != card.active) {
                            if (update.active == true) {
                                notifyCardAddedToCard(person, parentCard.people(), parentCard, card)
                            } else if (update.active == false) {
                                notifyCardRemovedFromCard(person, parentCard.people(), parentCard, card)
                            }
                        } else if (card.active == true) {
                            // todo also notify changes to the actual card, not just the parent
                            if (update.name != null && update.name != card.name) {
                                notifyCardInCardUpdated(
                                    person,
                                    parentCard.people(),
                                    parentCard,
                                    card,
                                    CollaborationEventDataDetails.Name
                                )
                            }
                            if (update.photo != null && update.photo != card.photo) {
                                notifyCardInCardUpdated(
                                    person,
                                    parentCard.people(),
                                    parentCard,
                                    card,
                                    CollaborationEventDataDetails.Photo
                                )
                            }
                            if (update.video != null && update.video != card.video) {
                                notifyCardInCardUpdated(
                                    person,
                                    parentCard.people(),
                                    parentCard,
                                    card,
                                    CollaborationEventDataDetails.Video
                                )
                            }
                            if (update.conversation != null && update.conversation != card.conversation) {
                                notifyCardInCardUpdated(
                                    person,
                                    parentCard.people(),
                                    parentCard,
                                    card,
                                    CollaborationEventDataDetails.Conversation
                                )
                            }
                            if (update.location != null && update.location != card.location) {
                                notifyCardInCardUpdated(
                                    person,
                                    parentCard.people(),
                                    parentCard,
                                    card,
                                    CollaborationEventDataDetails.Location
                                )
                            }
                            if (update.content != null && update.content != card.content) {
                                notifyCardInCardUpdated(
                                    person,
                                    parentCard.people(),
                                    parentCard,
                                    card,
                                    CollaborationEventDataDetails.Content
                                )
                            }
                        }
                    }

                    if (card.active == true) {
                        if (update.name != null && update.name != card.name) {
                            notifyCardUpdated(person, card.people(), card, CollaborationEventDataDetails.Name)
                        }
                        if (update.photo != null && update.photo != card.photo) {
                            notifyCardUpdated(person, card.people(), card, CollaborationEventDataDetails.Photo)
                        }
                        if (update.video != null && update.video != card.video) {
                            notifyCardUpdated(person, card.people(), card, CollaborationEventDataDetails.Video)
                        }
                        if (update.conversation != null && update.conversation != card.conversation) {
                            notifyCardUpdated(person, card.people(), card, CollaborationEventDataDetails.Conversation)
                        }
                        if (update.location != null && update.location != card.location) {
                            notifyCardUpdated(person, card.people(), card, CollaborationEventDataDetails.Location)
                        }
                        if (update.content != null && update.content != card.content) {
                            notifyCardUpdated(person, card.people(), card, CollaborationEventDataDetails.Content)
                        }
                    }

                    fun <T> check(prop: KMutableProperty1<Card, T>, doOnSet: ((T) -> Unit)? = null) {
                        val value = prop.get(update)
                        if (value != null) {
                            prop.set(card, value)
                            doOnSet?.invoke(value)
                        }
                    }

                    val previousParent = card.parent

                    check(Card::active)
                    check(Card::geo) {
                        card.parent = update.parent
                        card.offline = update.offline
                    }
                    check(Card::location)
                    check(Card::collaborators)
                    check(Card::categories)
                    check(Card::name)
                    check(Card::conversation)
                    check(Card::content)
                    check(Card::options)
                    check(Card::pay) {
                        if (it?.pay.isNullOrBlank()) {
                            card.pay = null
                        }
                    }
                    check(Card::npc) {
                        if (it?.name.isNullOrBlank()) {
                            card.npc = null
                        }
                    }
                    check(Card::photo) {
                        card.video = update.video
                    }
                    check(Card::video) {
                        card.photo = update.photo
                    }
                    check(Card::background)
                    check(Card::offline) {
                        card.parent = update.parent
                        card.equipped = update.equipped
                        card.geo = update.geo
                        card.group = null
                    }
                    check(Card::parent) {
                        card.equipped = update.equipped
                        card.offline = update.offline
                        card.geo = update.geo
                        card.group = null
                    }
                    check(Card::equipped) {
                        card.parent = update.parent
                        card.offline = update.offline
                        card.group = null
                    }

                    check(Card::group) {
                        card.parent = update.parent
                        card.offline = update.offline
                        card.geo = update.geo
                        card.equipped = update.equipped
                    }

                    if (card.parent == null && previousParent != null && card.active == true) {
                        val parentCard = db.document(Card::class, previousParent)!!
                        notifyCardRemovedFromCard(person, parentCard.people(), parentCard, card)
                    }

                    db.update(card)
                }
            }
        }

        post("/cards/{id}/upgrade") {
            respond {
                val body = call.receive<CardUpgradeBody>()
                val card = db.document(Card::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound
                val currentLevel = card.level ?: 0
                val account = accounts.account(me.id!!)

                if ((body.level - 1) != currentLevel) {
                    return@respond HttpStatusCode.BadRequest.description("Level must increase by exactly 1")
                }

                if (!accounts.canUpgradeCard(account, card)) {
                    return@respond HttpStatusCode.BadRequest.description("Insufficient points")
                }

                if (accounts.upgradeCard(account, card)) {
                    HttpStatusCode.OK
                } else {
                    HttpStatusCode.BadRequest
                }
            }
        }

        get("/cards/{id}/upgrade") {
            respond {
                val card = db.document(Card::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound
                val currentLevel = card.level ?: 0
                val account = accounts.account(me.id!!)

                if (!accounts.canUpgradeCard(account, card)) {
                    return@respond HttpStatusCode.BadRequest.description("Insufficient points")
                }

                CardUpgradeDetails(
                    level = currentLevel + 1,
                    points = accounts.upgradeCardCost(card),
                    available = accounts.canUpgradeCard(account, card)
                )
            }
        }

        post("/cards/{id}/photo") {
            respond {
                val card = db.document(Card::class, parameter("id"))
                val person = me

                if (card == null) {
                    HttpStatusCode.NotFound
                } else if (card.person!!.asKey() != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    call.receiveFile("photo", "card-${card.id}") { it, _ ->
                        card.photo = it
                        db.update(card)

                        if (card.active == true) {
                            card.parent?.let { db.document(Card::class, it) }?.let { parentCard ->
                                notifyCardInCardUpdated(
                                    person,
                                    parentCard.people(),
                                    parentCard,
                                    card,
                                    CollaborationEventDataDetails.Photo
                                )
                            }
                            notifyCardUpdated(person, card.people(), card, CollaborationEventDataDetails.Photo)
                        }
                    }
                }
            }
        }

        post("/cards/{id}/photo/generate") {
            respond {
                val card = db.document(Card::class, parameter("id"))
                val person = me

                if (card == null) {
                    HttpStatusCode.NotFound
                } else if (card.person!!.asKey() != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    launch {
                        val url = ai.photo(
                            "card-${card.id!!}",
                            buildList {
                                add(
                                    TextPrompt(
                                        card.name!!
                                    )
                                )

                                val message = card.getConversation().message

                                if (message.isNotBlank()) {
                                    add(
                                        TextPrompt(message, .5)
                                    )
                                }

                                if (!card.location.isNullOrBlank()) {
                                    add(
                                        TextPrompt(card.location!!, .25)
                                    )
                                }
                            }
                        )

                        card.photo = url
                        db.update(card)

                        if (card.active == true) {
                            card.parent?.let { db.document(Card::class, it) }?.let { parentCard ->
                                notifyCardInCardUpdated(
                                    person,
                                    parentCard.people(),
                                    parentCard,
                                    card,
                                    CollaborationEventDataDetails.Photo
                                )
                            }
                            notifyCardUpdated(person, card.people(), card, CollaborationEventDataDetails.Photo)
                        }
                    }

                    HttpStatusCode.OK
                }
            }
        }

        post("/cards/{id}/video") {
            respond {
                val card = db.document(Card::class, parameter("id"))
                val person = me

                if (card == null) {
                    HttpStatusCode.NotFound
                } else if (card.person!!.asKey() != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    call.receiveFile("photo", "card-${card.id}") { it, _ ->
                        card.video = it
                        db.update(card)

                        if (card.active == true) {
                            card.parent?.let { db.document(Card::class, it) }?.let { parentCard ->
                                notifyCardInCardUpdated(
                                    person,
                                    parentCard.people(),
                                    parentCard,
                                    card,
                                    CollaborationEventDataDetails.Video
                                )
                            }
                            notifyCardUpdated(person, card.people(), card, CollaborationEventDataDetails.Video)
                        }
                    }
                }
            }
        }

        post("/cards/{id}/content/photos") {
            respond {
                val cardId = parameter("id")
                var result: List<String>? = null
                call.receiveFiles("photo", "card-content-${cardId}") { photosUrls, _ ->
                    result = photosUrls
                }
                result ?: HttpStatusCode.InternalServerError
            }
        }

        post("/cards/{id}/content/audio") {
            respond {
                val cardId = parameter("id")
                var result: String? = null
                call.receiveFile("audio", "card-content-${cardId}") { audioUrl, _ ->
                    result = audioUrl
                }
                result ?: HttpStatusCode.InternalServerError
            }
        }

        post("/cards/{id}/delete") {
            respond {
                val card = db.document(Card::class, parameter("id"))
                val person = me

                if (card == null) {
                    HttpStatusCode.NotFound
                } else if (card.person!!.asKey() != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    db.allCardsOfCard(card.id!!).forEach {
                        it.parent = card.parent
                        it.offline = card.offline
                        it.geo = card.geo
                        it.equipped = card.equipped
                        db.update(it)
                    }

                    if (card.parent != null) {
                        db.document(Card::class, card.parent!!)?.let { parentCard ->
                            notifyCardRemovedFromCard(
                                person,
                                parentCard.people(),
                                parentCard,
                                card
                            )
                        }
                    }

                    db.delete(card)
                    HttpStatusCode.NoContent
                }
            }
        }

        post("/cards/{id}/save") {
            respond {
                val card = db.document(Card::class, parameter("id"))
                val person = me

                if (card == null) {
                    HttpStatusCode.NotFound
                } else {
                    db.saveCard(person.id!!, card.id!!)
                    HttpStatusCode.OK
                }
            }
        }

        post("/cards/{id}/unsave") {
            respond {
                val card = db.document(Card::class, parameter("id"))
                val person = me

                if (card == null) {
                    HttpStatusCode.NotFound
                } else {
                    db.unsaveCard(person.id!!, card.id!!)
                    HttpStatusCode.OK
                }
            }
        }
    }
}

suspend fun notifyCardUpdated(me: Person, people: Set<String>, card: Card, details: CollaborationEventDataDetails) {
    notifyCollaborators(
        me,
        people,
        CollaborationPushData(
            Person().apply {
                id = me.id
                name = me.name
            },
            Card().apply {
                id = card.id
                name = card.name
            },
            CollaborationEvent.UpdatedCard,
            CollaborationEventData(details = details)
        )
    )
}

suspend fun notifyCardInCardUpdated(
    me: Person,
    people: Set<String>,
    card: Card,
    updatedCard: Card,
    details: CollaborationEventDataDetails
) {
    notifyCollaborators(
        me,
        people,
        CollaborationPushData(
            Person().apply {
                id = me.id
                name = me.name
            },
            Card().apply {
                id = card.id
                name = card.name
            },
            CollaborationEvent.UpdatedCard,
            CollaborationEventData(updatedCard, details = details)
        )
    )
}

suspend fun notifyCardAddedToCard(me: Person, people: Set<String>, card: Card, addedCard: Card) {
    notifyCollaborators(
        me,
        people,
        CollaborationPushData(
            Person().apply {
                id = me.id
                name = me.name
            },
            Card().apply {
                id = card.id
                name = card.name
            },
            CollaborationEvent.AddedCard,
            CollaborationEventData(addedCard)
        )
    )
}

suspend fun notifyCardRemovedFromCard(me: Person, people: Set<String>, card: Card, removedCard: Card) {
    notifyCollaborators(
        me,
        people,
        CollaborationPushData(
            Person().apply {
                id = me.id
                name = me.name
            },
            Card().apply {
                id = card.id
                name = card.name
            },
            CollaborationEvent.RemovedCard,
            CollaborationEventData(removedCard)
        )
    )
}

suspend fun notifyCollaboratorAdded(me: Person, people: Set<String>, card: Card, personId: String) {
    notifyCollaborators(
        me,
        people,
        CollaborationPushData(
            Person().apply {
                id = me.id
                name = me.name
            },
            Card().apply {
                id = card.id
                name = card.name
            },
            CollaborationEvent.AddedPerson,
            CollaborationEventData(person = Person().apply {
                id = personId
                name = db.document(Person::class, me.id!!)?.name
            })
        )
    )
}

suspend fun notifyCollaboratorRemoved(me: Person, people: Set<String>, card: Card, personId: String) {
    notifyCollaborators(
        me,
        people,
        CollaborationPushData(
            Person().apply {
                id = me.id
                name = me.name
            },
            Card().apply {
                id = card.id
                name = card.name
            },
            CollaborationEvent.RemovedPerson,
            CollaborationEventData(person = Person().apply {
                id = personId
                name = db.document(Person::class, personId)?.name
            })
        )
    )
}

suspend fun notifyCollaborators(me: Person, people: Set<String>, collaborationPushData: CollaborationPushData) {
    val pushData = PushData(action = PushAction.Collaboration, data = collaborationPushData)
    db.peopleDevices(people.filter { it != me.id }.also {
        println("Notifying ${it.size} collaborator(s)")
    }).forEach { device ->
        println("Notifying collaborator ${device.person}'s ${device.type} device")
        push.sendPush(device, pushData)
    }
}

fun Card.people() = (collaborators ?: emptyList()).toSet() + person!!

fun Card.isActiveOrMine(me: Person?) = (me != null && isMineOrIAmCollaborator(me)) || active == true

fun Card.isMineOrIAmCollaborator(me: Person) = person!!.asKey() == me.id!! || collaborators?.contains(me.id!!) == true

fun Card.getConversation() = json.decodeFromString<ConversationItem>(conversation ?: "{}")

package com.queatz.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.queatz.db.Person
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.pipeline.*

private val jwt = object {
    val secret = secrets.jwt.secret
    val issuer = "http://0.0.0.0:8080/"
    val audience = "http://0.0.0.0:8080/"
    val realm = "Ai La Ai"
}

fun Application.configureSecurity() {
    authentication {
        jwt {
            realm = jwt.realm

            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwt.secret))
                    .withAudience(jwt.audience)
                    .withIssuer(jwt.issuer)
                    .build()
            )

            validate { credential ->
                if (credential.payload.audience.contains(jwt.audience))
                    JWTPrincipal(credential.payload)
                else
                    null
            }
        }
    }
}

fun jwt(id: String) = JWT.create()
    .withAudience(jwt.audience)
    .withIssuer(jwt.issuer)
    .withClaim("id", id)
    //.withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(180)))
    .sign(Algorithm.HMAC256(jwt.secret))!!

val PipelineContext<*, ApplicationCall>.me
    get() = call.principal<JWTPrincipal>()!!
        .getClaim("id", String::class)!!
        .let { db.document(Person::class, it) }!!

val PipelineContext<*, ApplicationCall>.meOrNull
    get() = call.principal<JWTPrincipal>()
        ?.getClaim("id", String::class)
        ?.let { db.document(Person::class, it) }

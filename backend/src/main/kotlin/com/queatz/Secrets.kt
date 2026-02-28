package com.queatz

import kotlinx.serialization.Serializable

@Serializable
data class Secrets(
    val config: SecretsConfig,
    val jwt: SecretsJwt,
    val hms: SecretsHms,
    val gms: SecretsGms,
    val apns: SecretsApns,
    val dezgo: SecretsDezgo,
    val openAi: SecretsOpenAi,
    val videoSdk: SecretsVideoSdk,
    val google: SecretsGoogle,
)

@Serializable
data class SecretsGoogle(
    val apiKey: String
)

@Serializable
data class SecretsConfig(
    val domain: String
)

@Serializable
data class SecretsVideoSdk(
    val apiKey: String,
    val secret: String,
)

@Serializable
data class SecretsHms(
    val appId: String,
    val clientId: String,
    val clientSecret: String
)

@Serializable
data class SecretsGms(
    val appId: String,
    val clientId: String,
    val clientEmail: String,
    val privateKeyId: String,
    val privateKey: String
)

@Serializable
data class SecretsJwt(
    val secret: String
)

@Serializable
data class SecretsApns(
    val privateKey: String,
    val privateKeyId: String,
    val teamId: String,
    val topic: String,
)

@Serializable
data class SecretsDezgo(
    val key: String
)

@Serializable
data class SecretsOpenAi(
    val key: String
)

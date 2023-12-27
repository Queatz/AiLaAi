package com.queatz

import com.queatz.plugins.json
import com.queatz.plugins.secrets
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.logging.Logger
import kotlin.random.Random
import kotlin.text.Charsets.UTF_8
import kotlin.time.Duration.Companion.minutes

class Ai {

    companion object {
        private const val endpoint = "https://api.dezgo.com/text2image"
        private val defaultStylePresets = listOf(
            "absolute_reality_1_8_1",
            "anything_5_0",
            "openniji",
            "pastel_mix",
            "furrytoonmix",
            "eimis_anime_diffusion_1",
            "realistic_vision_5_1",
            "toonify_2",
            "blood_orange_mix",
            "dreamix_1",
            "abyss_orange_mix_2",
            "duchaiten_anime",
            "duchaiten_dreamworld",
            "icbinp_seco",
            "something_2",
            "basil_mix",
            "kidsmix",
            "icbinp_seco"
        )
        private const val height = 416
        private const val width = 608
        private val basePrompt = TextPrompt(
            "lovely, beautiful, cute, happy, sweet, natural",
            .125
        )
        private val negativePrompt = "ugly, tiling, poorly drawn hands, poorly drawn feet, poorly drawn face, out of frame, extra limbs, disfigured, deformed, body out of frame, blurry, bad anatomy, blurred, watermark, grainy, signature, cut off, draft, weapon"
    }

    private val http = HttpClient(CIO) {
        expectSuccess = true
        engine {
            requestTimeout = 10.minutes.inWholeMilliseconds
        }
    }

    suspend fun photo(prefix: String, prompts: List<TextPrompt>): String {
        val model = defaultStylePresets.random()
        val body = json.encodeToString(
            DezgoPrompt(
                prompt = (prompts + basePrompt).joinToString { it.text },
                negativePrompt = negativePrompt,
                model = model,
                height = height,
                width = width,
            )
        )

        Logger.getAnonymousLogger().info("Sending text-to-image prompt: $body")

        return http.post(endpoint) {
            header("X-Dezgo-Key", secrets.dezgo.key)
            accept(ContentType.Image.PNG)
            contentType(ContentType.Application.Json.withCharset(UTF_8))
            setBody(body)
        }.body<ByteArray>().let {
            save("$prefix-$model", it)
        }
    }

    private suspend fun save(prefix: String, image: ByteArray): String {
        val folder = "./static/ai"

        if (!File(folder).isDirectory) {
            File(folder).mkdirs()
        }

        val fileName = "$prefix-${Random.nextInt(100_000_000, 999_999_999)}-ai.jpg"
        val file = File("$folder/$fileName")

        withContext(Dispatchers.IO) {
            file.outputStream().write(image)
        }

        return "${folder.drop(1)}/$fileName"
    }
}

@Serializable
data class DezgoPrompt(
    @SerialName("negative_prompt")
    val negativePrompt: String,
    val prompt: String,
    val model: String?,
    val height: Int,
    val width: Int,
    val sampler: String? = "dpmpp_2m_karras",
    val steps: Int = 25,
    val guidance: Int = 7,
    val format: String = "jpg",
)

@Serializable
data class TextPrompt(
    val text: String,
    val weight: Double = 1.0
)

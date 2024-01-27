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
        val styles = listOf(
            "Absolute Reality" to "absolute_reality_1_8_1",
            "Anything" to "anything_5_0",
            "Niji" to "openniji",
            "Yesmix" to "yesmix_4",
            "Pastel" to "pastel_mix",
            "Animal" to "furrytoonmix",
            "Eimis" to "eimis_anime_diffusion_1",
            "Realistic Vision" to "realistic_vision_5_1",
            "Toon" to "toonify_2",
            "Real Cartoon" to "realcartoonanime_10",
            "Real Cartoon 3D" to "realcartoon3d_13",
            "Blood Orange" to "blood_orange_mix",
            "Dreamix" to "dreamix_1",
            "Abyss Orange" to "abyss_orange_mix_2",
            "Duc Haiten" to "duchaiten_anime",
            "Duc Haiten Dream" to "duchaiten_dreamworld",
            "Icbinp Seco" to "icbinp_seco",
            "Something" to "something_2",
            "Basil" to "basil_mix",
            "Kids" to "kidsmix",
            "Blue Pencil (HD)" to "bluepencilxl_1024px",
            "Dreamshaper (HD)" to "dreamshaperxl_1024px",
            "Juggernaut (HD)" to "juggernautxl_1024px",
            "Juggernaut 8 (HD)" to "juggernautxl_rundiffusion_8_1024px",
            "Sdxl (HD)" to "sdxl_1024px",
            "T-Shirt (HD)" to "tshirtdesignredmond_1024px",
        )
        private val defaultStylePresets = styles.map { it.second }
        private const val endpoint = "https://api.dezgo.com/text2image"
        private const val endpointXl = "https://api.dezgo.com/text2image_sdxl"
        private const val height = 416
        private const val width = 608
        private const val heightXl = 832
        private const val widthXl = 1216
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

    suspend fun photo(prefix: String, prompts: List<TextPrompt>, style: String? = null): String {
        val model = style?.takeIf { it in defaultStylePresets } ?: defaultStylePresets.random()

        val isXl = model.endsWith("_1024px")

        val body = json.encodeToString(
            DezgoPrompt(
                prompt = (prompts + basePrompt).joinToString { it.text },
                negativePrompt = negativePrompt,
                model = model,
                refiner = true,
                height = if (isXl) heightXl else height,
                width = if (isXl) widthXl else width,
            )
        )

        Logger.getAnonymousLogger().info("Sending text-to-image prompt: $body")

        return http.post(if (isXl) endpointXl else endpoint) {
            header("X-Dezgo-Key", secrets.dezgo.key)
            accept(ContentType.Image.JPEG)
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
    val refiner: Boolean? = null,
    val guidance: Int = 7,
    val format: String = "jpg",
)

@Serializable
data class TextPrompt(
    val text: String,
    val weight: Double = 1.0
)

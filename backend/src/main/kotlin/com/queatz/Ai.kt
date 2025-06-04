package com.queatz

import com.queatz.plugins.json
import com.queatz.plugins.secrets
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.bodyAsText
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
            // OpenAI image generation via Responses API
            "OpenAI" to "openai",
            "Flux (Text Rendering, HD)" to ":flux",
            "Dreaming (HD)" to "dreamshaperxl_lightning_1024px",
            "Starlight (HD)" to "envy_starlight_xl_01_lightning_1024px",
            "Juggernaut 9 (HD)" to "juggernautxl_9_lightning_1024px",
            "Blue Pencil (HD)" to "bluepencilxl_1024px",
            "Dreamshaper (HD)" to "dreamshaperxl_1024px",
            "Juggernaut (HD)" to "juggernautxl_1024px",
            "Juggernaut 8 (HD)" to "juggernautxl_rundiffusion_8_1024px",
            "Sdxl (HD)" to "sdxl_1024px",
            "T-Shirt (HD)" to "tshirtdesignredmond_1024px",
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
        )
        private val defaultStylePresets = styles.map { it.second }
        private const val endpoint = "https://api.dezgo.com/text2image"
        private const val endpointXl = "https://api.dezgo.com/text2image_sdxl"
        private const val endpointXlLightning = "https://api.dezgo.com/text2image_sdxl_lightning"
        private const val endpointFlux = "https://api.dezgo.com/text2image_flux"
        private const val endpointRemoveBackground = "https://api.dezgo.com/remove-background"
        private const val height = 416
        private const val width = 608
        private const val square = 512
        private const val heightXl = 832
        private const val widthXl = 1216
        private const val squareXl = 1024
        private val basePrompt = TextPrompt(
            "lovely, beautiful, cute, happy, sweet, natural",
            .125
        )
        private val negativePrompt =
            "tiling, poorly drawn hands, poorly drawn feet, poorly drawn face, out of frame, extra limbs, disfigured, deformed, body out of frame, blurry, bad anatomy, blurred, watermark, grainy, signature, cut off, draft, weapon"
    }

    private val http = HttpClient(CIO) {
        expectSuccess = true
        engine {
            requestTimeout = 10.minutes.inWholeMilliseconds
        }
    }

    val String.isFlux get() = this == ":flux"
    val String.isXlLightning get() = endsWith("_lightning_1024px")
    val String.isXl get() = endsWith("_1024px") || isFlux

    suspend fun photo(
        prefix: String,
        prompts: List<TextPrompt>,
        style: String? = null,
        aspect: Double = 1.5,
        processBytes: (ByteArray, ContentType) -> ByteArray = { it, _ -> it },
        transparentBackground: Boolean = false,
        crop: Boolean = false
    ): Pair<String, Pair<Int, Int>?> {
        val model = style?.takeIf { it in defaultStylePresets } ?: defaultStylePresets.filter {
            it.isXlLightning || it.isXl
        }.random()

        // Handle OpenAI image generation
        if (model == "openai") {
            val openAiClient = OpenAi()
            val promptText = prompts.joinToString { it.text }
            val imageBytesRaw = openAiClient.image(promptText, transparentBackground)
                ?: throw IllegalStateException("OpenAI image generation failed")
            val processedBytes = processBytes(imageBytesRaw, ContentType.Image.PNG)
            val (finalBytes, dimensions) = if (transparentBackground && crop) {
                processedBytes.cropTransparentBackground()
            } else {
                Pair(processedBytes, null)
            }
            val path = save("$prefix-$model", finalBytes, "png")
            return Pair(path, dimensions)
        }

        val isFlux = model.isFlux
        val isXlLightning = model.isXlLightning
        val isXl = model.isXl

        val height = if (isXl) {
            if (aspect == 1.0) squareXl else heightXl
        } else {
            if (aspect == 1.0) square else height
        }
        val width = if (isXl) {
            if (aspect == 1.0) squareXl else widthXl
        } else {
            if (aspect == 1.0) square else width
        }

        val ext = if (transparentBackground) "png" else "jpg"

        val body = when {
            isFlux -> {
                json.encodeToString(
                    DezgoFluxPrompt(
                        prompt = prompts.joinToString { it.text },
                        height = if (aspect < 1.0) width else height,
                        width = if (aspect < 1.0) height else width,
                        transparentBackground = transparentBackground,
                        format = ext
                    )
                )
            }

            else -> {
                json.encodeToString(
                    DezgoPrompt(
                        prompt = prompts.joinToString { it.text },
                        negativePrompt = negativePrompt,
                        model = model,
                        steps = 25.takeIf { !isXlLightning },
                        refiner = true.takeIf { !isXlLightning },
                        guidance = if (isXlLightning) 2f else 7f,
                        height = if (aspect < 1.0) width else height,
                        width = if (aspect < 1.0) height else width,
                        transparentBackground = if (isXl) transparentBackground else null,
                        format = ext
                    )
                )
            }
        }

        Logger.getAnonymousLogger().info("Sending text-to-image prompt: $body")

        val contentType = if (ext == "png") ContentType.Image.PNG else ContentType.Image.JPEG

        val imageBytes = http.post(
            when {
                isFlux -> endpointFlux
                isXlLightning -> endpointXlLightning
                isXl -> endpointXl
                else -> endpoint
            }
        ) {
            header("X-Dezgo-Key", secrets.dezgo.key)
            accept(contentType)
            contentType(ContentType.Application.Json.withCharset(UTF_8))
            setBody(body)
        }.body<ByteArray>()
            .let {
                processBytes(it, contentType)
            }

        // Apply cropping if needed
        val (finalBytes, dimensions) = if (transparentBackground && crop) {
            // Crop the image based on transparency
            imageBytes.cropTransparentBackground()
        } else {
            // No cropping, just return the original bytes with null dimensions
            Pair(imageBytes, null)
        }

        val path = save("$prefix-$model", finalBytes, ext).let { path ->
            // Dezgo doesn't support transparent for non-XL models out of the box
            if (transparentBackground && !isXl) {
                runCatching {
                    removeBackground(File(".$path"))
                }.onFailure {
                    it.printStackTrace()
                    (it as? ServerResponseException)?.let {
                        println("Remove background error:")
                        println(it.response.bodyAsText())
                    }
                }.getOrDefault(path)
            } else {
                path
            }
        }

        return Pair(path, dimensions)
    }

    suspend fun removeBackground(photo: File): String {
        return http.post(endpointRemoveBackground) {
            header("X-Dezgo-Key", secrets.dezgo.key)
            accept(ContentType.Image.PNG)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("mode", "transparent")
                        append(
                            key = "image",
                            value = photo.readBytes(),
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
                            }
                        )
                    }
                )
            )
        }.body<ByteArray>().let {
            save("photo", it, "png")
        }
    }

    private suspend fun save(prefix: String, image: ByteArray, fileExtension: String = "jpg"): String {
        val folder = "./static/ai"

        if (!File(folder).isDirectory) {
            File(folder).mkdirs()
        }

        val fileName = "$prefix-${Random.nextInt(100_000_000, 999_999_999)}-ai.$fileExtension"
        val file = File("$folder/$fileName")

        withContext(Dispatchers.IO) {
            file.outputStream().write(image)
        }

        return "${folder.drop(1)}/$fileName"
    }
}

@Serializable
data class DezgoFluxPrompt(
    val prompt: String,
    @SerialName("transparent_background")
    val transparentBackground: Boolean? = null,
    val height: Int,
    val width: Int,
    val steps: Int = 8,
    val format: String = "jpg",
)

@Serializable
data class DezgoPrompt(
    @SerialName("negative_prompt")
    val negativePrompt: String,
    @SerialName("transparent_background")
    val transparentBackground: Boolean? = null,
    val prompt: String,
    val model: String?,
    val height: Int,
    val width: Int,
    val sampler: String? = "dpmpp_2m_karras",
    val steps: Int? = null,
    val refiner: Boolean? = null,
    val guidance: Float = 7f,
    val format: String = "jpg",
)

@Serializable
data class TextPrompt(
    val text: String,
    val weight: Double = 1.0
)

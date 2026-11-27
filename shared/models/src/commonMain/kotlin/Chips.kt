package com.queatz.db

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Instant

@Serializable
enum class ChipFormatType {
    TEXT,
    NUMBER,
    FIXED_2,
    JSON,
}

@Serializable
class ChipFeed(
    var url: String? = null,
    var value: String? = null,
    var syncedAt: Instant? = null,
    var attemptedAt: Instant? = null,
    var statusCode: Int? = null,
) : Model()

@Serializable
class PersonChip(
    var person: String? = null,
    var feed: String? = null,
    var name: String? = null,
    var url: String? = null,
    var format: ChipFormatType? = null,
    var prefix: String? = null,
    var color: String? = null,
    var jsonPath: String? = null,
    var isAbove: Boolean? = null,
    var order: Int? = null,
) : Model()

@Serializable
data class PersonChipExtended(
    val chip: PersonChip,
    val value: String? = null,
    val syncedAt: Instant? = null,
)

@Serializable
data class ChipOrderItem(
    val id: String,
    val order: Int,
    val isAbove: Boolean,
)

@Serializable
data class ReorderChipsBody(
    val items: List<ChipOrderItem>,
)

@Serializable
data class ImportChipsBody(
    val chips: List<PersonChip>,
)

@Serializable
data class ChipPreviewRequest(
    val url: String,
)

@Serializable
data class ChipPreviewResponse(
    val value: String? = null,
    val syncedAt: Instant? = null,
)

sealed interface ChipJsonParsedResult {
    data class BooleanValue(
        val value: Boolean,
    ) : ChipJsonParsedResult

    data object NullValue : ChipJsonParsedResult

    data object UndefinedValue : ChipJsonParsedResult

    data class TextValue(
        val value: String,
    ) : ChipJsonParsedResult
}

enum class ChipValueKind {
    Normal,
    BooleanTrue,
    BooleanFalse,
    Null,
    Undefined,
}

data class ChipEvaluatedDisplay(
    val prefix: String = "",
    val text: String = "",
    val kind: ChipValueKind = ChipValueKind.Normal,
)

data class ChipRgb(
    val r: Int,
    val g: Int,
    val b: Int,
) {
    fun toHex(): String =
        "#${r.coerceIn(0, 255).toHexByte()}${g.coerceIn(0, 255).toHexByte()}${b.coerceIn(0, 255).toHexByte()}"

    fun toArgbInt(
        alpha: Int = 0xFF,
    ): Int =
        ((alpha and 0xFF) shl 24) or
            ((r and 0xFF) shl 16) or
            ((g and 0xFF) shl 8) or
            (b and 0xFF)
}

data class ChipGradientPalette(
    val stops: List<ChipRgb>,
    val borderStops: List<ChipRgb>,
    val useLightForeground: Boolean,
)

val chipPastelHexColors = listOf(
    "#FFB3BA", // Pastel Pink
    "#FFDFBA", // Pastel Orange
    "#FFFFBA", // Pastel Yellow
    "#BAFFC9", // Pastel Green
    "#BAE1FF", // Pastel Blue
    "#E8BAFF", // Pastel Purple
    "#E0E0E0", // Soft Grey
)

const val defaultChipHexColor = "#E0E0E0"

private val chipJsonParser = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun evaluateChipJson(
    rawString: String,
    jsonPath: String,
): ChipJsonParsedResult {
    val trimmed = rawString.trim()
    if (trimmed.isEmpty()) return ChipJsonParsedResult.UndefinedValue

    val root = try {
        chipJsonParser.parseToJsonElement(trimmed)
    } catch (_: Exception) {
        return ChipJsonParsedResult.UndefinedValue
    }

    val segments = jsonPath
        .replace('[', '.')
        .replace(']', '.')
        .split('.')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    var current: JsonElement? = root
    for (segment in segments) {
        if (current == null) break
        current = when (current) {
            is JsonObject -> current[segment]
            is JsonArray -> {
                val index = segment.toIntOrNull()
                if (index != null && index in current.indices) {
                    current[index]
                } else {
                    null
                }
            }
            else -> null
        }
    }

    if (current == null) {
        return ChipJsonParsedResult.UndefinedValue
    }

    return when (current) {
        is JsonNull -> ChipJsonParsedResult.NullValue
        is JsonPrimitive -> {
            val b = current.booleanOrNull
            if (!current.isString && b != null) {
                ChipJsonParsedResult.BooleanValue(value = b)
            } else if (current.content.equals("true", ignoreCase = true)) {
                ChipJsonParsedResult.BooleanValue(value = true)
            } else if (current.content.equals("false", ignoreCase = true)) {
                ChipJsonParsedResult.BooleanValue(value = false)
            } else {
                ChipJsonParsedResult.TextValue(value = current.content)
            }
        }
        is JsonObject, is JsonArray -> {
            ChipJsonParsedResult.TextValue(value = current.toString())
        }
    }
}

fun evaluateChipDisplay(
    format: ChipFormatType,
    prefix: String,
    jsonPath: String,
    rawValue: String?,
): ChipEvaluatedDisplay {
    val value = rawValue?.trim().orEmpty()
    if (value.isEmpty() || value == "...") {
        return ChipEvaluatedDisplay(text = "...")
    }

    return when (format) {
        ChipFormatType.FIXED_2 -> {
            val parsed = value.toDoubleOrNull()
            val text = if (parsed != null) formatDecimalWithCommas(parsed / 100.0, fixedDecimals = 2) else value
            ChipEvaluatedDisplay(prefix = prefix, text = text)
        }
        ChipFormatType.NUMBER -> {
            val parsed = value.toDoubleOrNull()
            val text = if (parsed != null) formatDecimalWithCommas(parsed, fixedDecimals = null) else value
            ChipEvaluatedDisplay(prefix = prefix, text = text)
        }
        ChipFormatType.TEXT -> {
            ChipEvaluatedDisplay(prefix = prefix, text = value)
        }
        ChipFormatType.JSON -> {
            when (val jsonResult = evaluateChipJson(rawString = value, jsonPath = jsonPath)) {
                is ChipJsonParsedResult.BooleanValue -> {
                    ChipEvaluatedDisplay(
                        prefix = prefix,
                        text = if (jsonResult.value) "true" else "false",
                        kind = if (jsonResult.value) ChipValueKind.BooleanTrue else ChipValueKind.BooleanFalse,
                    )
                }
                is ChipJsonParsedResult.NullValue -> {
                    ChipEvaluatedDisplay(
                        prefix = prefix,
                        text = "null",
                        kind = ChipValueKind.Null,
                    )
                }
                is ChipJsonParsedResult.UndefinedValue -> {
                    ChipEvaluatedDisplay(
                        prefix = prefix,
                        text = "undefined",
                        kind = ChipValueKind.Undefined,
                    )
                }
                is ChipJsonParsedResult.TextValue -> {
                    ChipEvaluatedDisplay(
                        prefix = prefix,
                        text = jsonResult.value,
                    )
                }
            }
        }
    }
}

fun formatDecimalWithCommas(
    number: Double,
    fixedDecimals: Int?,
): String {
    if (!number.isFinite()) return number.toString()
    val negative = number < 0.0
    val absVal = abs(number)
    val scaledCents = (absVal * 100.0).roundToLong()
    val wholePart = scaledCents / 100L
    val fracPart = (scaledCents % 100L).toInt()

    val wholeGrouped = wholePart
        .toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()

    val decimals = when (fixedDecimals) {
        2 -> ".${fracPart.toString().padStart(2, '0')}"
        else -> {
            if (fracPart == 0) {
                ""
            } else if (fracPart % 10 == 0) {
                ".${fracPart / 10}"
            } else {
                ".${fracPart.toString().padStart(2, '0')}"
            }
        }
    }

    return "${if (negative && (wholePart > 0L || fracPart > 0)) "-" else ""}$wholeGrouped$decimals"
}

fun legacyColorIntToHex(
    colorInt: Int?,
): String? {
    if (colorInt == null || colorInt == 0) return null
    val r = (colorInt shr 16) and 0xFF
    val g = (colorInt shr 8) and 0xFF
    val b = colorInt and 0xFF
    return ChipRgb(r = r, g = g, b = b).toHex()
}

fun parseChipHexColor(
    hex: String?,
): ChipRgb? {
    val clean = hex?.trim()?.removePrefix("#") ?: return null
    return when (clean.length) {
        6 -> {
            val v = clean.toIntOrNull(16) ?: return null
            ChipRgb(
                r = (v shr 16) and 0xFF,
                g = (v shr 8) and 0xFF,
                b = v and 0xFF,
            )
        }
        8 -> {
            val v = clean.toLongOrNull(16) ?: return null
            ChipRgb(
                r = ((v shr 16) and 0xFF).toInt(),
                g = ((v shr 8) and 0xFF).toInt(),
                b = (v and 0xFF).toInt(),
            )
        }
        else -> null
    }
}

fun resolveChipBaseHex(
    color: String?,
    legacyBackgroundColor: Int? = null,
    format: ChipFormatType = ChipFormatType.TEXT,
    kind: ChipValueKind = ChipValueKind.Normal,
): String {
    val normalized = parseChipHexColor(color)?.toHex()
        ?: legacyColorIntToHex(legacyBackgroundColor)
    val isDefaultGrey = normalized == null || normalized.equals(defaultChipHexColor, ignoreCase = true)

    if (format == ChipFormatType.JSON && isDefaultGrey) {
        if (kind == ChipValueKind.BooleanTrue) return "#BAFFC9"
        if (kind == ChipValueKind.BooleanFalse) return "#FFB3BA"
    }

    return normalized ?: defaultChipHexColor
}

fun computeChipGradientPalette(
    baseHex: String,
): ChipGradientPalette {
    val base = parseChipHexColor(baseHex) ?: parseChipHexColor(defaultChipHexColor)!!
    val (h, s, l) = rgbToHsl(base)

    val softWarm = if (s < 0.08f) {
        // Achromatic / grey custom color -> give a subtle pearlescent soft gradient like Signals
        hslToRgb(
            h = 225f,
            s = 0.14f,
            l = (l + 0.06f).coerceIn(0.12f, 0.96f),
        )
    } else {
        hslToRgb(
            h = (h + 20f) % 360f,
            s = (s * 1.05f).coerceIn(0.12f, 1f),
            l = (l + 0.07f).coerceIn(0.14f, 0.95f),
        )
    }

    val softCool = if (s < 0.08f) {
        hslToRgb(
            h = 285f,
            s = 0.12f,
            l = (l - 0.05f).coerceIn(0.08f, 0.92f),
        )
    } else {
        hslToRgb(
            h = (h + 340f) % 360f,
            s = (s * 1.02f).coerceIn(0.12f, 1f),
            l = (l - 0.05f).coerceIn(0.10f, 0.92f),
        )
    }

    val border1 = mixRgb(softCool, ChipRgb(255, 255, 255), 0.25f)
    val border2 = mixRgb(base, ChipRgb(0, 0, 0), 0.08f)
    val border3 = mixRgb(softWarm, ChipRgb(255, 255, 255), 0.30f)

    // Relative perceptual luminance to ensure crisp contrast for any custom color
    val luminance = (0.299 * base.r + 0.587 * base.g + 0.114 * base.b) / 255.0

    return ChipGradientPalette(
        stops = listOf(base, softWarm, softCool, base),
        borderStops = listOf(border1, border2, border3, border1),
        useLightForeground = luminance < 0.52,
    )
}

private fun mixRgb(
    a: ChipRgb,
    b: ChipRgb,
    t: Float,
): ChipRgb {
    val clamped = t.coerceIn(0f, 1f)
    return ChipRgb(
        r = (a.r + (b.r - a.r) * clamped).roundToInt().coerceIn(0, 255),
        g = (a.g + (b.g - a.g) * clamped).roundToInt().coerceIn(0, 255),
        b = (a.b + (b.b - a.b) * clamped).roundToInt().coerceIn(0, 255),
    )
}

private fun rgbToHsl(
    rgb: ChipRgb,
): Triple<Float, Float, Float> {
    val r = rgb.r / 255f
    val g = rgb.g / 255f
    val b = rgb.b / 255f

    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    val l = (maxC + minC) / 2f

    if (delta == 0f) {
        return Triple(0f, 0f, l)
    }

    val s = delta / (1f - abs(2f * l - 1f)).coerceAtLeast(0.0001f)
    val hRaw = when (maxC) {
        r -> ((g - b) / delta) % 6f
        g -> ((b - r) / delta) + 2f
        else -> ((r - g) / delta) + 4f
    }
    val h = ((hRaw * 60f) + 360f) % 360f
    return Triple(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

private fun hslToRgb(
    h: Float,
    s: Float,
    l: Float,
): ChipRgb {
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs(((h / 60f) % 2f) - 1f))
    val m = l - c / 2f

    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return ChipRgb(
        r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255),
        g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255),
        b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255),
    )
}

private fun Int.toHexByte(): String =
    toString(16).uppercase().padStart(2, '0')

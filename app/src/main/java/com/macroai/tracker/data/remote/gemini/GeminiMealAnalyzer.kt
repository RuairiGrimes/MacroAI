package com.macroai.tracker.data.remote.gemini

import android.graphics.Bitmap
import android.util.Base64
import com.macroai.tracker.BuildConfig
import com.macroai.tracker.data.local.datastore.UserPreferencesDataStore
import com.macroai.tracker.data.remote.gemini.dto.GeminiContentDto
import com.macroai.tracker.data.remote.gemini.dto.GeminiDayOverviewDto
import com.macroai.tracker.data.remote.gemini.dto.GeminiDayOverviewResponseSchema
import com.macroai.tracker.data.remote.gemini.dto.GeminiGenerateContentRequest
import com.macroai.tracker.data.remote.gemini.dto.GeminiGenerateContentResponse
import com.macroai.tracker.data.remote.gemini.dto.GeminiGenerationConfigDto
import com.macroai.tracker.data.remote.gemini.dto.GeminiInlineDataDto
import com.macroai.tracker.data.remote.gemini.dto.GeminiMealAnalysisDto
import com.macroai.tracker.data.remote.gemini.dto.GeminiMealResponseSchema
import com.macroai.tracker.data.remote.gemini.dto.GeminiPartDto
import com.macroai.tracker.data.remote.gemini.dto.GeminiThinkingConfigDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class GeminiMealAnalyzer @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val userPreferencesDataStore: UserPreferencesDataStore? = null
) {
    companion object {
        const val MODEL_NAME = "gemini-3.1-flash-lite"
        const val MODEL_3_5_LITE = "gemini-3.5-flash-lite"

        fun endpointForModel(model: String): String =
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

        const val SYSTEM_INSTRUCTION =
            "You are a clinical nutritionist and food scientist. Analyze the meal image AND incorporate all details from the user's text prompt (cooking oils, weights, hidden ingredients). Estimate total weight, macronutrients, polyunsaturated fats, fibre, key micronutrients, NOVA processing group (1-4), and whether seed oils are likely present based on visual cues or prompt context. Consolidate minor items so ingredientBreakdown has at most 5 main components and keep aiConfidenceReasoning under 15 words. Return strictly valid JSON matching the schema."

        const val DAY_OVERVIEW_SYSTEM_INSTRUCTION =
            "You are an elite clinical nutritionist and metabolic health coach. Evaluate the user's full day of logged meals, macronutrient targets (especially protein adequacy), calorie balance/deficit, NOVA 4 ultra-processed food percentage, industrial seed oil exposure, fatty acid ratios, and micronutrients. Score the day objectively from 0 to 100 across overall nutrition, health/food quality, protein & macros, and calorie control. Provide specific, encouraging, and scientifically grounded feedback referencing their actual logged foods. Return strictly valid JSON matching the schema."

        // 640px fits comfortably inside a single Gemini vision tile (258 tokens) with ~35KB upload payload
        const val MAX_IMAGE_EDGE_PX = 640
        const val JPEG_QUALITY = 68
    }

    suspend fun getEffectiveApiKey(apiKeyOverride: String? = null): String {
        val override = apiKeyOverride?.trim()
        if (!override.isNullOrEmpty()) return override

        val userCustomKey = runCatching {
            userPreferencesDataStore?.userProfileFlow?.first()?.customGeminiApiKey?.trim()
        }.getOrNull()
        if (!userCustomKey.isNullOrEmpty()) return userCustomKey

        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildKey.isNotEmpty()) return buildKey

        return ""
    }

    suspend fun testApiKey(apiKey: String, modelId: String = MODEL_NAME): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKey.trim()
            require(key.isNotEmpty()) { "API key cannot be empty" }
            val targetModel = modelId.ifBlank { MODEL_NAME }
            val pingJson = """{"contents":[{"parts":[{"text":"ping"}]}]}"""
            val response = httpClient.post("${endpointForModel(targetModel)}?key=$key") {
                contentType(ContentType.Application.Json)
                setBody(pingJson)
            }
            if (response.status.isSuccess()) {
                true
            } else {
                val status = response.status.value
                val errorMsg = runCatching { response.bodyAsText() }.getOrDefault("")
                throw IllegalStateException("Gemini API error (HTTP $status): ${errorMsg.take(160)}")
            }
        }
    }

    suspend fun analyzeMealPhoto(
        bitmap: Bitmap,
        userContextPrompt: String?,
        modelId: String = MODEL_NAME
    ): Result<GeminiMealAnalysisDto> = withContext(Dispatchers.IO) {
        val base64Jpeg = compressAndEncodeBitmap(bitmap)
        analyzeEncodedPhoto(base64Jpeg, userContextPrompt, modelId = modelId)
    }

    suspend fun analyzeEncodedPhoto(
        base64Jpeg: String,
        userContextPrompt: String?,
        apiKeyOverride: String? = null,
        modelId: String = MODEL_NAME
    ): Result<GeminiMealAnalysisDto> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = getEffectiveApiKey(apiKeyOverride)
            require(apiKey.isNotEmpty()) {
                "Gemini API key is not configured. Add your API key in Settings -> Cloud & AI Integration."
            }

            val promptText = if (userContextPrompt.isNullOrBlank()) {
                "Analyze this meal image and return the structured JSON breakdown."
            } else {
                "Context: $userContextPrompt\nAnalyze this meal image with context and return the structured JSON breakdown."
            }

            val baseContents = listOf(
                GeminiContentDto(
                    role = "user",
                    parts = listOf(
                        GeminiPartDto(
                            inlineData = GeminiInlineDataDto(
                                mimeType = "image/jpeg",
                                data = base64Jpeg
                            )
                        ),
                        GeminiPartDto(text = promptText)
                    )
                )
            )

            val fastRequest = GeminiGenerateContentRequest(
                systemInstruction = GeminiContentDto(
                    parts = listOf(GeminiPartDto(text = SYSTEM_INSTRUCTION))
                ),
                contents = baseContents,
                generationConfig = GeminiGenerationConfigDto(
                    responseMimeType = "application/json",
                    responseSchema = GeminiMealResponseSchema.jsonSchema,
                    temperature = 0.1,
                    maxOutputTokens = 4096,
                    thinkingConfig = GeminiThinkingConfigDto(thinkingLevel = "low")
                )
            )

            executeGeminiRequest(apiKey, fastRequest, modelId)
        }
    }

    suspend fun analyzeTextPrompt(
        recipePrompt: String,
        apiKeyOverride: String? = null,
        modelId: String = MODEL_NAME
    ): Result<GeminiMealAnalysisDto> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = getEffectiveApiKey(apiKeyOverride)
            require(apiKey.isNotEmpty()) {
                "Gemini API key is not configured. Add your API key in Settings -> Cloud & AI Integration."
            }

            val promptText = "Analyze the following recipe/meal description. Breakdown its ingredients with estimated weights in grams, total calories, macros (protein, carbs, fat), micronutrients, NOVA processing group, and presence of seed oils:\n\n$recipePrompt"

            val baseContents = listOf(
                GeminiContentDto(
                    role = "user",
                    parts = listOf(GeminiPartDto(text = promptText))
                )
            )

            val fastRequest = GeminiGenerateContentRequest(
                systemInstruction = GeminiContentDto(
                    parts = listOf(GeminiPartDto(text = SYSTEM_INSTRUCTION))
                ),
                contents = baseContents,
                generationConfig = GeminiGenerationConfigDto(
                    responseMimeType = "application/json",
                    responseSchema = GeminiMealResponseSchema.jsonSchema,
                    temperature = 0.1,
                    maxOutputTokens = 4096,
                    thinkingConfig = GeminiThinkingConfigDto(thinkingLevel = "low")
                )
            )

            executeGeminiRequest(apiKey, fastRequest, modelId)
        }
    }

    suspend fun analyzeDailyNutrition(
        dayPrompt: String,
        apiKeyOverride: String? = null,
        modelId: String = MODEL_NAME
    ): Result<GeminiDayOverviewDto> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = getEffectiveApiKey(apiKeyOverride)
            require(apiKey.isNotEmpty()) {
                "Gemini API key is not configured. Add your API key in Settings -> Cloud & AI Integration."
            }

            val request = GeminiGenerateContentRequest(
                systemInstruction = GeminiContentDto(
                    parts = listOf(GeminiPartDto(text = DAY_OVERVIEW_SYSTEM_INSTRUCTION))
                ),
                contents = listOf(
                    GeminiContentDto(
                        role = "user",
                        parts = listOf(GeminiPartDto(text = dayPrompt))
                    )
                ),
                generationConfig = GeminiGenerationConfigDto(
                    responseMimeType = "application/json",
                    responseSchema = GeminiDayOverviewResponseSchema.jsonSchema,
                    temperature = 0.2,
                    maxOutputTokens = 2048,
                    thinkingConfig = GeminiThinkingConfigDto(thinkingLevel = "low")
                )
            )

            val rawJsonText = executeGeminiRawJsonRequest(apiKey, request, modelId)
            parseDayOverviewJson(rawJsonText)
        }
    }

    private suspend fun executeGeminiRequest(
        apiKey: String,
        fastRequest: GeminiGenerateContentRequest,
        modelId: String = MODEL_NAME
    ): GeminiMealAnalysisDto {
        val rawJsonText = executeGeminiRawJsonRequest(apiKey, fastRequest, modelId)
        return parseMealAnalysisJson(rawJsonText)
    }

    private suspend fun executeGeminiRawJsonRequest(
        apiKey: String,
        fastRequest: GeminiGenerateContentRequest,
        modelId: String = MODEL_NAME
    ): String {
        val targetModel = modelId.ifBlank { MODEL_NAME }

        // Primary Attempt: Selected Gemini Flash-Lite Model
        val primaryAttempt = runCatching {
            httpClient.post("${endpointForModel(targetModel)}?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(fastRequest)
            }
        }

        val primaryResponse = primaryAttempt.getOrNull()
        val finalResponse = if (primaryResponse != null && primaryResponse.status.isSuccess()) {
            primaryResponse
        } else if (primaryResponse != null && (primaryResponse.status.value == 429 || primaryResponse.status.value == 403)) {
            // Do not retry on rate limits or authorization errors
            primaryResponse
        } else {
            // Secondary Attempt: Retry same selected model with thinkingConfig omitted if primary encountered parameter error
            val noThinkingRequest = fastRequest.copy(
                generationConfig = fastRequest.generationConfig.copy(thinkingConfig = null)
            )
            val retryAttempt = runCatching {
                httpClient.post("${endpointForModel(targetModel)}?key=$apiKey") {
                    contentType(ContentType.Application.Json)
                    setBody(noThinkingRequest)
                }
            }
            val retryResponse = retryAttempt.getOrNull()
            if (retryResponse != null && retryResponse.status.isSuccess()) {
                retryResponse
            } else {
                primaryResponse ?: retryResponse
                ?: throw (primaryAttempt.exceptionOrNull()
                    ?: retryAttempt.exceptionOrNull()
                    ?: IllegalStateException("Network call to Gemini API ($targetModel) failed. Please check your connection."))
            }
        }

        if (!finalResponse.status.isSuccess()) {
            val errorBody = runCatching { finalResponse.body<String>() }.getOrDefault("")
            throw IllegalStateException("Gemini API HTTP ${finalResponse.status.value}: $errorBody")
        }

        val geminiResponse = finalResponse.body<GeminiGenerateContentResponse>()
        return geminiResponse.candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull { !it.text.isNullOrBlank() }
            ?.text
            ?: throw IllegalStateException("Gemini returned an empty response candidate.")
    }

    fun parseMealAnalysisJson(rawJsonText: String): GeminiMealAnalysisDto {
        val cleaned = rawJsonText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return try {
            json.decodeFromString<GeminiMealAnalysisDto>(cleaned)
        } catch (firstError: Exception) {
            val repaired = repairTruncatedMealJson(cleaned)
            if (repaired != null) {
                json.decodeFromString<GeminiMealAnalysisDto>(repaired)
            } else {
                throw firstError
            }
        }
    }

    fun parseDayOverviewJson(rawJsonText: String): GeminiDayOverviewDto {
        val cleaned = rawJsonText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val parsed = json.decodeFromString<GeminiDayOverviewDto>(cleaned)
        return parsed.copy(
            overallScore = parsed.overallScore.coerceIn(0, 100),
            healthScore = parsed.healthScore.coerceIn(0, 100),
            proteinAndMacroScore = parsed.proteinAndMacroScore.coerceIn(0, 100),
            calorieControlScore = parsed.calorieControlScore.coerceIn(0, 100)
        )
    }

    /**
     * Salvages a JSON payload if token limits or network framing truncated the stream inside
     * the trailing `ingredientBreakdown` array.
     */
    internal fun repairTruncatedMealJson(truncatedJson: String): String? {
        val breakdownKeyIndex = truncatedJson.indexOf("\"ingredientBreakdown\"")
        if (breakdownKeyIndex == -1) return null

        val arrayStartIndex = truncatedJson.indexOf('[', startIndex = breakdownKeyIndex)
        if (arrayStartIndex == -1) {
            // Cut off right after "ingredientBreakdown" key
            val beforeKey = truncatedJson.substring(0, breakdownKeyIndex).trimEnd().removeSuffix(",")
            return "$beforeKey, \"ingredientBreakdown\": []}"
        }

        // Find the last complete ingredient object '}' inside the ingredientBreakdown array
        val lastCompleteItemEnd = truncatedJson.lastIndexOf('}')
        return if (lastCompleteItemEnd > arrayStartIndex) {
            val salvagedPrefix = truncatedJson.substring(0, lastCompleteItemEnd + 1)
            "$salvagedPrefix]}"
        } else {
            // No complete ingredient object finished before EOF; close with empty array
            val prefixToBracket = truncatedJson.substring(0, arrayStartIndex + 1)
            "$prefixToBracket]}"
        }
    }

    fun compressAndEncodeBitmap(source: Bitmap): String {
        val width = source.width
        val height = source.height
        val longestEdge = max(width, height)

        val scaledBitmap = if (longestEdge > MAX_IMAGE_EDGE_PX) {
            val scale = MAX_IMAGE_EDGE_PX.toFloat() / longestEdge.toFloat()
            val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
            val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
        } else {
            source
        }

        val outputStream = ByteArrayOutputStream(65536)
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        val jpegBytes = outputStream.toByteArray()
        return Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
    }
}

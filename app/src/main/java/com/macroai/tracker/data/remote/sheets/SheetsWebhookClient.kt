package com.macroai.tracker.data.remote.sheets

import com.macroai.tracker.BuildConfig
import com.macroai.tracker.data.local.datastore.UserPreferencesDataStore
import com.macroai.tracker.data.local.entity.FoodLogEntity
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SheetsSyncPayloadDto(
    val entries: List<SheetsRowDto>
)

@Serializable
data class SheetsRowDto(
    val id: String,
    val timestamp: Long,
    val isoDateTime: String,
    val date: String,
    val mealType: String,
    val name: String,
    val source: String,
    val servingWeightGrams: Double,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val saturatedFatG: Double,
    val polyunsaturatedFatG: Double,
    val monounsaturatedFatG: Double,
    val sugarG: Double,
    val fibreG: Double,
    val novaGroup: Int?,
    val isUpf: Boolean,
    val containsSeedOils: Boolean,
    val detectedSeedOils: String,
    val aiConfidenceReasoning: String?,
    val sodiumMg: Double,
    val potassiumMg: Double,
    val ironMg: Double,
    val calciumMg: Double,
    val vitaminCMg: Double,
    val vitaminDUg: Double,
    val magnesiumMg: Double
)

fun FoodLogEntity.toSheetsRowDto(): SheetsRowDto {
    val zone = ZoneId.systemDefault()
    val zonedDateTime = Instant.ofEpochMilli(timestamp).atZone(zone)
    return SheetsRowDto(
        id = id,
        timestamp = timestamp,
        isoDateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(zonedDateTime),
        date = DateTimeFormatter.ISO_LOCAL_DATE.format(zonedDateTime),
        mealType = mealType.name,
        name = name,
        source = source.name,
        servingWeightGrams = servingWeightGrams,
        calories = calories,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        saturatedFatG = saturatedFatG,
        polyunsaturatedFatG = polyunsaturatedFatG,
        monounsaturatedFatG = monounsaturatedFatG,
        sugarG = sugarG,
        fibreG = fibreG,
        novaGroup = novaGroup,
        isUpf = isUpf,
        containsSeedOils = containsSeedOils,
        detectedSeedOils = detectedSeedOils,
        aiConfidenceReasoning = aiConfidenceReasoning,
        sodiumMg = sodiumMg,
        potassiumMg = potassiumMg,
        ironMg = ironMg,
        calciumMg = calciumMg,
        vitaminCMg = vitaminCMg,
        vitaminDUg = vitaminDUg,
        magnesiumMg = magnesiumMg
    )
}

@Singleton
class SheetsWebhookClient @Inject constructor(
    private val httpClient: HttpClient,
    private val userPreferencesDataStore: UserPreferencesDataStore? = null
) {
    suspend fun getEffectiveWebhookUrl(override: String? = null): String {
        val overrideTrimmed = override?.trim()
        if (!overrideTrimmed.isNullOrEmpty()) return overrideTrimmed

        val userCustom = runCatching {
            userPreferencesDataStore?.userProfileFlow?.first()?.customSheetsWebhookUrl?.trim()
        }.getOrNull()
        if (!userCustom.isNullOrEmpty()) return userCustom

        val buildUrl = BuildConfig.SHEETS_WEBHOOK_URL.trim()
        if (buildUrl.isNotEmpty()) return buildUrl

        return ""
    }

    suspend fun testWebhookUrl(webhookUrl: String): Result<Boolean> = runCatching {
        val url = webhookUrl.trim()
        require(url.isNotEmpty()) { "Webhook URL cannot be empty" }
        require(url.startsWith("http://") || url.startsWith("https://")) {
            "URL must begin with https:// or http://"
        }

        val testPayload = """{"action":"ping","test":true}"""
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(testPayload)
        }

        if (response.status == HttpStatusCode.OK || response.status.isSuccess()) {
            true
        } else {
            val status = response.status.value
            val text = runCatching { response.bodyAsText() }.getOrDefault("")
            throw IllegalStateException("Webhook returned HTTP $status: ${text.take(160)}")
        }
    }

    suspend fun postUnsyncedLogs(
        logs: List<FoodLogEntity>,
        webhookUrlOverride: String? = null
    ): Result<Boolean> = runCatching {
        if (logs.isEmpty()) return@runCatching true

        val webhookUrl = getEffectiveWebhookUrl(webhookUrlOverride)
        require(webhookUrl.isNotEmpty()) {
            "Google Sheets Webhook URL is not configured. Add your webhook URL in Settings -> Cloud & AI Integration."
        }

        val payload = SheetsSyncPayloadDto(
            entries = logs.map { it.toSheetsRowDto() }
        )

        val response = httpClient.post(webhookUrl) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        response.status == HttpStatusCode.OK || response.status.isSuccess()
    }
}


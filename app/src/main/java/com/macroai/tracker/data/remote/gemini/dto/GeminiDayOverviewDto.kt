package com.macroai.tracker.data.remote.gemini.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Structured AI evaluation returned by Gemini when the user presses "End Day".
 * Scores the day across overall nutrition, food quality/health (UPF & seed oils),
 * protein/macro adherence, and calorie control.
 */
@Serializable
data class GeminiDayOverviewDto(
    val overallScore: Int = 80,
    val scoreGrade: String = "B+",
    val headline: String = "",
    val healthScore: Int = 80,
    val proteinAndMacroScore: Int = 80,
    val calorieControlScore: Int = 80,
    val executiveSummary: String = "",
    val keyWins: List<String> = emptyList(),
    val actionableImprovements: List<String> = emptyList()
)

object GeminiDayOverviewResponseSchema {
    val jsonSchema: JsonObject = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("overallScore") {
                put("type", "INTEGER")
                put("description", "Overall daily nutrition score from 0 to 100")
            }
            putJsonObject("scoreGrade") {
                put("type", "STRING")
                put("description", "Letter grade such as A+, A, A-, B+, B, C+, C, or D")
            }
            putJsonObject("headline") {
                put("type", "STRING")
                put("description", "Punchy 1-sentence clinical summary of today's nutrition")
            }
            putJsonObject("healthScore") {
                put("type", "INTEGER")
                put("description", "Food quality & health score (0-100) based on whole foods vs NOVA 4 UPF %, absence of seed oils, fibre, and micronutrients")
            }
            putJsonObject("proteinAndMacroScore") {
                put("type", "INTEGER")
                put("description", "Protein and macronutrient balance score (0-100) based on hitting protein and macro targets")
            }
            putJsonObject("calorieControlScore") {
                put("type", "INTEGER")
                put("description", "Calorie target and deficit adherence score (0-100)")
            }
            putJsonObject("executiveSummary") {
                put("type", "STRING")
                put("description", "2 to 3 sentences of personalized clinical nutritionist feedback referencing the specific meals eaten today, protein/macro distribution, UPF/seed oils, and calorie balance")
            }
            putJsonObject("keyWins") {
                put("type", "ARRAY")
                put("description", "2 to 4 concise bullet points highlighting the best nutritional wins achieved today")
                putJsonObject("items") {
                    put("type", "STRING")
                }
            }
            putJsonObject("actionableImprovements") {
                put("type", "ARRAY")
                put("description", "1 to 3 concrete, actionable recommendations to improve nutrition tomorrow")
                putJsonObject("items") {
                    put("type", "STRING")
                }
            }
        }
        putJsonArray("required") {
            listOf(
                "overallScore",
                "scoreGrade",
                "headline",
                "healthScore",
                "proteinAndMacroScore",
                "calorieControlScore",
                "executiveSummary",
                "keyWins",
                "actionableImprovements"
            ).forEach { add(JsonPrimitive(it)) }
        }
    }
}


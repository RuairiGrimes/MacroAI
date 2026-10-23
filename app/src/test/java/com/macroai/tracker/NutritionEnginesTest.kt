package com.macroai.tracker

import com.macroai.tracker.data.remote.gemini.GeminiMealAnalyzer
import com.macroai.tracker.data.remote.off.OpenFoodFactsApi
import com.macroai.tracker.domain.model.LogSource
import com.macroai.tracker.domain.model.MealType
import com.macroai.tracker.domain.util.SeedOilDetector
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionEnginesTest {

    private val testJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun `SeedOilDetector detects all target seed oils case-insensitively`() {
        val ingredients = """
            Potatoes, Vegetable Oil (Rapeseed Oil, Sunflower, CANOLA, Soybean Oil,
            Cottonseed, Corn Oil, Safflower, Grapeseed Oil), Sea Salt, Dextrose.
        """.trimIndent()

        val result = SeedOilDetector.analyzeIngredients(ingredients)

        assertTrue(result.containsSeedOils)
        assertEquals(
            listOf(
                "rapeseed",
                "canola",
                "sunflower",
                "soybean",
                "cottonseed",
                "corn oil",
                "safflower",
                "grapeseed",
                "vegetable oil"
            ),
            result.detectedOils
        )
    }

    @Test
    fun `SeedOilDetector does not false-positive on clean fats or sweetcorn`() {
        val cleanIngredients =
            "Organic Sweetcorn, Extra Virgin Olive Oil, Grass-Fed Butter, Coconut Oil, Beef Tallow, Sea Salt"

        val result = SeedOilDetector.analyzeIngredients(cleanIngredients)

        assertFalse(result.containsSeedOils)
        assertTrue(result.detectedOils.isEmpty())
        assertEquals("", result.detectedOilsCsv)
    }

    @Test
    fun `SeedOilDetector differentiates 2g trace sunflower oil (GREEN) from 50g deep fried oil (RED) and moderate cooking oil (AMBER)`() {
        // 1. Trace / small amount: 2g sunflower oil in 100g salad or bread
        val traceCheck = SeedOilDetector.analyzeIngredients(
            ingredientsText = "Wheat flour, water, yeast, sunflower oil (2%), salt",
            servingWeightGrams = 100.0,
            totalFatG = 2.5,
            pufaG = 1.0,
            foodName = "Artisan Wholegrain Sourdough"
        )
        assertTrue(traceCheck.containsSeedOils)
        assertEquals(com.macroai.tracker.domain.util.SeedOilRiskLevel.GREEN, traceCheck.riskLevel)
        assertTrue(traceCheck.estimatedGrams <= 2.5)

        // Also test pure 2g sunflower oil item in ingredient breakdown:
        val cleanChicken = com.macroai.tracker.domain.model.MealIngredient(
            name = "Chicken Breast",
            weightGrams = 150.0,
            calories = 160.0,
            proteinG = 35.0,
            carbsG = 0.0,
            fatG = 2.0
        )
        val traceOil = com.macroai.tracker.domain.model.MealIngredient(
            name = "Sunflower Oil",
            weightGrams = 2.0,
            calories = 18.0,
            proteinG = 0.0,
            carbsG = 0.0,
            fatG = 2.0,
            containsSeedOils = true,
            detectedSeedOils = "sunflower"
        )
        val traceMealCheck = SeedOilDetector.assessIngredients(
            ingredients = listOf(cleanChicken, traceOil),
            totalMealWeight = 152.0,
            totalMealFat = 4.0,
            mealName = "Chicken with light sunflower oil"
        )
        assertTrue(traceMealCheck.containsSeedOils)
        assertEquals(com.macroai.tracker.domain.util.SeedOilRiskLevel.GREEN, traceMealCheck.riskLevel)
        assertEquals(2.0, traceMealCheck.estimatedGrams, 0.1)

        // 2. Heavy / oxidised deep fried oil: 50g deep frying oil in takeaway chips
        val heavyFryingOil = com.macroai.tracker.domain.model.MealIngredient(
            name = "Vegetable Oil (Deep Frying)",
            weightGrams = 50.0,
            calories = 450.0,
            proteinG = 0.0,
            carbsG = 0.0,
            fatG = 50.0,
            containsSeedOils = true,
            detectedSeedOils = "vegetable oil"
        )
        val deepFriedChips = SeedOilDetector.assessIngredients(
            ingredients = listOf(heavyFryingOil),
            totalMealWeight = 250.0,
            totalMealFat = 50.0,
            mealName = "Deep Fried Chippy Chips"
        )
        assertTrue(deepFriedChips.containsSeedOils)
        assertEquals(com.macroai.tracker.domain.util.SeedOilRiskLevel.RED, deepFriedChips.riskLevel)
        assertEquals(50.0, deepFriedChips.estimatedGrams, 0.1)

        // Single product deep-fried scan
        val deepFriedBarcode = SeedOilDetector.analyzeIngredients(
            ingredientsText = "Potatoes, Rapeseed Oil, Salt",
            servingWeightGrams = 200.0,
            totalFatG = 35.0,
            pufaG = 12.0,
            foodName = "Deep-Fried French Fries"
        )
        assertTrue(deepFriedBarcode.containsSeedOils)
        assertEquals(com.macroai.tracker.domain.util.SeedOilRiskLevel.RED, deepFriedBarcode.riskLevel)
        assertTrue(deepFriedBarcode.estimatedGrams > 10.0)

        // 3. Moderate cooking oil: 6g cooking oil in a home stir fry
        val moderateOil = com.macroai.tracker.domain.model.MealIngredient(
            name = "Canola Oil",
            weightGrams = 6.0,
            calories = 54.0,
            proteinG = 0.0,
            carbsG = 0.0,
            fatG = 6.0,
            containsSeedOils = true,
            detectedSeedOils = "canola"
        )
        val moderateStirFry = SeedOilDetector.assessIngredients(
            ingredients = listOf(cleanChicken, moderateOil),
            totalMealWeight = 250.0,
            totalMealFat = 8.0,
            mealName = "Chicken Stir Fry"
        )
        assertTrue(moderateStirFry.containsSeedOils)
        assertEquals(com.macroai.tracker.domain.util.SeedOilRiskLevel.AMBER, moderateStirFry.riskLevel)
        assertEquals(6.0, moderateStirFry.estimatedGrams, 0.1)
    }

    @Test
    fun `OpenFoodFactsApi sends required User-Agent and scales 100g values proportionally`() = runTest {
        var capturedUserAgent: String? = null

        val mockOffJson = """
            {
              "code": "5000159459228",
              "status": 1,
              "product": {
                "product_name": "Salted Tortilla Chips",
                "brands": "CrunchCo",
                "nova_group": 4,
                "ingredients_text": "Corn flour, Sunflower Oil, Rapeseed oil, Salt",
                "nutriments": {
                  "energy-kcal_100g": 490.0,
                  "proteins_100g": 6.4,
                  "carbohydrates_100g": 62.0,
                  "fat_100g": 24.0,
                  "saturated-fat_100g": 2.2,
                  "polyunsaturated-fat_100g": 9.8,
                  "monounsaturated-fat_100g": 11.0,
                  "sugars_100g": 0.8,
                  "fiber_100g": 4.2,
                  "sodium_100g": 0.32,
                  "sodium_unit": "g",
                  "potassium_100g": 220.0,
                  "potassium_unit": "mg",
                  "magnesium_100g": 80.0,
                  "magnesium_unit": "mg"
                }
              }
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            capturedUserAgent = request.headers[HttpHeaders.UserAgent]
            respond(
                content = mockOffJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(testJson) }
        }

        val api = OpenFoodFactsApi(client)
        val productResult = api.getProductByBarcode("5000159459228")

        assertTrue(productResult.isSuccess)
        assertEquals(OpenFoodFactsApi.USER_AGENT, capturedUserAgent)

        val normalized = productResult.getOrThrow()
        assertEquals("CrunchCo — Salted Tortilla Chips", normalized.name)
        assertEquals(4, normalized.novaGroup)
        assertTrue(normalized.isUpf)
        assertTrue(normalized.containsSeedOils)
        assertEquals("rapeseed, sunflower", normalized.detectedSeedOilsCsv)

        // Scale to 50g consumed weight
        val entity50g = normalized.scaleToEntity(
            servingWeightGrams = 50.0,
            mealType = MealType.SNACK
        )

        assertEquals(LogSource.BARCODE, entity50g.source)
        assertEquals(245.0, entity50g.calories, 0.01)
        assertEquals(3.2, entity50g.proteinG, 0.01)
        assertEquals(31.0, entity50g.carbsG, 0.01)
        assertEquals(12.0, entity50g.fatG, 0.01)
        assertEquals(4.9, entity50g.polyunsaturatedFatG, 0.01)
        assertEquals(160.0, entity50g.sodiumMg, 0.01) // 0.32g = 320mg per 100g -> 160mg per 50g
        assertEquals(110.0, entity50g.potassiumMg, 0.01)
        assertEquals(40.0, entity50g.magnesiumMg, 0.01)
        assertTrue(entity50g.isUpf)
        assertTrue(entity50g.containsSeedOils)
    }

    @Test
    fun `GeminiMealAnalyzer parses structured JSON and updates ingredient breakdown totals`() {
        val client = HttpClient(MockEngine { respond("") })
        val analyzer = GeminiMealAnalyzer(client, testJson)

        val geminiStructuredJson = """
            {
              "name": "Pan-Seared Chicken Breast with Rice",
              "servingWeightGrams": 350.0,
              "calories": 540.0,
              "proteinG": 51.0,
              "carbsG": 46.0,
              "fatG": 16.0,
              "saturatedFatG": 11.5,
              "polyunsaturatedFatG": 1.4,
              "monounsaturatedFatG": 2.6,
              "sugarG": 0.5,
              "fibreG": 1.8,
              "novaGroup": 1,
              "isUpf": false,
              "containsSeedOils": false,
              "detectedSeedOils": "",
              "aiConfidenceReasoning": "Estimated 150g cooked chicken breast, 185g steamed jasmine rice, and 1 tbsp (14g) coconut oil from user prompt.",
              "sodiumMg": 310.0,
              "potassiumMg": 620.0,
              "ironMg": 1.9,
              "calciumMg": 28.0,
              "vitaminCMg": 2.0,
              "vitaminDUg": 0.4,
              "magnesiumMg": 68.0,
              "ingredientBreakdown": [
                {
                  "name": "Chicken Breast (Cooked)",
                  "estimatedWeightGrams": 150.0,
                  "calories": 248.0,
                  "proteinG": 46.5,
                  "carbsG": 0.0,
                  "fatG": 5.4,
                  "novaGroup": 1,
                  "containsSeedOils": false
                },
                {
                  "name": "Steamed Jasmine Rice",
                  "estimatedWeightGrams": 186.0,
                  "calories": 242.0,
                  "proteinG": 4.5,
                  "carbsG": 46.0,
                  "fatG": 0.4,
                  "novaGroup": 1,
                  "containsSeedOils": false
                },
                {
                  "name": "Coconut Oil (1 tbsp)",
                  "estimatedWeightGrams": 14.0,
                  "calories": 120.0,
                  "proteinG": 0.0,
                  "carbsG": 0.0,
                  "fatG": 14.0,
                  "novaGroup": 2,
                  "containsSeedOils": false
                }
              ]
            }
        """.trimIndent()

        val parsed = analyzer.parseMealAnalysisJson(geminiStructuredJson)
        assertEquals(3, parsed.ingredientBreakdown.size)
        assertEquals("Pan-Seared Chicken Breast with Rice", parsed.name)
        assertFalse(parsed.containsSeedOils)

        // Simulate user doubling the chicken weight in the confirmation review UI (150g -> 300g)
        val tweakedIngredients = parsed.ingredientBreakdown.mapIndexed { index, item ->
            if (index == 0) item.scaleToWeight(300.0) else item
        }
        val recalculated = parsed.withUpdatedIngredients(tweakedIngredients)

        assertEquals(500.0, recalculated.servingWeightGrams, 0.1)
        assertEquals(496.0 + 242.0 + 120.0, recalculated.calories, 0.5)
        assertEquals(93.0 + 4.5, recalculated.proteinG, 0.2)

        // Also verify prompt seed oil override if user prompt mentioned soybean oil
        val entityWithSeedOilContext = recalculated.toFoodLogEntity(
            mealType = MealType.DINNER,
            userContextPrompt = "marinated in 1 tsp soybean oil"
        )
        assertTrue(entityWithSeedOilContext.containsSeedOils)
        assertEquals("soybean", entityWithSeedOilContext.detectedSeedOils)
    }

    @Test
    fun `TdeeCalculator computes Mifflin-St Jeor BMR and respects mode precedence`() {
        val baseProfile = com.macroai.tracker.domain.model.UserProfile(
            weightKg = 80.0,
            heightCm = 180.0,
            age = 30,
            biologicalSex = com.macroai.tracker.domain.model.BiologicalSex.MALE,
            activityLevel = com.macroai.tracker.domain.model.ActivityLevel.MODERATE,
            tdeeMode = com.macroai.tracker.domain.model.TdeeMode.HEALTH_CONNECT_DYNAMIC,
            manualOverrideKcal = 2850
        )

        // BMR = (10 * 80) + (6.25 * 180) - (5 * 30) + 5 = 800 + 1125 - 150 + 5 = 1780 kcal
        val dynamicResult = com.macroai.tracker.domain.util.TdeeCalculator.calculateTdee(
            profile = baseProfile,
            healthConnectWeightKg = 80.0,
            healthConnectHeightCm = 180.0,
            healthConnectActiveCaloriesKcal = 520.0
        )
        assertEquals(1780, dynamicResult.bmrKcal)
        assertEquals(2300, dynamicResult.tdeeKcal)

        // Formula multiplier mode: 1780 * 1.55 = 2759 kcal
        val multiplierResult = com.macroai.tracker.domain.util.TdeeCalculator.calculateTdee(
            profile = baseProfile.copy(tdeeMode = com.macroai.tracker.domain.model.TdeeMode.FORMULA_MULTIPLIER)
        )
        assertEquals(2759, multiplierResult.tdeeKcal)

        // Manual override mode when energyTargetMode is TDEE_DEFICIT overrides TDEE burn
        val manualResult = com.macroai.tracker.domain.util.TdeeCalculator.calculateTdee(
            profile = baseProfile.copy(
                tdeeMode = com.macroai.tracker.domain.model.TdeeMode.MANUAL_OVERRIDE,
                energyTargetMode = com.macroai.tracker.domain.model.EnergyTargetMode.TDEE_DEFICIT,
                targetDeficitKcal = 0,
                manualOverrideKcal = 2850,
                macroInputMode = com.macroai.tracker.domain.model.MacroInputMode.CUSTOM_GRAMS,
                customProteinGrams = 190,
                customCarbsGrams = 160,
                customFatGrams = 65
            ),
            healthConnectActiveCaloriesKcal = 900.0
        )
        assertEquals(2850, manualResult.tdeeKcal)
        assertEquals(2850, manualResult.energyTargetKcal)
        assertEquals(190, manualResult.proteinTargetGrams)
        assertEquals(160, manualResult.carbsTargetGrams)
        assertEquals(65, manualResult.fatTargetGrams)
    }

    @Test
    fun `UpfMetricsCalculator computes NOVA 4 calorie percentage, weight, and seed oil counts`() {
        val wholeFood = com.macroai.tracker.data.local.entity.FoodLogEntity(
            mealType = MealType.LUNCH,
            name = "Ribeye Steak & Eggs",
            source = LogSource.MANUAL,
            servingWeightGrams = 300.0,
            calories = 750.0,
            proteinG = 65.0,
            carbsG = 2.0,
            fatG = 53.0,
            novaGroup = 1,
            isUpf = false,
            containsSeedOils = false
        )
        val upfSnack = com.macroai.tracker.data.local.entity.FoodLogEntity(
            mealType = MealType.SNACK,
            name = "Packaged Protein Bar",
            source = LogSource.BARCODE,
            servingWeightGrams = 100.0,
            calories = 250.0,
            proteinG = 20.0,
            carbsG = 25.0,
            fatG = 8.0,
            novaGroup = 4,
            isUpf = true,
            containsSeedOils = true,
            detectedSeedOils = "sunflower, canola"
        )

        val summary = com.macroai.tracker.domain.util.UpfMetricsCalculator.summarizeDay(
            listOf(wholeFood, upfSnack)
        )

        assertEquals(1000.0, summary.totalCalories, 0.01)
        assertEquals(400.0, summary.totalWeightGrams, 0.01)
        assertEquals(250.0, summary.upfCalories, 0.01)
        assertEquals(100.0, summary.upfWeightGrams, 0.01)
        assertEquals(25.0, summary.upfCaloriePercentage, 0.01)
        assertEquals(25.0, summary.upfWeightPercentage, 0.01)
        assertEquals(1, summary.seedOilItemCount)
        assertEquals(listOf("sunflower", "canola"), summary.distinctSeedOilsToday)
    }

    @Test
    fun `OpenFoodFactsApi searchFoods parses search hits with lenient arrays and numbers`() = runTest {
        val mockSearchJson = """
            {
              "count": 2,
              "page": 1,
              "page_size": 2,
              "hits": [
                {
                  "code": "5000108236832",
                  "product_name": "Oat Bran",
                  "brands": ["Bob's Red Mill", "Bob's"],
                  "serving_size": "40 g",
                  "serving_quantity": "40",
                  "nova_group": "1",
                  "ingredients_text": "100% Oat Bran",
                  "nutriments": {
                    "energy-kcal_100g": 360.0,
                    "proteins_100g": 17.0,
                    "carbohydrates_100g": 66.0,
                    "fat_100g": 7.0,
                    "fiber_100g": 15.0
                  }
                },
                {
                  "code": "4056489822479",
                  "product_name_en": "Protein Bar",
                  "brands": "PowerCo",
                  "serving_size": "1 bar (60g)",
                  "nova_group": 4,
                  "ingredients_text": "Soy protein, Canola Oil, Milk Chocolate",
                  "nutriments": {
                    "energy-kcal_100g": 400.0,
                    "proteins_100g": 35.0,
                    "carbohydrates_100g": 30.0,
                    "fat_100g": 12.0
                  }
                }
              ]
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("https://search.openfoodfacts.org/search", request.url.toString().substringBefore("?"))
            assertEquals("MacroAI/1.0.0 (Android; PersonalUse; contact: macroai-support@example.com)", request.headers[HttpHeaders.UserAgent])
            respond(
                content = mockSearchJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(testJson) }
        }

        val api = OpenFoodFactsApi(client)
        val result = api.searchFoods("oat bran")

        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(2, list.size)

        val oat = list[0]
        assertEquals("Bob's Red Mill — Oat Bran", oat.name)
        assertEquals("Bob's Red Mill", oat.brand)
        assertEquals(40.0, oat.defaultServingGrams, 0.01)
        assertEquals(1, oat.novaGroup)
        assertFalse(oat.isUpf)
        assertFalse(oat.containsSeedOils)
        assertEquals(360.0, oat.caloriesPer100g, 0.01)

        val bar = list[1]
        assertEquals("PowerCo — Protein Bar", bar.name)
        assertEquals("PowerCo", bar.brand)
        assertEquals(60.0, bar.defaultServingGrams, 0.01)
        assertEquals(4, bar.novaGroup)
        assertTrue(bar.isUpf)
        assertTrue(bar.containsSeedOils)
        assertEquals("canola", bar.detectedSeedOilsCsv)
    }

    @Test
    fun `ServingOption correctly computes grams and scales FoodLogEntity with SEARCH source`() {
        val product = com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct(
            barcode = "5000108236832",
            name = "Oat Bran",
            ingredientsText = "100% Oat Bran",
            defaultServingGrams = 40.0,
            novaGroup = 1,
            isUpf = false,
            containsSeedOils = false,
            detectedSeedOilsCsv = "",
            caloriesPer100g = 360.0,
            proteinPer100g = 17.0,
            carbsPer100g = 66.0,
            fatPer100g = 7.0,
            saturatedFatPer100g = 1.2,
            polyunsaturatedFatPer100g = 2.8,
            monounsaturatedFatPer100g = 2.5,
            sugarPer100g = 1.0,
            fibrePer100g = 15.0,
            sodiumMgPer100g = 5.0,
            potassiumMgPer100g = 350.0,
            ironMgPer100g = 4.0,
            calciumMgPer100g = 50.0,
            vitaminCMgPer100g = 0.0,
            vitaminDUgPer100g = 0.0,
            magnesiumMgPer100g = 180.0,
            brand = "Bob's Red Mill",
            servingSizeName = "1 bowl (40g)",
            servingQuantityGrams = 40.0
        )

        val options = com.macroai.tracker.domain.model.ServingOption.optionsForProduct(product)
        assertEquals(4, options.size)

        val gramsOpt = options.first { it.type == com.macroai.tracker.domain.model.ServingUnitType.GRAMS }
        val servingOpt = options.first { it.type == com.macroai.tracker.domain.model.ServingUnitType.SERVING }
        val ozOpt = options.first { it.type == com.macroai.tracker.domain.model.ServingUnitType.OUNCES }

        assertEquals(80.0, gramsOpt.calculateTotalGrams(80.0), 0.01)
        assertEquals(60.0, servingOpt.calculateTotalGrams(1.5), 0.01) // 1.5 * 40g = 60g
        assertEquals(56.7, ozOpt.calculateTotalGrams(2.0), 0.1) // 2 oz ~ 56.7g

        // Scale to 50g FoodLogEntity with LogSource.SEARCH
        val entity = product.scaleToEntity(
            servingWeightGrams = 50.0,
            mealType = MealType.BREAKFAST,
            source = LogSource.SEARCH
        )

        assertEquals(LogSource.SEARCH, entity.source)
        assertEquals(MealType.BREAKFAST, entity.mealType)
        assertEquals(50.0, entity.servingWeightGrams, 0.01)
        assertEquals(180.0, entity.calories, 0.01) // 360 * 0.5
        assertEquals(8.5, entity.proteinG, 0.01)  // 17 * 0.5
        assertEquals(33.0, entity.carbsG, 0.01)   // 66 * 0.5
        assertEquals(3.5, entity.fatG, 0.01)      // 7 * 0.5
        assertEquals(7.5, entity.fibreG, 0.01)    // 15 * 0.5
        assertFalse(entity.isUpf)
        assertFalse(entity.containsSeedOils)
    }

    @Test
    fun `SavedMealEntity fromIngredients aggregates ingredients and calculates per-serving metrics`() {
        val ingredients = listOf(
            com.macroai.tracker.domain.model.MealIngredient(
                name = "Minced Beef 5%",
                weightGrams = 500.0,
                calories = 650.0,
                proteinG = 105.0,
                carbsG = 0.0,
                fatG = 25.0,
                saturatedFatG = 10.0,
                polyunsaturatedFatG = 1.0,
                monounsaturatedFatG = 11.0,
                sugarG = 0.0,
                fibreG = 0.0,
                novaGroup = 1,
                containsSeedOils = false
            ),
            com.macroai.tracker.domain.model.MealIngredient(
                name = "Passata",
                weightGrams = 500.0,
                calories = 150.0,
                proteinG = 7.5,
                carbsG = 25.0,
                fatG = 1.0,
                saturatedFatG = 0.2,
                polyunsaturatedFatG = 0.3,
                monounsaturatedFatG = 0.2,
                sugarG = 20.0,
                fibreG = 6.0,
                novaGroup = 1,
                containsSeedOils = false
            ),
            com.macroai.tracker.domain.model.MealIngredient(
                name = "Store Bought Sauce",
                weightGrams = 200.0,
                calories = 200.0,
                proteinG = 2.0,
                carbsG = 20.0,
                fatG = 12.0,
                saturatedFatG = 1.5,
                polyunsaturatedFatG = 6.5,
                monounsaturatedFatG = 3.0,
                sugarG = 15.0,
                fibreG = 2.0,
                novaGroup = 4,
                containsSeedOils = true,
                detectedSeedOils = "sunflower"
            )
        )

        val recipe = com.macroai.tracker.data.local.entity.SavedMealEntity.fromIngredients(
            name = "Family Bolognese",
            description = "Family batch recipe",
            servings = 4.0,
            ingredients = ingredients
        )

        assertEquals("Family Bolognese", recipe.name)
        assertEquals(4.0, recipe.totalServings, 0.01)
        assertEquals(1200.0, recipe.totalWeightGrams, 0.01)
        assertEquals(1000.0, recipe.totalCalories, 0.01)
        assertEquals(114.5, recipe.totalProteinG, 0.01)
        assertEquals(45.0, recipe.totalCarbsG, 0.01)
        assertEquals(38.0, recipe.totalFatG, 0.01)
        assertEquals(4, recipe.novaGroup)
        assertTrue(recipe.isUpf)
        assertTrue(recipe.containsSeedOils)
        assertEquals("sunflower", recipe.detectedSeedOils)

        // Per serving calculations (1/4th)
        assertEquals(250.0, recipe.perServingCalories, 0.1) // 1000 / 4
        assertEquals(28.6, recipe.perServingProtein, 0.1)  // 114.5 / 4
        assertEquals(11.2, recipe.perServingCarbs, 0.1)    // 45 / 4 = 11.25 -> 11.2 (round to even)
        assertEquals(9.5, recipe.perServingFat, 0.1)       // 38 / 4
        assertEquals(300.0, recipe.perServingWeightGrams, 0.1)

        // Log 1 portion (1/4th)
        val log1Portion = recipe.toFoodLogEntity(
            servingsToLog = 1.0,
            mealType = MealType.DINNER
        )

        assertEquals("Family Bolognese (1 of 4 servings)", log1Portion.name)
        assertEquals(MealType.DINNER, log1Portion.mealType)
        assertEquals(300.0, log1Portion.servingWeightGrams, 0.1)
        assertEquals(250.0, log1Portion.calories, 0.1)
        assertEquals(28.63, log1Portion.proteinG, 0.1)
        assertEquals(11.25, log1Portion.carbsG, 0.1)
        assertEquals(9.5, log1Portion.fatG, 0.1)
        assertTrue(log1Portion.isUpf)
        assertTrue(log1Portion.containsSeedOils)
        assertEquals(1.0, log1Portion.servingsConsumed ?: 0.0, 0.01)

        // Verify ingredient breakdown in the logged entity is scaled to 1/4th
        val parsedIngredients = log1Portion.parseIngredients()
        assertEquals(3, parsedIngredients.size)
        assertEquals(125.0, parsedIngredients[0].weightGrams, 0.01) // 500g * 0.25
        assertEquals(162.5, parsedIngredients[0].calories, 0.01)   // 650 * 0.25
        assertEquals(26.25, parsedIngredients[0].proteinG, 0.01)
    }

    @Test
    fun `FoodLogEntity withUpdatedIngredients dynamically recalculates totals when component weights change`() {
        val initialIngredients = listOf(
            com.macroai.tracker.domain.model.MealIngredient(
                name = "White Rice",
                weightGrams = 200.0,
                calories = 260.0,
                proteinG = 5.0,
                carbsG = 56.0,
                fatG = 0.5,
                novaGroup = 1
            ),
            com.macroai.tracker.domain.model.MealIngredient(
                name = "Grilled Salmon",
                weightGrams = 150.0,
                calories = 310.0,
                proteinG = 34.0,
                carbsG = 0.0,
                fatG = 18.0,
                novaGroup = 1
            )
        )

        val log = com.macroai.tracker.data.local.entity.FoodLogEntity(
            name = "Salmon & Rice",
            mealType = MealType.LUNCH,
            source = LogSource.AI_PHOTO,
            servingWeightGrams = 350.0,
            calories = 570.0,
            proteinG = 39.0,
            carbsG = 56.0,
            fatG = 18.5,
            ingredientBreakdownJson = Json.encodeToString(initialIngredients)
        )

        // User reduces rice from 200g to 100g (half) and increases salmon to 200g
        val updatedIngredients = listOf(
            initialIngredients[0].withWeight(100.0), // 130 kcal, 2.5g P, 28g C, 0.25g F
            initialIngredients[1].withWeight(200.0)  // 413.3 kcal, 45.3g P, 0g C, 24g F
        )

        val updatedLog = log.withUpdatedIngredients(updatedIngredients)

        assertEquals(300.0, updatedLog.servingWeightGrams, 0.1) // 100 + 200
        assertEquals(543.3, updatedLog.calories, 0.2)           // 130 + 413.3
        assertEquals(47.8, updatedLog.proteinG, 0.2)           // 2.5 + 45.33
        assertEquals(28.0, updatedLog.carbsG, 0.2)             // 28 + 0
        assertEquals(24.3, updatedLog.fatG, 0.2)               // 0.25 + 24
        assertFalse(updatedLog.isSyncedToSheets)

        // Now save this logged meal as a recipe
        val recipe = updatedLog.toSavedMealEntity("My Prepped Salmon Meal", servings = 1.0)
        assertEquals("My Prepped Salmon Meal", recipe.name)
        assertEquals(1.0, recipe.totalServings, 0.01)
        assertEquals(300.0, recipe.totalWeightGrams, 0.1)
        assertEquals(543.3, recipe.totalCalories, 0.2)
    }

    @Test
    fun `FoodLogEntity withScaledWeight scales single items proportionally`() {
        val singleItem = com.macroai.tracker.data.local.entity.FoodLogEntity(
            name = "Peanut Butter",
            mealType = MealType.SNACK,
            source = LogSource.SEARCH,
            servingWeightGrams = 30.0,
            calories = 180.0,
            proteinG = 8.0,
            carbsG = 6.0,
            fatG = 15.0,
            fibreG = 2.0
        )

        // Scale to 45g (1.5x)
        val scaled = singleItem.withScaledWeight(45.0)

        assertEquals(45.0, scaled.servingWeightGrams, 0.01)
        assertEquals(270.0, scaled.calories, 0.01) // 180 * 1.5
        assertEquals(12.0, scaled.proteinG, 0.01)  // 8 * 1.5
        assertEquals(9.0, scaled.carbsG, 0.01)     // 6 * 1.5
        assertEquals(22.5, scaled.fatG, 0.01)      // 15 * 1.5
        assertEquals(3.0, scaled.fibreG, 0.01)     // 2 * 1.5
        assertFalse(scaled.isSyncedToSheets)
    }

    @Test
    fun `StapleIngredientsDatabase search with 5 percent fat minced beef returns 5 percent beef staple at top rank`() {
        val results = com.macroai.tracker.data.local.StapleIngredientsDatabase.search("5% fat minced beef")
        assertTrue("Search should return results for 5% fat minced beef", results.isNotEmpty())

        val topResult = results.first()
        assertTrue("Top result should be 5% minced beef, but got: ${topResult.name}", topResult.name.contains("5% Fat", ignoreCase = true))
        assertEquals("UK/Irish CoFID Staple", topResult.brand)
        assertEquals(124.0, topResult.caloriesPer100g, 0.1)
        assertEquals(21.4, topResult.proteinPer100g, 0.1)
        assertEquals(4.5, topResult.fatPer100g, 0.1)
        assertEquals(0.0, topResult.carbsPer100g, 0.1)
        assertEquals(1, topResult.novaGroup)
        assertFalse(topResult.isUpf)
        assertFalse(topResult.containsSeedOils)
    }

    @Test
    fun `StapleIngredientsDatabase search skinless boneless chick thighs returns chicken thighs at rank 1`() {
        val results = com.macroai.tracker.data.local.StapleIngredientsDatabase.search("skinless boneless chick thighs")
        assertTrue("Search should find skinless boneless chicken thighs for query 'skinless boneless chick thighs'", results.isNotEmpty())

        val top = results.first()
        assertEquals("Chicken Thighs (Skinless & Boneless, Raw)", top.name)
        assertEquals("UK/Irish CoFID Staple", top.brand)
        assertEquals(152.0, top.caloriesPer100g, 0.1)
        assertEquals(19.3, top.proteinPer100g, 0.1)
        assertEquals(8.3, top.fatPer100g, 0.1)
        assertEquals(0.0, top.carbsPer100g, 0.1)
        assertEquals(1, top.novaGroup)
    }

    @Test
    fun `StapleIngredientsDatabase search chicken breast returns raw and cooked chicken breast`() {
        val results = com.macroai.tracker.data.local.StapleIngredientsDatabase.search("chicken breast")
        assertTrue("Search should find chicken breast staples", results.isNotEmpty())

        val names = results.map { it.name }
        assertTrue("Results should include Chicken Breast", names.any { it.contains("Chicken Breast", ignoreCase = true) })
        assertTrue("Results should all be NOVA 1 whole foods", results.all { it.novaGroup == 1 })
    }

    @Test
    fun `StapleIngredientsDatabase search olive oil returns extra virgin olive oil without seed oils`() {
        val results = com.macroai.tracker.data.local.StapleIngredientsDatabase.search("olive oil")
        assertTrue("Search should find olive oil", results.isNotEmpty())

        val topResult = results.first()
        assertTrue("Top result for olive oil should be pure olive oil, but got: ${topResult.name}", topResult.name.contains("Olive Oil", ignoreCase = true) && !topResult.name.contains("Tuna", ignoreCase = true))
        assertFalse("Olive oil should not be flagged as seed oil", topResult.containsSeedOils)
        assertEquals(14.0, topResult.saturatedFatPer100g, 0.5)
        assertEquals(11.0, topResult.polyunsaturatedFatPer100g, 0.5)
        assertEquals(73.0, topResult.monounsaturatedFatPer100g, 1.0)
    }

    @Test
    fun `IngredientPortionSelector serving options correctly calculate gram weights`() {
        val mincedBeef = com.macroai.tracker.data.local.StapleIngredientsDatabase.staples.first { it.barcode == "staple_beef_5pct" }
        val options = com.macroai.tracker.domain.model.ServingOption.optionsForProduct(mincedBeef)

        assertTrue("Options should have at least 100g, grams, oz, and package serving", options.size >= 4)

        val option100g = options.first { it.type == com.macroai.tracker.domain.model.ServingUnitType.HUNDRED_GRAMS }
        assertEquals(200.0, option100g.calculateTotalGrams(2.0), 0.01) // 2 x 100g = 200g

        val optionGrams = options.first { it.type == com.macroai.tracker.domain.model.ServingUnitType.GRAMS }
        assertEquals(350.0, optionGrams.calculateTotalGrams(350.0), 0.01)

        val optionOz = options.first { it.type == com.macroai.tracker.domain.model.ServingUnitType.OUNCES }
        assertEquals(56.7, optionOz.calculateTotalGrams(2.0), 0.1) // 2 oz ~ 56.7g

        val optionServing = options.first { it.type == com.macroai.tracker.domain.model.ServingUnitType.SERVING }
        assertEquals(150.0, optionServing.calculateTotalGrams(1.0), 0.01) // 1 serving = 150g
        assertEquals(300.0, optionServing.calculateTotalGrams(2.0), 0.01) // 2 servings = 300g
    }

    @Test
    fun `EndOfDayCalculator computes daily calorie deficit and 4-week weight projection statement`() {
        val profile = com.macroai.tracker.domain.model.UserProfile(
            weightKg = 80.0,
            heightCm = 180.0,
            age = 30,
            biologicalSex = com.macroai.tracker.domain.model.BiologicalSex.MALE,
            tdeeMode = com.macroai.tracker.domain.model.TdeeMode.MANUAL_OVERRIDE,
            manualOverrideKcal = 2500,
            energyTargetMode = com.macroai.tracker.domain.model.EnergyTargetMode.TDEE_DEFICIT,
            targetDeficitKcal = 0
        )
        val tdee = com.macroai.tracker.domain.util.TdeeCalculator.calculateTdee(profile)
        val summary = com.macroai.tracker.domain.util.DailyNutritionSummary(
            totalCalories = 1950.0,
            proteinG = 175.0,
            carbsG = 180.0,
            fatG = 60.0
        )

        // Daily deficit = 2500 - 1950 = 550 kcal
        // 4-week (28 days) total deficit = 550 * 28 = 15,400 kcal
        // Weight change = -15,400 / 7700 = -2.0 kg -> Projected weight = 78.0 kg
        val metrics = com.macroai.tracker.domain.util.EndOfDayCalculator.calculateMetrics(
            summary = summary,
            tdee = tdee
        )

        assertEquals(1950, metrics.consumedCalories)
        assertEquals(2500, metrics.maintenanceExpenditureKcal)
        assertEquals(550, metrics.dailyCalorieDeficitKcal)
        assertEquals(80.0, metrics.currentWeightKg, 0.01)
        assertEquals(-2.0, metrics.weightChange4WeeksKg, 0.01)
        assertEquals(78.0, metrics.projectedWeight4WeeksKg, 0.01)
        assertEquals(
            "If you keep up with this, you'll weigh 78.0 kg in 4 weeks",
            metrics.fourWeekProjectionStatement
        )
    }

    @Test
    fun `TdeeCalculator and EndOfDayCalculator separate TDEE expenditure from set calorie consumed goal`() {
        // User with BMR = 1780 kcal, Moderate activity (1.55x) -> TDEE = 2759 kcal
        // User sets a fixed calorie consumed goal of 2100 kcal
        val profile = com.macroai.tracker.domain.model.UserProfile(
            weightKg = 80.0,
            heightCm = 180.0,
            age = 30,
            biologicalSex = com.macroai.tracker.domain.model.BiologicalSex.MALE,
            activityLevel = com.macroai.tracker.domain.model.ActivityLevel.MODERATE,
            tdeeMode = com.macroai.tracker.domain.model.TdeeMode.FORMULA_MULTIPLIER,
            energyTargetMode = com.macroai.tracker.domain.model.EnergyTargetMode.SET_CALORIE_GOAL,
            customCalorieTargetKcal = 2100
        )
        val tdee = com.macroai.tracker.domain.util.TdeeCalculator.calculateTdee(profile)

        // Verify TDEE (burn) is 2759, while energy target (intake goal) is 2100
        assertEquals(2759, tdee.tdeeKcal)
        assertEquals(2100, tdee.energyTargetKcal)
        assertEquals(659, tdee.plannedDeficitKcal)

        // User logs exactly 2100 kcal consumed
        val summary = com.macroai.tracker.domain.util.DailyNutritionSummary(totalCalories = 2100.0)
        val metrics = com.macroai.tracker.domain.util.EndOfDayCalculator.calculateMetrics(
            summary = summary,
            tdee = tdee
        )

        // Deficit relative to true TDEE: 2759 - 2100 = 659 kcal
        // 4-week weight loss: -(659 * 28) / 7700 = -2.4 kg
        assertEquals(2100, metrics.consumedCalories)
        assertEquals(2100, metrics.dailyBudgetKcal)
        assertEquals(2759, metrics.maintenanceExpenditureKcal)
        assertEquals(659, metrics.dailyCalorieDeficitKcal)
        assertEquals(0, metrics.budgetVarianceKcal) // Hit budget exactly
        assertEquals(-2.4, metrics.weightChange4WeeksKg, 0.01)
        assertEquals(77.6, metrics.projectedWeight4WeeksKg, 0.01)
        assertEquals(
            "If you keep up with this, you'll weigh 77.6 kg in 4 weeks",
            metrics.fourWeekProjectionStatement
        )
    }

    @Test
    fun `GeminiMealAnalyzer parses structured GeminiDayOverviewDto JSON`() {
        val client = HttpClient(MockEngine { respond("") })
        val analyzer = GeminiMealAnalyzer(client, testJson)

        val dayOverviewJson = """
            {
              "overallScore": 92,
              "scoreGrade": "A",
              "headline": "Exceptional high-protein whole-food day with zero seed oils and a 550 kcal deficit.",
              "healthScore": 95,
              "proteinAndMacroScore": 94,
              "calorieControlScore": 88,
              "executiveSummary": "You hit 100% of your protein goal using NOVA 1 whole foods while maintaining a clean 550 kcal deficit.",
              "keyWins": [
                "Hit 175g protein target from whole-food sources",
                "0% NOVA 4 ultra-processed foods and zero seed oils"
              ],
              "actionableImprovements": [
                "Add 1 serving of leafy greens to boost magnesium intake"
              ]
            }
        """.trimIndent()

        val dto = analyzer.parseDayOverviewJson(dayOverviewJson)
        assertEquals(92, dto.overallScore)
        assertEquals("A", dto.scoreGrade)
        assertEquals(95, dto.healthScore)
        assertEquals(94, dto.proteinAndMacroScore)
        assertEquals(88, dto.calorieControlScore)
        assertEquals(2, dto.keyWins.size)
        assertEquals(1, dto.actionableImprovements.size)
    }

    @Test
    fun `WeightUnit converts accurately between kilograms and pounds`() {
        val kg = 75.0
        val lbs = com.macroai.tracker.domain.model.WeightUnit.KG.fromKg(kg)
        assertEquals(75.0, lbs, 0.001)

        val convertedLbs = com.macroai.tracker.domain.model.WeightUnit.LBS.fromKg(kg)
        assertEquals(165.346, convertedLbs, 0.01)

        val backToKg = com.macroai.tracker.domain.model.WeightUnit.LBS.toKg(convertedLbs)
        assertEquals(75.0, backToKg, 0.01)

        assertEquals("75.0 kg", com.macroai.tracker.domain.model.WeightUnit.KG.format(kg))
        assertEquals("165.3 lbs", com.macroai.tracker.domain.model.WeightUnit.LBS.format(kg))
    }

    @Test
    fun `TdeeCalculator respects autoSyncHealthConnectBiometrics flag`() {
        val profileWithAutoSync = com.macroai.tracker.domain.model.UserProfile(
            weightKg = 70.0,
            heightCm = 175.0,
            autoSyncHealthConnectBiometrics = true
        )

        val profileWithoutAutoSync = com.macroai.tracker.domain.model.UserProfile(
            weightKg = 70.0,
            heightCm = 175.0,
            autoSyncHealthConnectBiometrics = false
        )

        // Health Connect has newer weight (80kg) and height (180cm)
        val hcWeight = 80.0
        val hcHeight = 180.0

        val resultAutoSync = com.macroai.tracker.domain.util.TdeeCalculator.calculateTdee(
            profile = profileWithAutoSync,
            healthConnectWeightKg = hcWeight,
            healthConnectHeightCm = hcHeight
        )

        val resultManualOnly = com.macroai.tracker.domain.util.TdeeCalculator.calculateTdee(
            profile = profileWithoutAutoSync,
            healthConnectWeightKg = hcWeight,
            healthConnectHeightCm = hcHeight
        )

        // With auto-sync enabled, Health Connect takes effect
        assertEquals(80.0, resultAutoSync.effectiveWeightKg, 0.01)
        assertEquals(180.0, resultAutoSync.effectiveHeightCm, 0.01)
        assertTrue(resultAutoSync.isUsingHealthConnectWeight)
        assertTrue(resultAutoSync.isUsingHealthConnectHeight)

        // With auto-sync disabled, manual profile takes precedence
        assertEquals(70.0, resultManualOnly.effectiveWeightKg, 0.01)
        assertEquals(175.0, resultManualOnly.effectiveHeightCm, 0.01)
        assertFalse(resultManualOnly.isUsingHealthConnectWeight)
        assertFalse(resultManualOnly.isUsingHealthConnectHeight)
    }

    @Test
    fun `EndOfDayCalculator formats 4-week weight projection in selected weight unit`() {
        val profile = com.macroai.tracker.domain.model.UserProfile(
            weightKg = 80.0,
            heightCm = 180.0,
            manualOverrideKcal = 2000,
            weightUnit = com.macroai.tracker.domain.model.WeightUnit.LBS
        )
        val tdee = com.macroai.tracker.domain.util.TdeeCalculator.calculateTdee(profile)
        val summary = com.macroai.tracker.domain.util.DailyNutritionSummary(totalCalories = 1500.0)

        val metricsLbs = com.macroai.tracker.domain.util.EndOfDayCalculator.calculateMetrics(
            summary = summary,
            tdee = tdee,
            weightUnit = com.macroai.tracker.domain.model.WeightUnit.LBS
        )

        val metricsKg = com.macroai.tracker.domain.util.EndOfDayCalculator.calculateMetrics(
            summary = summary,
            tdee = tdee,
            weightUnit = com.macroai.tracker.domain.model.WeightUnit.KG
        )

        assertTrue(metricsLbs.fourWeekProjectionStatement.contains("lbs"))
        assertTrue(metricsKg.fourWeekProjectionStatement.contains("kg"))
    }

    @Test
    fun `MacroAiWidgetSnapshot calculates progress ratios, budget surplus and deficit, and warnings correctly`() {
        val normalSnapshot = com.macroai.tracker.ui.widget.MacroAiWidgetSnapshot(
            calorieTarget = 2000,
            caloriesConsumed = 1500,
            remainingCalories = 500,
            proteinConsumedG = 120f,
            proteinTargetG = 160f,
            carbsConsumedG = 150f,
            carbsTargetG = 200f,
            fatConsumedG = 45f,
            fatTargetG = 65f,
            upfPercentage = 15.5f,
            seedOilCount = 0
        )

        assertEquals(0.75f, normalSnapshot.calorieProgress, 0.01f)
        assertFalse(normalSnapshot.isOverBudget)
        assertEquals(500, normalSnapshot.remainingCalories)
        assertEquals(0.75f, normalSnapshot.proteinProgress, 0.01f)
        assertEquals(0.75f, normalSnapshot.carbsProgress, 0.01f)
        assertEquals(0.692f, normalSnapshot.fatProgress, 0.01f)

        // Test over budget
        val overBudgetSnapshot = normalSnapshot.copy(
            caloriesConsumed = 2350,
            remainingCalories = -350,
            seedOilCount = 1
        )
        assertTrue(overBudgetSnapshot.isOverBudget)
        assertEquals(1.0f, overBudgetSnapshot.calorieProgress, 0.001f)
        assertEquals(-350, overBudgetSnapshot.remainingCalories)
        assertEquals(1, overBudgetSnapshot.seedOilCount)
    }

    @Test
    fun `AiModel resolves 3_1 Flash Lite and 3_5 Flash Lite accurately`() {
        val m31 = com.macroai.tracker.domain.model.AiModel.GEMINI_3_1_FLASH_LITE
        val m35 = com.macroai.tracker.domain.model.AiModel.GEMINI_3_5_FLASH_LITE

        assertEquals("gemini-3.1-flash-lite", m31.modelId)
        assertEquals("3.1 Flash Lite", m31.shortLabel)
        assertEquals("gemini-3.5-flash-lite", m35.modelId)
        assertEquals("3.5 Flash Lite", m35.shortLabel)

        assertEquals(m35, com.macroai.tracker.domain.model.AiModel.fromModelId("gemini-3.5-flash-lite"))
        assertEquals(m31, com.macroai.tracker.domain.model.AiModel.fromModelId("gemini-3.1-flash-lite"))
        assertEquals(m31, com.macroai.tracker.domain.model.AiModel.fromModelId(null))
    }

    @Test
    fun `EndOfDayCalculator generateLocalReport produces instantaneous backend report without AI`() {
        val profile = com.macroai.tracker.domain.model.UserProfile(
            weightKg = 80.0,
            heightCm = 180.0,
            manualOverrideKcal = 2400
        )
        val tdee = com.macroai.tracker.domain.util.TdeeCalculator.calculateTdee(profile)
        val summary = com.macroai.tracker.domain.util.DailyNutritionSummary(
            totalCalories = 1900.0,
            proteinG = 175.0,
            carbsG = 180.0,
            fatG = 52.0,
            fibreG = 30.0,
            upfCaloriePercentage = 5.0
        )

        val localReport = com.macroai.tracker.domain.util.EndOfDayCalculator.generateLocalReport(
            logs = emptyList(),
            summary = summary,
            tdee = tdee
        )

        assertFalse(localReport.isAiGenerated)
        assertEquals("On-Device Clinical Engine", localReport.modelUsed)
        assertEquals(1900, localReport.metrics.consumedCalories)
        assertTrue(localReport.aiOverview.overallScore >= 80)
        assertTrue(localReport.aiOverview.keyWins.isNotEmpty())
    }

    @Test
    fun `StapleIngredientsDatabase and scoreProductRelevance accurately rank Avonmore Chocolate Gold Protein Milk at rank 1`() {
        val results = com.macroai.tracker.data.local.StapleIngredientsDatabase.search(
            "Avonmore Chocolate Gold Protein Milk"
        )
        assertTrue("Search should find Avonmore Chocolate Gold Protein Milk", results.isNotEmpty())

        val top = results.first()
        assertEquals("5011056023559", top.barcode)
        assertEquals("Avonmore Chocolate Gold Protein Milk", top.name)
        assertEquals("Avonmore", top.brand)
        assertEquals(59.0, top.caloriesPer100g, 0.1)
        assertEquals(6.6, top.proteinPer100g, 0.1)

        // Also verify partial / reordered queries match accurately
        val partialResults = com.macroai.tracker.data.local.StapleIngredientsDatabase.search(
            "chocolate protein milk avonmore"
        )
        assertTrue(partialResults.isNotEmpty())
        assertEquals("5011056023559", partialResults.first().barcode)
    }

    @Test
    fun `CameraCaptureMode defaults to BARCODE_SCAN as first option with AI_PHOTO second`() {
        val modes = com.macroai.tracker.ui.camera.CameraCaptureMode.entries
        assertEquals(com.macroai.tracker.ui.camera.CameraCaptureMode.BARCODE_SCAN, modes[0])
        assertEquals(com.macroai.tracker.ui.camera.CameraCaptureMode.AI_PHOTO, modes[1])

        val defaultState = com.macroai.tracker.ui.camera.CameraUiState()
        assertEquals(com.macroai.tracker.ui.camera.CameraCaptureMode.BARCODE_SCAN, defaultState.captureMode)
    }

    @Test
    fun `GeminiMealAnalyzer getEffectiveApiKey respects override and fallback`() = runTest {
        val mockEngine = MockEngine { respond("ok", HttpStatusCode.OK) }
        val client = HttpClient(mockEngine)
        val analyzer = GeminiMealAnalyzer(client, testJson, null)

        val explicit = analyzer.getEffectiveApiKey("custom_override_123")
        assertEquals("custom_override_123", explicit)

        val fallback = analyzer.getEffectiveApiKey(null)
        // If BuildConfig has a key, it returns it; otherwise returns empty string
        assertEquals(BuildConfig.GEMINI_API_KEY.trim(), fallback)
    }

    @Test
    fun `SheetsWebhookClient getEffectiveWebhookUrl and testWebhookUrl succeed on HTTP 200`() = runTest {
        var pingReceived = false
        val mockEngine = MockEngine { request ->
            if (request.url.toString() == "https://script.google.com/test_webhook") {
                pingReceived = true
                respond("""{"status":"ok","message":"pong"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
            } else {
                respond("error", HttpStatusCode.InternalServerError)
            }
        }
        val client = HttpClient(mockEngine)
        val sheetsClient = com.macroai.tracker.data.remote.sheets.SheetsWebhookClient(client, null)

        val overrideUrl = sheetsClient.getEffectiveWebhookUrl("https://script.google.com/custom")
        assertEquals("https://script.google.com/custom", overrideUrl)

        val testSuccess = sheetsClient.testWebhookUrl("https://script.google.com/test_webhook")
        assertTrue(testSuccess.isSuccess)
        assertTrue(testSuccess.getOrThrow())
        assertTrue(pingReceived)

        val testFail = sheetsClient.testWebhookUrl("https://script.google.com/bad_webhook")
        assertTrue(testFail.isFailure)
    }
}



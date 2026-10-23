package com.macroai.tracker.data.remote.off

import com.macroai.tracker.data.remote.off.dto.NormalizedOffProduct
import com.macroai.tracker.data.remote.off.dto.OffProductResponseDto
import com.macroai.tracker.data.remote.off.dto.OffSearchResponseDto
import com.macroai.tracker.data.remote.off.dto.toNormalizedProduct
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenFoodFactsApi @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/api/v2/product"
        const val SEARCH_URL = "https://search.openfoodfacts.org/search"
        const val USER_AGENT = "MacroAI/1.0.0 (Android; PersonalUse; contact: macroai-support@example.com)"
        const val SEARCH_FIELDS = "code,product_name,product_name_en,brands,serving_size,serving_quantity,serving_quantity_unit,nova_group,ingredients_text,ingredients_text_en,nutriments,image_front_small_url"
    }

    suspend fun getProductByBarcode(barcode: String): Result<NormalizedOffProduct> = runCatching {
        val cleanBarcode = barcode.trim()
        require(cleanBarcode.isNotEmpty()) { "Barcode must not be empty" }

        val response = httpClient.get("$BASE_URL/$cleanBarcode.json") {
            header(HttpHeaders.UserAgent, USER_AGENT)
        }

        if (!response.status.isSuccess()) {
            throw IllegalStateException("Open Food Facts HTTP ${response.status.value}")
        }

        val body = response.body<OffProductResponseDto>()
        val product = body.product
            ?: throw NoSuchElementException("Product not found in Open Food Facts for barcode $cleanBarcode")

        product.toNormalizedProduct(cleanBarcode)
    }

    suspend fun searchFoods(query: String, pageSize: Int = 50): Result<List<NormalizedOffProduct>> = runCatching {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return@runCatching emptyList()

        val queryLower = cleanQuery.lowercase()
        val queryTokens = queryLower.split(Regex("""[\s,.-]+""")).filter { it.isNotEmpty() }
        val stemmedTokens = queryTokens.map { com.macroai.tracker.data.local.StapleIngredientsDatabase.stemWord(it) }

        val ukIrishBrands = setOf(
            "tesco", "sainsbury", "sainsbury's", "asda", "morrisons", "waitrose",
            "marks & spencer", "marks and spencer", "m&s", "co-op", "coop", "co-operative",
            "dunnes stores", "dunnes", "supervalu", "lidl", "aldi", "birchwood", "ashfields",
            "dewfresh", "deluxe", "kerrygold", "brennans", "avonmore", "denny", "ballyfree",
            "flahavan's", "flahavans", "quaker", "heinz", "warburtons", "hovis", "kingsmill",
            "cathedral city", "arla", "richmond", "birds eye", "muller", "yeo valley",
            "princes", "john west", "fage", "cravendale", "clonakilty", "charleville",
            "glenisk", "milbona", "brooklea", "grahams", "graham's", "fulfil", "grenade",
            "mooju", "keoghs", "keogh's", "tayto", "alpro", "danone", "actimel", "innocent"
        )

        val matchedBrand = ukIrishBrands.firstOrNull { brand ->
            queryTokens.contains(brand) || queryLower.startsWith("$brand ")
        }

        suspend fun executeSearchQuery(qParam: String, size: Int = pageSize): List<NormalizedOffProduct> {
            val response = httpClient.get(SEARCH_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                url {
                    parameters.append("q", qParam)
                    parameters.append("fields", SEARCH_FIELDS)
                    parameters.append("page_size", size.toString())
                }
            }
            if (!response.status.isSuccess()) return emptyList()
            val body = response.body<OffSearchResponseDto>()
            return body.hits
                .filter { !it.productName.isNullOrBlank() || !it.productNameEn.isNullOrBlank() }
                .map { it.toNormalizedProduct(it.code ?: "") }
                .filter { product ->
                    product.caloriesPer100g > 0.0 || product.proteinPer100g > 0.0 ||
                        product.fatPer100g > 0.0 || product.carbsPer100g > 0.0
                }
        }

        val combinedHits = mutableListOf<NormalizedOffProduct>()
        val seenCodes = mutableSetOf<String>()

        fun addUnique(products: List<NormalizedOffProduct>) {
            for (p in products) {
                val key = "${p.barcode}_${p.name.lowercase().trim()}"
                if (key !in seenCodes) {
                    seenCodes.add(key)
                    combinedHits.add(p)
                }
            }
        }

        // 1. If a known brand is in the query, execute a targeted Lucene brand filter query first
        if (matchedBrand != null) {
            val remainingTokens = queryTokens.filter { it != matchedBrand }
            val luceneBrandQuery = if (remainingTokens.isNotEmpty()) {
                "brands:\"$matchedBrand\" ${remainingTokens.joinToString(" ")}"
            } else {
                "brands:\"$matchedBrand\""
            }
            runCatching { executeSearchQuery(luceneBrandQuery, size = 40) }.getOrNull()?.let { addUnique(it) }

            // If remaining tokens had 3+ words (e.g. "Chocolate Gold Protein Milk") and brand query was too restrictive,
            // also query brand + primary keyword
            if (combinedHits.size < 5 && remainingTokens.size >= 2) {
                val broaderBrandQuery = "brands:\"$matchedBrand\""
                runCatching { executeSearchQuery(broaderBrandQuery, size = 50) }.getOrNull()?.let { addUnique(it) }
            }
        }

        // 2. Standard full-text query
        val primaryResults = executeSearchQuery(cleanQuery, size = pageSize)
        addUnique(primaryResults)

        // 3. Score and rank candidates by multi-field token coverage and UK/Irish brand relevance
        combinedHits
            .mapNotNull { product ->
                val nameLower = product.name.lowercase()
                val brandLower = (product.brand ?: "").lowercase()

                var score = com.macroai.tracker.data.local.StapleIngredientsDatabase.scoreProductRelevance(
                    product = product,
                    rawTokens = queryTokens,
                    stemmedTokens = stemmedTokens,
                    cleanQuery = queryLower
                )

                // Hits returned by search are preserved with a baseline score if token overlap is low
                if (score <= 0) {
                    score = 1
                }

                // Boost UK & Irish supermarket and dairy brands
                if (ukIrishBrands.any { brandLower.contains(it) || nameLower.contains(it) }) {
                    score += 55
                }

                // Penalize non-English foreign noise
                if (nameLower.contains("matière grasse") || nameLower.contains("surgelé") ||
                    nameLower.contains("façon") || nameLower.contains("bœuf") ||
                    nameLower.contains("haché") || nameLower.contains("fromage blanc") ||
                    nameLower.contains("eiweiß") || nameLower.contains("abtropfjoghurt")
                ) {
                    score -= 120
                }

                if (score > 0) product to score else null
            }
            .sortedWith(
                compareByDescending<Pair<NormalizedOffProduct, Int>> { it.second }
                    .thenByDescending { (product, _) ->
                        when (product.novaGroup) {
                            1 -> 40
                            2 -> 30
                            3 -> 20
                            4 -> 5
                            else -> 10
                        }
                    }
            )
            .map { it.first }
    }
}

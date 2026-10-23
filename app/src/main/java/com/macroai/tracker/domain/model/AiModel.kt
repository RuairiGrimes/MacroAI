package com.macroai.tracker.domain.model

enum class AiModel(
    val modelId: String,
    val displayName: String,
    val shortName: String,
    val description: String
) {
    GEMINI_3_1_FLASH_LITE(
        modelId = "gemini-3.1-flash-lite",
        displayName = "Gemini 3.1 Flash-Lite",
        shortName = "3.1 Flash Lite",
        description = "High rate limits, ultra-fast latency, optimal for daily meal logs"
    ),
    GEMINI_3_5_FLASH_LITE(
        modelId = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite",
        shortName = "3.5 Flash Lite",
        description = "Next-gen Flash Lite, separate quota pool to avoid peak traffic load"
    );

    companion object {
        val DEFAULT = GEMINI_3_1_FLASH_LITE

        fun fromModelId(id: String?): AiModel =
            entries.firstOrNull { it.modelId.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) } ?: DEFAULT
    }

    val shortLabel: String
        get() = shortName
}

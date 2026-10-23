package com.macroai.tracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class LogSource(val displayName: String) {
    BARCODE("Barcode"),
    AI_PHOTO("AI Photo"),
    SEARCH("Search"),
    MANUAL("Manual")
}


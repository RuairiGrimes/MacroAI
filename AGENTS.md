# Project: Personal AI Nutrition & UPF Tracker (Android)

## 1. Core Context & Constraints
- **Target Audience:** Single-user personal Android application (sideloaded via ADB).
- **No Backend Server:** All logic runs on-device. API keys (`GEMINI_API_KEY`, `SHEETS_WEBHOOK_URL`) are injected at build time via `local.properties` and `BuildConfig`.
- **Architecture:** MVVM / Clean Architecture (Single-Activity Jetpack Compose, Unidirectional Data Flow, Kotlin Coroutines + StateFlow).

## 2. Strict Tech Stack
- **Language & Build:** Kotlin 2.x, Gradle Version Catalog (`libs.versions.toml`), KSP (never use KAPT).
- **UI:** Jetpack Compose (Material 3), Navigation Compose (Type-Safe).
- **DI:** Hilt (`dagger.hilt.android`).
- **Local Storage:** Room Database (primary source of truth for instant UI) + Preferences DataStore (user profile, TDEE overrides).
- **Networking:** Ktor Client (`io.ktor:ktor-client-okhttp`) + `kotlinx.serialization`.
- **AI SDK:** Direct REST calls via Ktor to Gemini API (`gemini-3.1-flash-lite`) using Structured Outputs (`responseSchema`), or official Google GenAI SDK.
- **Camera & Vision:** CameraX + Google ML Kit Barcode Scanning (`com.google.mlkit:barcode-scanning`).
- **Health Integration:** Android Health Connect (`androidx.health.connect:connect-client`).
- **Background Sync:** Android WorkManager (syncs Room entries to Google Sheets Apps Script Webhook).

## 3. Domain Rules & Calculations
- **Seed Oils Flagging:** Parse ingredient strings (case-insensitive regex) for: `rapeseed`, `canola`, `sunflower`, `soybean`, `cottonseed`, `corn oil`, `safflower`, `grapeseed`, `vegetable oil`. Uses a 3-tier Green/Amber/Red traffic light system based on estimated grams, ratio, and cooking medium:
  - **Green (Low / Clean):** Seed-oil free (0g) or trace amounts (`<= 2.5g` or `<= 2.5%` ratio).
  - **Amber (Moderate):** Moderate cooking oil (`2.5g` to `10.0g` or `2.5%` to `10.0%` ratio).
  - **Red (High):** Heavy industrial or oxidised deep-frying oil (`> 10.0g` or `> 10.0%` ratio, or commercial deep-fried food).
- **UPF Tracking:** Use NOVA classification (1 to 4). Group 4 = Ultra-Processed Food. Track daily % of total calories and total gram weight derived from NOVA 4 items.
- **TDEE Logic:** 
  - Pull latest `WeightRecord`, `HeightRecord`, and `ActiveCaloriesBurnedRecord` from Health Connect.
  - Calculate BMR via Mifflin-St Jeor: `(10 * weight_kg) + (6.25 * height_cm) - (5 * age) + 5` (allow user configuration in settings).
  - Support two modes in settings: `Static Multiplier` (BMR * user multiplier) or `Dynamic` (BMR + Health Connect Active Calories). Manual TDEE override must always take precedence if toggled.
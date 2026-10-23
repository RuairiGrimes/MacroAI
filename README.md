# MacroAI

MacroAI is an open-source, on-device Android nutrition and food tracker. It tracks calories, macronutrients, ultra-processed food (NOVA 4) intake, and industrial seed oils.

The application runs entirely on-device without a proprietary backend server. User data is stored locally in a Room database and can optionally be synchronized to a personal Google Sheet via a Google Apps Script webhook. AI features (meal photo analysis, text-based recipe estimation, and daily reviews) call the Google Gemini API directly from the device using the user's own API key.

---

## Configuring API Keys and Google Sheets

Because MacroAI does not run a middleman backend or central account service, users configure their own Google Gemini API key and optional Google Sheets webhook. Once entered, these credentials are saved locally in the app's Preferences DataStore and persist across restarts.

### 1. Google Gemini API Key

A Gemini API key is required to use photo-based meal analysis, AI recipe generation, and daily AI nutrition feedback.

1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey) in a web browser and sign in with a Google account.
2. Click **Create API key** (or **Get API key**).
3. Copy the generated key string (starts with `AIzaSy`).
4. In the MacroAI app:
   - Navigate to the **Goals / Settings** screen (accessible from the top bar on the main dashboard).
   - Scroll down to section **4. Cloud & AI Integration**.
   - Paste your key into the **Gemini AI API Key** field.
   - Tap **Test Key** to verify that the key is valid.
   - Tap **Save Credentials**.

The free tier of Google AI Studio provides sufficient request limits for personal daily logging at no cost.

### 2. Google Sheets Webhook (Optional)

MacroAI can automatically sync logged foods and micronutrients to a Google Sheet in the background. If you do not configure a sheet, the app operates entirely offline and stores all data in the local SQLite database.

1. Open [Google Sheets](https://sheets.new) and create a new spreadsheet (e.g. name it "MacroAI Food Log").
2. In the top menu, go to **Extensions** > **Apps Script**.
3. Delete any default code in the editor (`Code.gs`).
4. Copy the entire contents of [`scripts/google_apps_script_webhook.js`](scripts/google_apps_script_webhook.js) from this repository and paste it into `Code.gs`.
5. Click the Save icon (or press `Ctrl+S`).
6. Click **Deploy** > **New deployment**:
   - For *Select type*, choose **Web app**.
   - Description: `MacroAI Webhook`
   - Execute as: **Me**
   - Who has access: **Anyone** *(required so the on-device sync worker can send POST requests without OAuth flows)*
7. Click **Deploy** and authorize the script when prompted by Google.
8. Copy the generated **Web app URL** (the URL must end with `/exec`).
9. In the MacroAI app:
   - In **Settings** under **4. Cloud & AI Integration**, paste the URL into the **Google Sheets Webhook URL** field.
   - Tap **Test Webhook** to confirm reachability.
   - Tap **Save Credentials**.

When entries are logged or edited, Android WorkManager sends rows in the background to your sheet, creating a `FoodLogs` tab populated with timestamps, macros, lipid breakdown (saturated fat, PUFA, MUFA), micronutrients, and NOVA classifications.

### 3. Build-Time Configuration (For Developers)

If you are compiling from source or running automated builds via GitHub Actions, credentials can optionally be injected at build time:

- **Via `local.properties`:**
  ```properties
  GEMINI_API_KEY=your_gemini_api_key_here
  SHEETS_WEBHOOK_URL=https://script.google.com/macros/s/.../exec
  ```
- **Via Environment Variables / GitHub Secrets:**
  Set `GEMINI_API_KEY` and `SHEETS_WEBHOOK_URL` in your shell environment or GitHub Repository Secrets. The Gradle build script reads these environment variables when `local.properties` is absent.

If both in-app credentials and build-time keys are present, in-app keys entered in Settings always take precedence.

---

## Features

- **Meal Photo Analysis:** Takes a meal photograph, compresses it to fit a single Gemini vision tile (640px JPEG), and sends structured JSON prompts to `gemini-3.1-flash-lite` or `gemini-3.5-flash-lite`. Returns estimated ingredients, weights in grams, macros, and potential seed oils.
- **AI Ingredient Swapper:** Allows swapping or correcting individual ingredients estimated by the AI using the local staples database or barcode scans.
- **Barcode Scanner:** Scans product barcodes using Google ML Kit on-device scanning. Retrieves nutrition information and per-serving measurements from Open Food Facts.
- **Search & Staples Database:** Includes an offline reference database of UK and Irish whole-food staples (derived from official CoFID / McCance and Widdowson data) with token stemming for common culinary terms, combined with remote Open Food Facts search.
- **Seed Oil Detection:** Scans ingredient lists using case-insensitive regex for nine industrial seed oils: rapeseed/canola, sunflower, soybean, cottonseed, corn oil, safflower, grapeseed, and generic vegetable oil. Categorizes exposure into a 3-tier traffic light system:
  - Green (Low / Clean): 0g or trace amounts (<= 2.5g or <= 2.5% ratio).
  - Amber (Moderate): Moderate cooking oil (2.5g to 10.0g or 2.5% to 10.0% ratio).
  - Red (High): Industrial or deep-frying oil (> 10.0g or > 10.0% ratio).
- **UPF Tracking:** Categorizes foods by NOVA classification (Groups 1 through 4) and tracks daily percentage of calories and total grams derived from Group 4 ultra-processed foods.
- **TDEE & Weight Projections:** Calculates basal metabolic rate (Mifflin-St Jeor formula) and total daily energy expenditure. Pulls weight, height, and active calorie burn from Android Health Connect. Uses Wishnofsky's rule (7,700 kcal deficit = 1 kg body mass) to display 4-week weight change projections.
- **Recipes & Portions:** Supports saving multi-ingredient recipes with fractional portion logging (e.g. logging 1/4 or 1/5 of a family dish).
- **End-of-Day Review:** Supports two review modes: instantaneous on-device calculation (runs locally in 0ms without network access) or a Gemini AI evaluation summarizing daily macro balance, food quality, and areas for improvement.
- **Home Screen Widget & Quick Tile:** Android Glance home screen widget displaying live calorie balance and macro progress bars, plus a Quick Settings notification shade tile to jump directly into the barcode scanner.

---

## Installation

### Sideloading the Release APK

1. Download `MacroAI-v1.0.0-release.apk` from the repository root or the GitHub Releases page.
2. Install via ADB:
   ```bash
   adb install -r MacroAI-v1.0.0-release.apk
   ```
   Or transfer the `.apk` file to your Android device and open it in any file manager (requires allowing installation from unknown sources).
3. Launch the app and grant Camera and Health Connect permissions when prompted.

---

## Building from Source

### Requirements
- Android SDK with API 34 (Android 14) platform and build-tools installed.
- JDK 17 (configured via `JAVA_HOME` or `org.gradle.java.home` in `gradle.properties`).

### Build Commands

```powershell
# Run unit tests
.\gradlew.bat testReleaseUnitTest

# Build release APK
.\gradlew.bat assembleRelease
```

The output APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

---

## Tech Stack & Architecture

- **Architecture:** MVVM / Clean Architecture with unidirectional data flow (UDF)
- **UI:** Jetpack Compose, Material 3, Navigation Compose, Android Glance AppWidget
- **Database:** Room 2.6 (SQLite), Preferences DataStore
- **Dependency Injection:** Hilt
- **Network:** Ktor Client 2.3 with OkHttp engine and `kotlinx.serialization`
- **Vision & Scanning:** CameraX and Google ML Kit Barcode Scanning
- **Health:** Android Health Connect API
- **Background Tasks:** Android WorkManager

---

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

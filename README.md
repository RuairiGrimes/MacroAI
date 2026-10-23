<div align="center">

# 🥗 MacroAI

### **Personal AI Nutrition & Ultra-Processed Food (UPF) Tracker for Android**

*100% On-Device • Zero Subscription Paywalls • Privacy-First • Powered by Google Gemini AI*

---

[![Android Platform](https://img.shields.io/badge/Platform-Android%2014%2B%20(API%2034)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Version](https://img.shields.io/badge/Release-v1.0.0-10B981?style=for-the-badge)](https://github.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Gemini AI](https://img.shields.io/badge/Google%20Gemini-Flash%203.1%20%26%203.5%20Lite-FF7043?style=for-the-badge&logo=google&logoColor=white)](https://aistudio.google.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](LICENSE)

</div>

---

## 💡 Why MacroAI?

Most mainstream calorie tracking apps are cluttered with **\$80/year subscription paywalls**, sluggish barcode scanners, aggressive advertisements, and opaque algorithms. Crucially, they treat all calories equally—ignoring the physiological impact of **industrial seed oils**, **ultra-processed foods (UPF)**, and **fatty acid profiles**.

**MacroAI** was built from scratch to fix this. It is a native, modern, edge-to-edge Android application that combines cutting-edge **multimodal AI vision** with clinical metabolic intelligence—giving you full control over your nutrition without subscriptions or middleman servers.

---

## ✨ Key Features

### 📷 1. Multimodal AI Meal Vision
- **Photo to Macros in Seconds:** Snap a photo of any home-cooked plate, restaurant dish, or takeaway. Powered by **Google Gemini Flash 3.1 & 3.5 Lite**, MacroAI segments individual components, estimates weights in grams, extracts macros, and identifies potential hidden seed oils.
- **Context Prompting & 1-Tap Chips:** Add quick context (e.g., *"+ cooked in 1 tbsp olive oil"*, *"+ ~200g portion"*).
- **Interactive AI Ingredient Swapper:** Swap any AI-estimated ingredient with verified items from the curated database or packet barcode scans with live recalculation.

### 🔍 2. Precision Food Search & Barcode Scanner
- **Google ML Kit Barcode Scanner:** Instant packet scanning right from your camera with automatic manufacturer serving sizes.
- **Curated UK & Irish Offline Staples (CoFID / Nutracheck Standard):** Curated USDA/CoFID clinical reference dataset for fresh butcher cuts, dairy (Kerrygold, Greek yogurt), grains, and produce with intelligent token stemming.
- **Open Food Facts Integration:** Millions of global and UK/Irish supermarket products searchable with lenient JSON parsing.

### 🚦 3. 3-Tier Seed Oil Traffic Light System
Industrial seed oils (rapeseed, canola, sunflower, soybean, cottonseed, corn, safflower, grapeseed, vegetable oil) are detected using case-insensitive word-boundary analysis and classified into a clinical 3-tier traffic light system:
- 🟢 **Green (Clean / Low):** Seed-oil free (0g) or trace amounts ($\le 2.5\text{g}$ or $\le 2.5\%$).
- 🟡 **Amber (Moderate):** Moderate cooking oil ($2.5\text{g} - 10.0\text{g}$ or $2.5\% - 10.0\%$).
- 🔴 **Red (High):** Heavy industrial or oxidised deep-frying oil ($> 10.0\text{g}$ or commercial deep-fried items).

### 🏭 4. Ultra-Processed Food (NOVA 4) Tracking
- Tracks daily percentage of calories and total gram weight derived from **NOVA Group 4** ultra-processed foods.
- Real-time bento alerts on the dashboard keep your whole-food ratio front and center.

### 📈 5. Dynamic TDEE & 4-Week Weight Projections
- **Mifflin-St Jeor Engine:** Calculates precise BMR from biological biometrics and activity levels.
- **Android Health Connect Integration:** Automatically synchronizes weight, height, and live active energy burned from your smartwatch or fitness tracker.
- **Wishnofsky Calorie Deficit Modeling:** Applies Wishnofsky's clinical formula ($7,700\text{ kcal} = 1\text{ kg}$ body mass change over 28 days) to calculate:
  > *"If you keep up with this, you'll weigh X.X kg in 4 weeks"*
- **Dual Fitness Units:** Displays both metric ($kg$) and imperial ($lbs$), with strength athlete protein targets ($g/lb$ and $g/kg$).

### 📊 6. Automated Google Sheets Cloud Backup
- Every food log, lipid breakdown (saturated fat, PUFA, MUFA), and 7 micronutrients (sodium, potassium, iron, calcium, vitamin C, vitamin D, magnesium) are automatically archived to your personal Google Sheet in the background using Android WorkManager.
- Works offline—logs are queued and sync automatically upon network reconnection.

### 📱 7. Home Screen Widget & Quick Scan Tile
- **Android Glance Widget:** Live remaining calories, protein/carb/fat progress bars, and 1-tap scan shortcuts directly on your Android home screen.
- **Quick Settings Tile:** Add the MacroAI Quick Scan tile to your notification shade to launch the camera instantly from anywhere.

### 🧑‍🍳 8. Repeatable Recipes Studio
- Build batch recipes using AI prompt descriptions or ingredient searches.
- Log fractional portions (e.g. $1/4$th or $1/5$th for family dinners) with mathematical nutrient scaling.

### 🌙 9. End-of-Day Metabolic Review
- Choose between **Instant On-Device Backend Stats** (0ms, works completely offline) or **Gemini AI Daily Evaluation** scoring your day across Health & UPF, Protein Adequacy, and Calorie Control.

---

## 🚀 Quick Start & Installation

### Option A: Install Pre-Built APK (Recommended)
1. Download `MacroAI-v1.0.0-release.apk` from the latest [GitHub Releases](https://github.com) or project root.
2. Sideload the APK onto your Android phone via USB/ADB or transfer the file directly to your device:
   ```bash
   adb install -r MacroAI-v1.0.0-release.apk
   ```
3. Open **MacroAI** and grant Camera & Health Connect permissions when prompted.

---

## ⚙️ 2-Minute Setup: API Key & Google Sheets

MacroAI allows you to configure your personal Gemini API key and Google Sheets backup directly inside the app:

1. **Free Gemini API Key (60 Seconds):**
   - Open [Google AI Studio](https://aistudio.google.com/app/apikey) and click **"Create API key"**.
   - In MacroAI, go to **Goals / Settings** → **4. Cloud & AI Integration**.
   - Paste your key into the **Gemini AI API Key** field and tap **Test Key**.

2. **Personal Google Sheets Backup (2 Minutes):**
   - Create a new Google Sheet at [sheets.new](https://sheets.new).
   - Click **Extensions** → **Apps Script**, delete the contents, and paste [`scripts/google_apps_script_webhook.js`](scripts/google_apps_script_webhook.js).
   - Click **Deploy** → **New deployment** → Type: **Web app** → Access: **Anyone** → **Deploy**.
   - Copy the Web App URL (`.../exec`) and paste it into MacroAI's **Google Sheets Webhook URL** field, then tap **Test Webhook**.

📖 *For detailed step-by-step screenshots and tips, see the complete [Setup Guide](docs/SETUP_GUIDE.md).*

---

## 🔒 Privacy & Architecture Philosophy

- **Zero Middleman Servers:** MacroAI has no proprietary backend server, database, or analytics tracking. 
- **100% Data Ownership:** Your nutrition logs reside locally in Room SQLite on your device and sync only to your personal Google Sheet.
- **Direct Encrypted API Calls:** Gemini multimodal requests are transmitted directly over HTTPS to Google's official endpoints using your own API key.
- **Offline First:** All core tracking, barcode scanning, staples search, and metabolic deficit math function completely without internet.

---

## 🛠️ Tech Stack & Architecture

- **Architecture:** MVVM / Clean Architecture with Unidirectional Data Flow (UDF)
- **UI:** Jetpack Compose, Material 3, Type-Safe Navigation Compose, Android Glance AppWidget
- **Language & Toolchain:** Kotlin 2.0.21, KSP, Gradle Version Catalog (`libs.versions.toml`)
- **Dependency Injection:** Hilt (`dagger.hilt.android`)
- **Local Persistence:** Room Database 2.6, Preferences DataStore
- **Networking:** Ktor Client 2.3 (`okhttp` engine) + `kotlinx.serialization`
- **Computer Vision:** CameraX + Google ML Kit Barcode Scanning
- **Health:** Android Health Connect (`androidx.health.connect:connect-client`)
- **Background Tasks:** Android WorkManager (`androidx.work:work-runtime-ktx`)

---

## 🏗️ Building from Source

### Prerequisites:
- JDK 17 (Eclipse Temurin or OpenJDK)
- Android SDK (API 34 / Android 14)

### Build Commands:
```powershell
# Clone the repository
git clone https://github.com/<your-username>/MacroAI.git
cd MacroAI

# Run release unit tests
.\gradlew.bat testReleaseUnitTest

# Assemble release APK
.\gradlew.bat assembleRelease
```
The compiled APK will be located at `app/build/outputs/apk/release/app-release.apk`.

---

## 🤝 Contributing

Contributions, bug reports, and feature suggestions are welcome! Feel free to open an issue or submit a pull request.

---

## 📄 License

MacroAI is open-source software licensed under the **Apache License 2.0**.
See [LICENSE](LICENSE) for details.


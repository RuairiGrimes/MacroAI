# MacroAI — Project Work Log & Architecture Reference

> **Purpose:** This file serves as a persistent engineering work log and handoff reference for future chat sessions working on **MacroAI** (`c:\MyProjects\MacroAI`).

---

## 1. Environment & Build Configuration

- **Target OS:** Native Android 14+ (`minSdk = 34`, `targetSdk = 34`, `compileSdk = 34`)
- **Language & Build:** Kotlin 2.0.21, AGP 8.7.3, KSP 2.0.21-1.0.28, Hilt 2.52, Room 2.6.1, Ktor 2.3.12 (`okhttp`), Jetpack Compose (Material 3)
- **Local Toolchain Paths:**
  - **JDK 17:** `C:\AndroidDev\jdk-17` (configured in `gradle.properties` via `org.gradle.java.home=C:/AndroidDev/jdk-17` and `gradlew.bat`)
  - **Android SDK:** `C:\AndroidDev\android-sdk` (configured in `local.properties`)
- **Secrets (`local.properties` -> `BuildConfig`):**
  - `GEMINI_API_KEY`: Injected into `BuildConfig.GEMINI_API_KEY`
  - `SHEETS_WEBHOOK_URL`: Injected into `BuildConfig.SHEETS_WEBHOOK_URL`
- **Release Signing & APK Output:**
  - Keystore: `c:\MyProjects\MacroAI\keystore\macroai-release.jks` (Alias: `macroai`)
  - Standard Build & Copy Command:
    ```powershell
    .\gradlew.bat assembleRelease testReleaseUnitTest --stacktrace; if ($LASTEXITCODE -eq 0) { Copy-Item -Force "c:\MyProjects\MacroAI\app\build\outputs\apk\release\app-release.apk" "c:\MyProjects\MacroAI\MacroAI-v1.0.0-release.apk" }
    ```
  - Sideload APK Path: `c:\MyProjects\MacroAI\MacroAI-v1.0.0-release.apk`

---

## 2. Architecture & Key Source Files

### Data Layer (`com.macroai.tracker.data`)
- **Room Database:**
  - `data/local/entity/FoodLogEntity.kt`: Primary table storing macro/micronutrients, lipid profile (`saturatedFatG`, `polyunsaturatedFatG`, `monounsaturatedFatG`), UPF classification (`novaGroup` 1–4, `isUpf`), seed oil flags (`containsSeedOils`, `detectedSeedOils`), `aiConfidenceReasoning`, and `isSyncedToSheets`.
  - `data/local/dao/FoodLogDao.kt`: Reactive `Flow<List<FoodLogEntity>>` queries by date range, unsynced row queries (`getUnsyncedLogs()`), and sync-marking updates.
  - `data/local/MacroAiDatabase.kt`: Room database definition.
- **Preferences DataStore:**
  - `data/local/preferences/UserPreferencesDataStore.kt`: Persists `UserProfile` (`TdeeMode`, weight/height/age/sex/activity level, manual TDEE override, and custom calorie + macro target settings in `MacroInputMode.GRAMS` or `MacroInputMode.PERCENTAGE`).
- **Gemini Multimodal AI Engine:**
  - `data/remote/gemini/GeminiMealAnalyzer.kt`: Compresses meal photos on a background thread (`640px` longest edge @ `68%` JPEG quality to fit in a single 258-token vision tile), enforces structured JSON output (`GeminiMealResponseSchema.jsonSchema`), sets `thinkingLevel = "low"` and `maxOutputTokens = 4096`, routes strictly through `gemini-3.1-flash-lite` with no fallback models for high rate limits, and includes `repairTruncatedMealJson` recovery.
  - `data/remote/gemini/dto/GeminiMealSchemaDto.kt`: Request/response DTOs, `GeminiMealAnalysisDto`, `GeminiIngredientEstimateDto`, and JSON Schema definition.
- **Open Food Facts Barcode Engine:**
  - `data/remote/off/OpenFoodFactsApi.kt` & `dto/OffProductResponseDto.kt`: Queries `GET https://world.openfoodfacts.org/api/v2/product/{barcode}.json` with custom `User-Agent: PersonalCalorieTracker/1.0 (Android; PersonalUse)`, extracts per-100g macros/micros/NOVA group, and runs `SeedOilDetector`.
- **Health Connect Integration:**
  - `data/health/HealthConnectManager.kt`: Manages Health Connect permissions (`READ_WEIGHT`, `READ_HEIGHT`, `READ_ACTIVE_CALORIES_BURNED`, `READ_NUTRITION`, `WRITE_NUTRITION`), reads latest biometrics/active energy for dynamic TDEE, and automatically writes/deletes `NutritionRecord` entries (`macroai_<uuid>`) when meals are logged or deleted.
- **Google Sheets Sync (WorkManager):**
  - `data/sync/SheetsSyncWorker.kt` & `SheetsWebhookClient.kt`: Background `CoroutineWorker` with `NetworkType.CONNECTED` constraint that syncs `isSyncedToSheets == false` entries to `BuildConfig.SHEETS_WEBHOOK_URL`.
  - `scripts/google_apps_script_webhook.js`: Standalone Google Apps Script `doPost(e)` webhook bound to Spreadsheet ID `1ZmpQpp8EYVAvbim7Ay0374nkRmK5wAJg8lxN3-IFl_4`.

### Domain Layer (`com.macroai.tracker.domain`)
- `domain/util/SeedOilDetector.kt`: Case-insensitive word-boundary regex detector for industrial seed oils (`rapeseed`, `canola`, `sunflower`, `soybean`, `cottonseed`, `corn oil`, `safflower`, `grapeseed`, `vegetable oil`).
- `domain/util/TdeeCalculator.kt`: Mifflin-St Jeor BMR & TDEE engine supporting 4 modes (`CUSTOM_GOALS`, `FORMULA_MULTIPLIER`, `HEALTH_CONNECT_DYNAMIC`, `MANUAL_OVERRIDE`) and custom macro target calculation (`GRAMS` or `PERCENTAGE`).
- `domain/model/`: `TdeeMode.kt`, `UserProfile.kt`, `MealType.kt`, `LogSource.kt`.

### UI Layer (`com.macroai.tracker.ui`)
- `ui/theme/`: Obsidian Emerald dark/light Material 3 theme (`Color.kt`, `Theme.kt`).
- `ui/dashboard/`: `DashboardScreen.kt` & `DashboardViewModel.kt` — Animated calorie hero arc gauge, macro rings, UPF % and Seed Oil bento cards, expandable Lipid (PUFA/MUFA/SatFat) & 7-micronutrient card, and meal log timeline.
- `ui/camera/`:
  - `UnifiedCameraScreen.kt`: CameraX preview + ML Kit barcode scanner. Automatically unbinds CameraX (`unbindAll()`) and transitions the entire screen via `AnimatedContent` as soon as a photo is captured or barcode is scanned.
  - `MealReviewBottomSheet.kt`: Full-screen `MealAnalysisAndReviewScreen` and `FullScreenAnalyzingView` (animated orbital progress ring around the captured meal photo, context prompt editor, and interactive ingredient gram scaling).
  - `CameraCaptureViewModel.kt`: Background bitmap pre-encoding on photo capture and draft state management.
- `ui/manual/`: `ManualEntryScreen.kt` & `ManualEntryViewModel.kt` — Quick manual food entry with seed-oil ingredient scanner.
- `ui/settings/`: `TdeeSettingsScreen.kt` & `TdeeSettingsViewModel.kt` — Custom Calorie & Macro Target selector (`GRAMS` vs `PERCENTAGE`), TDEE mode switcher, Health Connect permission/sync status, and Google Sheets sync trigger.

---

## 3. Chronological Work Log

1. **Milestones 1–5 Initial Implementation:**
   - Built Room database, Preferences DataStore, Hilt DI modules, Open Food Facts barcode pipeline, `SeedOilDetector`, `GeminiMealAnalyzer`, CameraX + ML Kit unified capture flow, Health Connect reader + TDEE calculator, Compose Dashboard, and WorkManager Google Sheets sync.
2. **Release Signing & Non-Debug Build:**
   - Created `keystore/macroai-release.jks`, configured release signing in `app/build.gradle.kts`, and produced standalone release APK (`MacroAI-v1.0.0-release.apk`).
3. **UI Overhaul, Custom Calorie/Macro Selector & Health Connect Nutrition Write:**
   - Updated launcher icon to custom vector emblem (`ic_launcher_foreground.xml`, `ic_launcher_background.xml`).
   - Redesigned UI with Obsidian Emerald palette, animated canvas gauges, and Bento cards.
   - Added `WRITE_NUTRITION` permission and `NutritionRecord` automatic write/delete in `HealthConnectManager.kt` and `FoodLogRepository.kt`.
   - Added `TdeeMode.CUSTOM_GOALS` and custom calorie + macro target builder (`GRAMS` and `PERCENTAGE` modes) in `TdeeSettingsScreen.kt`.
4. **Gemini Performance & Latency Optimization:**
   - Added background image pre-compression in `CameraCaptureViewModel.onPhotoCaptured` while the user types their context prompt.
   - Configured `thinkingBudget = 0` and single-tile image downscaling (`640px` @ `68%` JPEG quality) to minimize upload size and time-to-first-token.
5. **Camera Lifecycle & Non-Wrapping Metric Value Fixes:**
   - Replaced the camera modal bottom sheet with full-screen `AnimatedContent` in `UnifiedCameraScreen.kt` so CameraX immediately calls `unbindAll()` and stops recording when a photo is captured or analyzed.
   - Added `maxLines = 1, softWrap = false` on metric badges/values and `Modifier.weight(1f)` on labels across `DashboardScreen.kt`, `TdeeSettingsScreen.kt`, and `MealReviewBottomSheet.kt`.
6. **JSON Truncation & Socket Timeout Fixes (Latest):**
   - Raised `maxOutputTokens` from `750` to `4096` in `GeminiMealAnalyzer.kt` and `GeminiMealSchemaDto.kt` to prevent mid-JSON `EOF` truncation on 5-item meals.
   - Added `repairTruncatedMealJson` in `GeminiMealAnalyzer.kt` to salvage truncated `ingredientBreakdown` arrays if stream cutoff ever occurs.
   - Increased Ktor `HttpTimeout` and OkHttp `readTimeout`/`callTimeout` to `180s` (`writeTimeout = 120s`) with `retryOnConnectionFailure(true)` in `NetworkModule.kt`.
   - Standardized camelCase `generationConfig` / `thinkingConfig` serialization in `GeminiMealSchemaDto.kt` and added automatic timeout/error fallback across `gemini-3.8-flash` -> `gemini-2.5-flash` -> `gemini-2.5-flash-lite`.
7. **Open Food Facts Database Search & Serving Size Selector (Cronometer / MyFitnessPal Style):**
   - Added `searchFoods` to `OpenFoodFactsApi.kt` using Open Food Facts' Elasticsearch Search-a-licious API (`https://search.openfoodfacts.org/search`) with a compliant user agent (`MacroAI/1.0.0 (Android; PersonalUse; contact: macroai-support@example.com)`).
   - Added lenient custom serializers (`OffBrandsSerializer`, `LenientDoubleSerializer`, `LenientIntSerializer`) and `OffSearchResponseDto` in `OffProductDto.kt` to robustly handle diverse Open Food Facts data shapes without crashes.
   - Created `ServingOption` and `ServingUnitType` (`GRAMS`, `OUNCES`, `SERVING`, `HUNDRED_GRAMS`) with live mathematical portion scaling in `domain/model/ServingOption.kt`.
   - Added `LogSource.SEARCH` to `LogSource.kt`.
   - Built `FoodSearchViewModel.kt` featuring 450ms query debounce, popular staples suggestions ("Chicken breast", "Eggs", "Oats", etc.), live nutrient recalculation, and Room/Health Connect/Sheets logging.
   - Created `FoodSearchScreen.kt` with instant search bar, empty state suggestions, search result cards with NOVA and seed-oil badges, and a full Cronometer/MyFitnessPal-style portion customizer with preset chips, steppers, live macro cards, secondary lipid breakdown, and micronutrients.
   - Wired `FoodSearchRoute` into `AppNavigation.kt`, updated `DashboardScreen.kt` floating action dock with a dedicated "Search" button, added `+` quick-add buttons to each meal group header, and updated `EmptyDayCard`.
   - Added comprehensive unit tests in `NutritionEnginesTest.kt` for food search parsing and portion scaling. Built standalone release APK (`MacroAI-v1.0.0-release.apk`).
8. **Logged Meal Editing & Repeatable Recipes / Meals (Goals 1 & 2):**
   - **Database Migration (v1 -> v2):** Created `SavedMealEntity.kt`, added `MIGRATION_1_2` in `DatabaseModule.kt` (adding `ingredientBreakdownJson`, `recipeId`, `servingsConsumed` to `food_logs`, and creating `saved_meals` table), and created `SavedMealDao.kt`.
   - **Domain Models & Converters:** Created `MealIngredient.kt` with `withWeight()`, `scaleFraction()`, and converters for AI DTOs and Open Food Facts products. Added `withUpdatedIngredients()`, `withScaledWeight()`, and `toSavedMealEntity()` to `FoodLogEntity.kt`.
   - **Gemini Text Prompt Recipe Parser:** Added `analyzeTextPrompt()` in `GeminiMealAnalyzer.kt` and `parseRecipePrompt()` in `NutritionRepository.kt` for structured AI recipe creation without needing a camera photo.
   - **Goal 1 - Edit Logged Meals (`EditFoodLogScreen.kt` & `EditFoodLogViewModel.kt`):**
     - Tapping any food card on the dashboard or clicking its edit icon opens the full editor.
     - For multi-component meals (AI photo scans, recipes): displays each ingredient with name, macros, and `[-]`/`[+]` gram steppers or direct text entry. Live recalculates all meal macros, PUFA, saturated fat, fibre, NOVA group, and seed oils as ingredients change.
     - For single foods: provides gram scaling slider/steppers plus direct macro adjustments.
     - Allows renaming meal, changing meal slot (Breakfast, Lunch, Dinner, Snack), changing NOVA group, and toggling seed oil detection.
     - "Save as Recipe" action directly converts the logged meal into a reusable recipe.
     - "Save Changes" persists to Room, updates Health Connect `NutritionRecord`, and marks for Google Sheets sync.
     - "Delete Meal" with confirmation dialog cleanses from Room and Health Connect.
   - **Goal 2 - Repeatable "Meals & Recipes" (`SavedMealsScreen.kt` & `SavedMealsViewModel.kt`):**
     - Dedicated screen accessed via the "Recipes" button in the Dashboard top bar.
     - Displays saved recipes with total servings, total weight, per-serving macros, NOVA/seed oil badges, and ingredient count.
     - "Log Portion" action: opens bottom sheet allowing 1 portion (e.g. 1/5th for family meals), fractional portions (0.5, 1.5, 2), or custom portions with live preview of exact portion calories, protein, carbs, and fat before logging directly into today's journal.
     - "Create Recipe" flow with two modes:
       - **AI Prompt Builder:** User describes the recipe/ingredients (e.g. "Family Bolognese with 500g lean minced beef... Makes 4 servings") and Gemini generates the complete breakdown.
       - **Search & Manual Builder:** Set name and servings, search the Open Food Facts database to add ingredients with custom grams, or add manual ingredients.
   - **Verification & Testing:**
     - Added unit tests in `NutritionEnginesTest.kt` verifying `SavedMealEntity.fromIngredients`, `toFoodLogEntity` fraction scaling, `FoodLogEntity.withUpdatedIngredients`, and `withScaledWeight`.
     - Ran `.\gradlew.bat testReleaseUnitTest` (all 11 unit tests passed).
     - Built signed release APK: `MacroAI-v1.0.0-release.apk` (77.6MB).

9. **Recipe Search Relevance Overhaul & Packet Barcode Scanning with Serving Selection:**
   - **Pure Staples Offline Database (`data/local/StapleIngredientsDatabase.kt`):**
     - Created curated offline database of verified home cooking staples (USDA/clinical reference values per 100g) covering:
       - Lean meats & poultry: Minced Beef 5% Fat, 10% Fat, 15%, 20%, Chicken Breast (raw & cooked), Chicken Thighs, Turkey Mince, Sirloin Steak.
       - Seafood & eggs: Salmon Fillets, Cod Fillets, Canned Tuna in Spring Water, Whole Eggs, Liquid Egg Whites.
       - Grains & starches: Basmati Rice, Jasmine Rice, Brown Rice, Durum Wheat Pasta/Spaghetti, Rolled Porridge Oats, White Potatoes, Sweet Potatoes.
       - Produce & pantry staples: Canned Chopped Tomatoes, Tomato Passata, Tomato Puree, Brown Onions, Garlic, Broccoli, Baby Spinach, Bell Peppers, White Mushrooms.
       - Pure fats & oils: Extra Virgin Olive Oil, Pure Salted Butter, Extra Virgin Coconut Oil, Pure Rapeseed Oil, Sunflower Oil.
       - Dairy: Greek Yogurt 0% Fat, Cheddar Cheese, Parmesan Cheese.
     - Implemented token-based relevance scoring with exact query phrase boosting so searches like `"5% fat minced beef"` instantly return the exact pure staple at rank 1.
   - **Search Relevance & Processed Food Filtering (`OpenFoodFactsApi.kt` & `NutritionRepository.kt`):**
     - Filtered out Open Food Facts items with zero macros/empty records.
     - Implemented ranking boosting whole/minimally processed ingredients (NOVA 1–3) over ultra-processed items (NOVA 4) for cooking recipes.
     - Prepended pure staples front-and-center ahead of remote search results with deduplication.
   - **Packet Barcode Scanner for Recipe Ingredients (`ui/camera/BarcodeScannerDialog.kt`):**
     - Embedded CameraX + Google ML Kit `BarcodeScanning` viewfinder dialog directly into the recipe builder sheet.
     - Enables instant barcode detection when adding ingredients from packets, with fallback manual barcode entry option.
   - **Cronometer / MyFitnessPal-Style Portion Selector (`ui/recipes/IngredientPortionSelectorDialog.kt`):**
     - Dialog with interactive serving unit selector: `Grams (g)`, `Ounces (oz)`, `Package Serving (e.g. 1 can 400g / 1 portion 150g)`, and `100g Portion`.
     - Live macro, lipid (PUFA/MUFA/SatFat), and micronutrient recalculation as the user adjusts quantities or units.
     - Automatically creates scaled `MealIngredient` and adds to the recipe.
   - **Testing & Release Verification:**
     - Added comprehensive unit tests in `NutritionEnginesTest.kt` verifying `StapleIngredientsDatabase.search("5% fat minced beef")`, chicken breast search, olive oil fatty acid verification, and serving option calculations.
     - Executed `.\gradlew.bat testReleaseUnitTest` (15/15 tests passed).
     - Assembled signed release build via `assembleRelease` and refreshed root `MacroAI-v1.0.0-release.apk` (77.7MB).
9. **Recipe Search Relevance Overhaul & Packet Barcode Scanning with Serving Selection:**
   - **Pure Staples Offline Database (`data/local/StapleIngredientsDatabase.kt`):**
     - Created curated offline database of verified home cooking staples (USDA/clinical reference values per 100g) covering:
       - Lean meats & poultry: Minced Beef 5% Fat, 10% Fat, 15%, 20%, Chicken Breast (raw & cooked), Chicken Thighs, Turkey Mince, Sirloin Steak.
       - Seafood & eggs: Salmon Fillets, Cod Fillets, Canned Tuna in Spring Water, Whole Eggs, Liquid Egg Whites.
       - Grains & starches: Basmati Rice, Jasmine Rice, Brown Rice, Durum Wheat Pasta/Spaghetti, Rolled Porridge Oats, White Potatoes, Sweet Potatoes.
       - Produce & pantry staples: Canned Chopped Tomatoes, Tomato Passata, Tomato Puree, Brown Onions, Garlic, Broccoli, Baby Spinach, Bell Peppers, White Mushrooms.
       - Pure fats & oils: Extra Virgin Olive Oil, Pure Salted Butter, Extra Virgin Coconut Oil, Pure Rapeseed Oil, Sunflower Oil.
       - Dairy: Greek Yogurt 0% Fat, Cheddar Cheese, Parmesan Cheese.
     - Implemented token-based relevance scoring with exact query phrase boosting so searches like `"5% fat minced beef"` instantly return the exact pure staple at rank 1.
   - **Search Relevance & Processed Food Filtering (`OpenFoodFactsApi.kt` & `NutritionRepository.kt`):**
     - Filtered out Open Food Facts items with zero macros/empty records.
     - Implemented ranking boosting whole/minimally processed ingredients (NOVA 1–3) over ultra-processed items (NOVA 4) for cooking recipes.
     - Prepended pure staples front-and-center ahead of remote search results with deduplication.
   - **Packet Barcode Scanner for Recipe Ingredients (`ui/camera/BarcodeScannerDialog.kt`):**
     - Embedded CameraX + Google ML Kit `BarcodeScanning` viewfinder dialog directly into the recipe builder sheet.
     - Enables instant barcode detection when adding ingredients from packets, with fallback manual barcode entry option.
   - **Cronometer / MyFitnessPal-Style Portion Selector (`ui/recipes/IngredientPortionSelectorDialog.kt`):**
     - Dialog with interactive serving unit selector: `Grams (g)`, `Ounces (oz)`, `Package Serving (e.g. 1 can 400g / 1 portion 150g)`, and `100g Portion`.
     - Live macro, lipid (PUFA/MUFA/SatFat), and micronutrient recalculation as the user adjusts quantities or units.
     - Automatically creates scaled `MealIngredient` and adds to the recipe.
   - **Testing & Release Verification:**
     - Added comprehensive unit tests in `NutritionEnginesTest.kt` verifying `StapleIngredientsDatabase.search("5% fat minced beef")`, chicken breast search, olive oil fatty acid verification, and serving option calculations.
     - Executed `.\gradlew.bat testReleaseUnitTest` (15/15 tests passed).
     - Assembled signed release build via `assembleRelease` and refreshed root `MacroAI-v1.0.0-release.apk` (77.7MB).


10. **Keyboard-Adaptive AI Food Prompt & Window Inset Overhaul:**
    - **Root Cause Analysis:**
      - In Android 14+ with edge-to-edge enabled (`enableEdgeToEdge()`), `windowSoftInputMode="adjustResize"` does not automatically resize Compose root views without explicit `Modifier.imePadding()` or `WindowInsets.ime` management.
      - On the AI Meal Photo prompt (`MealReviewBottomSheet.kt`), the 220dp preview image and headers pushed the context text field down. When the software keyboard appeared, the text box was completely occluded behind the keyboard, and Compose's scroll state did not recognize the keyboard height, disabling vertical scrolling.
    - **Adaptive Compact Photo Hero (`MealReviewBottomSheet.kt`):**
      - Detected software keyboard visibility via `WindowInsets.isImeVisible`.
      - When keyboard is closed: displays the full 200dp photo preview with rounded corners and emerald border.
      - When keyboard opens: smoothly animates via `AnimatedContent` into a sleek 54dp compact card (`Photo Attached` badge with thumbnail and tap-to-dismiss helper). This reclaims 150+ dp of vertical screen estate.
    - **BringIntoViewRequester & Auto-Scroll:**
      - Integrated `BringIntoViewRequester` with `LaunchedEffect(isImeVisible)` and `onFocusEvent` to automatically scroll the prompt text box and action buttons into clear view above the software keyboard.
    - **Visual Polish & 1-Tap Quick Context Chips:**
      - Added one-tap suggestion chips for rapid context entry (`+ 1 tbsp Olive Oil`, `+ Cooked in Butter`, `+ ~150g Portion`, `+ ~250g Portion`, `+ No Seed Oils`) that immediately append to the prompt.
      - Added leading Gemini AI sparkle icon, clear button (`Icons.Default.Clear`) trailing icon, and `ImeAction.Done` keyboard actions with `LocalSoftwareKeyboardController.current?.hide()`.
      - Ensured action buttons ("Retake Photo" and "Analyze Meal") stay visible and clickable above the keyboard.
    - **App-Wide Keyboard Inset Protection:**
      - Added `Modifier.imePadding()` to `SavedMealsScreen.kt` ("AI Recipe Prompt"), `FoodSearchScreen.kt` (Food Search & Portion Selector), and `EditFoodLogScreen.kt`.
    - **Verification & Release:**
      - Verified unit test suite via `.\gradlew.bat testReleaseUnitTest` (15/15 passed).
      - Assembled signed release APK (`MacroAI-v1.0.0-release.apk`, 77.8MB).

10. **Keyboard-Adaptive AI Food Prompt & Window Inset Overhaul:**
    - **Root Cause Analysis:**
      - In Android 14+ with edge-to-edge enabled (`enableEdgeToEdge()`), `windowSoftInputMode="adjustResize"` does not automatically resize Compose root views without explicit `Modifier.imePadding()` or `WindowInsets.ime` management.
      - On the AI Meal Photo prompt (`MealReviewBottomSheet.kt`), the 220dp preview image and headers pushed the context text field down. When the software keyboard appeared, the text box was completely occluded behind the keyboard, and Compose's scroll state did not recognize the keyboard height, disabling vertical scrolling.
    - **Adaptive Compact Photo Hero (`MealReviewBottomSheet.kt`):**
      - Detected software keyboard visibility via `WindowInsets.isImeVisible`.
      - When keyboard is closed: displays the full 200dp photo preview with rounded corners and emerald border.
      - When keyboard opens: smoothly animates via `AnimatedContent` into a sleek 54dp compact card (`Photo Attached` badge with thumbnail and tap-to-dismiss helper). This reclaims 150+ dp of vertical screen estate.
    - **BringIntoViewRequester & Auto-Scroll:**
      - Integrated `BringIntoViewRequester` with `LaunchedEffect(isImeVisible)` and `onFocusEvent` to automatically scroll the prompt text box and action buttons into clear view above the software keyboard.
    - **Visual Polish & 1-Tap Quick Context Chips:**
      - Added one-tap suggestion chips for rapid context entry (`+ 1 tbsp Olive Oil`, `+ Cooked in Butter`, `+ ~150g Portion`, `+ ~250g Portion`, `+ No Seed Oils`) that immediately append to the prompt.
      - Added leading Gemini AI sparkle icon, clear button (`Icons.Default.Clear`) trailing icon, and `ImeAction.Done` keyboard actions with `LocalSoftwareKeyboardController.current?.hide()`.
      - Ensured action buttons ("Retake Photo" and "Analyze Meal") stay visible and clickable above the keyboard.
    - **App-Wide Keyboard Inset Protection:**
      - Added `Modifier.imePadding()` to `SavedMealsScreen.kt` ("AI Recipe Prompt"), `FoodSearchScreen.kt` (Food Search & Portion Selector), and `EditFoodLogScreen.kt`.
    - **Verification & Release:**
      - Verified unit test suite via `.\gradlew.bat testReleaseUnitTest` (15/15 passed).
      - Assembled signed release APK (`MacroAI-v1.0.0-release.apk`, 77.8MB).

11. **UK & Irish Audience Curation (CoFID / Nutracheck Alignment & Token Stemming):**
    - **Curated UK & Irish Reference Database (`data/local/StapleIngredientsDatabase.kt`):**
      - Curated exhaustive offline database based on official UK & Irish CoFID (McCance and Widdowson / Public Health England & FSAI reference dataset, the same gold standard underlying Nutracheck).
      - Added key UK/Irish butcher and staple items missing previously:
        - **Chicken Thighs:** Skinless & Boneless Raw (`152 kcal, 19.3g P, 8.3g F`), Skinless & Boneless Cooked/Roasted (`195 kcal, 26.5g P, 10.0g F`), Skin-On Bone-In Raw (`221 kcal, 17.5g P, 16.9g F`), Drumsticks.
        - **Chicken Breast:** Skinless & Boneless Raw (`106 kcal, 24.0g P, 1.1g F`), Skinless & Boneless Cooked (`148 kcal, 32.0g P, 2.2g F`), Diced Chicken Breast.
        - **Minced Beef & Steaks:** Minced Beef 5% Fat (`124 kcal, 21.4g P, 4.5g F`), 10%, 12%, 15%, 20%; Irish Sirloin Steak (`155 kcal`), Ribeye Steak, Fillet Steak, Rump Steak, Diced Stewing Beef.
        - **Pork & Bacon:** Pork Loin Medallions (`120 kcal, 22.8g P, 3.2g F`), 5% Pork Mince, Unsmoked & Smoked Back Bacon Medallions (`120 kcal, 25.0g P, 2.0g F`), Standard & Reduced Fat Irish Pork Sausages.
        - **Dairy & Kerrygold:** Kerrygold Irish Salted & Unsalted Butter (`717 kcal, 81g F`), Irish Whole Milk, Semi-Skimmed Milk, Skimmed Milk, Mature Irish Cheddar (`416 kcal, 25.4g P`), Lighter Cheddar, Irish Cottage Cheese, Fage Total 0% Greek Yogurt (`54 kcal, 10.3g P`).
        - **Breads & Oats:** Brennans Wholegrain/Wholemeal Bread (`82 kcal / slice`), White Sliced Bread, Artisan Sourdough, Flahavan's Irish Rolled Porridge Oats (`371 kcal, 12g P`).
        - **Potatoes & Grains:** Irish Rooster Potatoes, Maris Piper, Sweet Potatoes, Baby New Potatoes; Basmati Rice (Raw & Cooked), Jasmine Rice, Brown Rice, Durum Wheat Pasta.
        - **Pantry & Tinned Goods:** Heinz Baked Beans in Tomato Sauce (`81 kcal / 100g`), Canned Chopped Tomatoes, Tomato Passata, Tomato Puree, Kidney Beans, Chickpeas.
    - **Intelligent English Stemming & Typo Engine (`StapleIngredientsDatabase.stemWord`):**
      - Built a token stemmer mapping singular/plural and culinary abbreviations:
        - `"chick"` / `"chkn"` -> `"chicken"`
        - `"thighs"` -> `"thigh"`
        - `"breasts"` -> `"breast"`
        - `"potatoes"` / `"spuds"` -> `"potato"`
        - `"tomatoes"` -> `"tomato"`
        - `"mince"` -> `"minced"`
        - `"medallions"` -> `"medallion"`
      - Added exact phrase boosting (+100) and token-coverage boost (+150). Searching `"skinless boneless chick thighs"` now immediately resolves to `Chicken Thighs (Skinless & Boneless, Raw)` at Rank 1.
      - Penalized secondary mediums (`" in olive oil"` / `" in tomato sauce"` penalized when searching primary oils).
    - **UK/Irish Remote Search Prioritization (`OpenFoodFactsApi.kt`):**
      - Scoped remote queries using `countries_tags_en=en:united-kingdom,en:ireland`.
      - Boosted UK & Irish supermarket brands (+50 score) including Tesco, Sainsbury's, ASDA, Morrisons, Waitrose, M&S, Dunnes Stores, SuperValu, Lidl, Aldi, Birchwood Farm, Kerrygold, Brennans, Flahavan's, Cathedral City, Heinz.
      - Filtered out non-English foreign noise words (e.g. `matière grasse`, `surgelé`, `façon`, `bœuf`).
    - **UI Polish:**
      - Added green `"UK/Irish Staple"` badge and subtle emerald outline on staple cards in both `FoodSearchScreen.kt` and `SavedMealsScreen.kt`.
      - Updated popular staple chips in `FoodSearchViewModel.kt` with UK/Irish favourites.
    - **Verification & Release:**
      - Ran unit test suite via `.\gradlew.bat testReleaseUnitTest` (16/16 passed, including dedicated test for `"skinless boneless chick thighs"` ranking #1).
      - Built signed standalone release APK (`MacroAI-v1.0.0-release.apk`, 77.8MB).

12. **End Day Button, Gemini AI Daily Nutrition Overview & 4-Week Weight Projection:**
    - **Structured Gemini Daily Evaluation (`data/remote/gemini/dto/GeminiDayOverviewDto.kt` & `GeminiMealAnalyzer.kt`):**
      - Created `GeminiDayOverviewDto` and `GeminiDayOverviewResponseSchema` enforcing structured JSON output with `overallScore` (0–100), `scoreGrade` (`A+` to `D`), `headline`, `healthScore` (NOVA 4 UPF %, seed oils, fibre, micronutrients), `proteinAndMacroScore`, `calorieControlScore`, `executiveSummary`, `keyWins`, and `actionableImprovements`.
      - Added `analyzeDailyNutrition()` and `parseDayOverviewJson()` in `GeminiMealAnalyzer.kt` using the resilient `gemini-3.8-flash` -> `gemini-2.5-flash` -> `gemini-2.5-flash-lite` cascade.
    - **Calorie Deficit & 4-Week Weight Projection Engine (`domain/util/EndOfDayCalculator.kt` & `TdeeCalculator.kt`):**
      - Added `maintenanceKcal` to `TdeeResult` and built `EndOfDayCalculator.calculateMetrics()` using Wishnofsky's rule ($7,700\text{ kcal} = 1\text{ kg}$ over 28 days) to compute exact daily calorie deficit/surplus and state `"If you keep up with this, you'll weigh X.X kg in 4 weeks"`.
      - Included deterministic clinical fallback (`generateFallbackOverview`) if the device is offline.
    - **UI Flow (`ui/dashboard/EndOfDayOverviewModal.kt`, `DashboardScreen.kt`, `DashboardViewModel.kt`):**
      - Added **"End Day"** button at the bottom of the screen (in the bottom action dock and at the bottom of the daily log list).
      - Pressing **"End Day"** opens `EndDayConfirmationDialog` prompting `"Are you sure you have logged all of your food for the day?"` with a quick summary of logged meals, calories, and protein.
      - Confirming **"Yes, End Day"** launches `EndOfDayOverviewDialog` with animated radial AI score gauge, 3 pillar progress bars (Health/UPF/Seed Oils, Protein & Macros, Calorie Control), highlighted 4-week weight projection banner (`"If you keep up with this, you'll weigh x in 4 weeks"`), AI Nutritionist executive summary, key wins, and tomorrow's focus areas.

13. **Settings Expansion: Weight Unit (lbs / kg) Selection & Automatic Health Connect Biometrics Sync:**
    - **Weight Unit Preferences Engine (`domain/model/WeightUnit.kt`):**
      - Created `WeightUnit` enum (`KG`, `LBS`) with high-precision conversion helpers (`toKg`, `fromKg`, `format`, `formatValueOnly`).
      - Stored `weightUnit` in `UserProfile` and persisted via `UserPreferencesDataStore` with reactive Flow exposure.
    - **Automatic Health Connect Biometrics Synchronization (`HealthConnectManager.kt`, `DashboardViewModel.kt`, `TdeeSettingsViewModel.kt`):**
      - Extended `HealthConnectSnapshot` with `latestWeightTime`, `latestHeightTime`, and `hasBiometricsPermissions`.
      - Broadened Health Connect read window for `WeightRecord` (past 730 days + forward timezone margin) and `HeightRecord` (past 3,650 days).
      - Added `autoSyncHealthConnectBiometrics: Boolean = true` toggle to `UserProfile` and `UserPreferencesDataStore`.
      - Integrated automatic synchronization hooks in `DashboardViewModel` (triggered on app start & date change) and `TdeeSettingsViewModel` (triggered on screen focus & manual trigger).
      - Provided manual `"Sync Biometrics Now"` trigger button in Settings with instant Snackbar feedback and last-synced timestamp readouts.
    - **Unified Settings UI & Dynamic Recalculation (`ui/settings/TdeeSettingsScreen.kt`):**
      - **Weight Unit Segmented Switch:** Seamlessly converts input numbers in real time when toggling between `Kilograms (kg)` and `Pounds (lbs)`.
      - **Smart Supporting Text:** Displays secondary conversion values (e.g. `165.3 lbs` displays `≈ 75.0 kg`; `75.0 kg` displays `≈ 165.3 lbs`) and indicates `✓ Health Connect Synced`.
      - **Height in Feet & Inches:** Displays automatic imperial conversion (e.g. `178 cm` displays `≈ 5 ft 10 in`).
      - **Dual Protein Metrics:** Subtitle displays both `g/lb` and `g/kg` targets (e.g. `1.05 g/lb (2.3 g/kg)`), catering directly to strength/fitness athletes.
    - **End of Day Weight Projections (`EndOfDayCalculator.kt`, `EndOfDayOverviewModal.kt`):**
      - Updated 4-week weight projection banner to format dynamically in the user's preferred unit (`lbs` or `kg`), showing both converted deltas and projected progression.
    - **Testing & Verification:**
      - Added unit tests in `NutritionEnginesTest.kt` verifying `WeightUnit` mathematical conversions, `TdeeCalculator` respecting `autoSyncHealthConnectBiometrics`, and `EndOfDayCalculator` formatting.
      - Ran `.\gradlew.bat testReleaseUnitTest` (19/19 tests passed).
      - Built signed standalone release APK: `MacroAI-v1.0.0-release.apk` (78.0MB).

14. **Portrait Phone UI, Layout & Text Box Readability Refactoring:**
    - **Portrait Orientation Lock (`AndroidManifest.xml`):**
      - Configured `android:screenOrientation="portrait"` on `.MainActivity` alongside `android:windowSoftInputMode="adjustResize"`.
    - **Dashboard Portrait Polish (`ui/dashboard/DashboardScreen.kt`):**
      - Compacted TopAppBar title/subtitle (`"MacroAI"` / `"AI Nutrition & UPF"`) and action pills (`Sync`, `Recipes`, `Goals`) so they never crowd or wrap vertically on a 360dp portrait width.
      - Refactored `HeroCalorieAndMacroRingCard` to use `FlowRow` for top mode/HC badges, sized the radial gauge to `148.dp` so the right-hand stats column has ample width, and ensured `StatMiniPill` and `AnimatedMacroRingCard` never wrap or clip.
      - Balanced `UpfAndSeedOilBentoRow` with `titleMedium` values and fixed 2-line descriptions (`minLines = 2, maxLines = 2`).
      - Redesigned `FoodLogItemCard` from a cramped horizontal 3-column layout into a clean vertical card: Top row (Food Name + Calories pill), Middle row (Portion weight + P/C/F/Fibre macros), Bottom row (`FlowRow` of NOVA/Seed Oil/Recipe badges + compact Edit/Delete icons).
      - Refactored `ManualFoodEntryDialog` from 3-column `OutlinedTextField` rows (which were only ~85dp wide inside an `AlertDialog` and clipped labels) into a spacious 2-column grid (`Weight` + `Calories`, `Protein` + `Carbs`, `Fat` + `Fibre`, `PUFA` + `Sat Fat`) and `FlowRow` for NOVA chips.
    - **Settings Screen Portrait Polish (`ui/settings/TdeeSettingsScreen.kt`):**
      - Split the 3-column Biometrics `OutlinedTextField` row (`Weight`, `Height`, `Age`) into a spacious 2-column row (`Weight` + `Height`) followed by `Age (years)` + `Biological Sex` on the next row so floating labels and `✓ HC` supporting conversions never wrap or overlap.
      - Added `Modifier.weight(1f)` and `maxLines = 1, softWrap = false` to the Macro Sum row and Health Connect action buttons.
    - **AI Meal Review & Full-Screen Editor (`ui/camera/MealReviewBottomSheet.kt` & `ui/edit/EditFoodLogScreen.kt`):**
      - Restructured AI ingredient breakdown cards into 2-row cards with a `128.dp` `Grams` `OutlinedTextField` in `MealReviewBottomSheet.kt`.
      - Stacked `QualityAlertBanner` badges vertically when seed oils are detected so the full list of detected seed oils is readable without truncation.
      - Separated component ingredient macro summaries and weight steppers (`-10g`, full-width `OutlinedTextField`, `+10g`) onto dedicated rows in `EditFoodLogScreen.kt`, and converted the `Add Component Item` dialog from 3-column to 2-column text fields.
    - **Food Search, Recipes & End-of-Day Overview (`FoodSearchScreen.kt`, `SavedMealsScreen.kt`, `IngredientPortionSelectorDialog.kt`, `EndOfDayOverviewModal.kt`):**
      - Moved the `"= Xg total"` indicator out of the `OutlinedTextField` suffix into a header badge in `FoodSearchScreen.kt`, and converted the 4-item secondary lipid & micronutrient rows to `FlowRow`s.
      - Stacked `Recipe Name` and `Total Servings` full-width in `SavedMealsScreen.kt`, added `Modifier.weight(1f, fill = false)` to search result titles so `"Staple"` badges never clip, and converted the `Add Ingredient` dialog from 3-column to 2-column text fields.
      - Converted macro preview rows in `IngredientPortionSelectorDialog.kt` to `FlowRow` and prevented truncation in `EndOfDayOverviewModal.kt`.

15. **Phase 1: Batch Meal Prep & Bulk-Cooking Studio with Cooked-Weight Evaporation Math & Live Fridge Inventory:**
    - **Room v3 Batch Prep Storage (`data/local/entity/BatchPrepEntity.kt`, `BatchPrepDao.kt`, `MacroAiDatabase.kt`, `DatabaseModule.kt`):**
      - Implemented `BatchPrepEntity` with `rawIngredientsWeightGrams`, `cookedTotalWeightGrams`, `totalContainers`, `containersRemaining`, `prepDate`, `lastLoggedAt`, and calculated fields `cookingLossPercentage` (evaporation math: `(raw - cooked) / raw * 100`), `perContainerWeightGrams`, `perContainerCalories`, and `perContainerProtein`/`Carbs`/`Fat`.
      - Provided `toContainerFoodLogEntity(mealType)` and `toCustomGramsFoodLogEntity(scoopedGrams, mealType)` for precision portion logging.
      - Added Room `MIGRATION_2_3` with `batch_preps` table and provided `BatchPrepDao`.
    - **Meal Prep Studio UI (`ui/prep/MealPrepScreen.kt`, `MealPrepViewModel.kt`):**
      - **Active Fridge Inventory Card:** Visual container dots (filled vs logged), freshness indicators (days old with warning badges when older than 4 days), 1-tap "Log 1 Container" action, and custom gram scale logger.
      - **New Meal Prep Modal:** Raw ingredients builder, cooked pot weight input, real-time evaporation loss display, and container portion calculator.
    - **Dashboard & Navigation Integration (`ui/dashboard/DashboardScreen.kt`, `AppNavigation.kt`):**
      - Added `ActiveFridgePrepsDashboardCard` on the main dashboard showing remaining prepped meals in the fridge with 1-tap logging.
      - Added "Meal Prep" top action pill and wired `MealPrepRoute` in `AppNavigation`.

16. **Phase 2: Android Glance Home Screen Widget & Quick Settings Notification Shade Tile:**
    - **Android Glance App Widget (`ui/widget/MacroAiGlanceWidget.kt`, `MacroAiGlanceReceiver.kt`, `WidgetDataProvider.kt`, `MacroAiWidgetSnapshot.kt`, `MacroAiWidgetManager.kt`):**
      - Integrated `androidx.glance:glance-appwidget:1.1.1` and `androidx.glance:glance-material3:1.1.1`.
      - **Live Remaining Calories:** High-visibility remaining calorie counter with over-budget warning colors and consumed vs budget breakdown.
      - **Macro Progress Bars:** Cyan Protein, Amber Carbs, Rose Fat linear progress bars with consumed/target gram readouts.
      - **UPF / NOVA 4 & Seed Oils Alert Pill:** Live status badge showing UPF percentage and industrial seed oil warnings.
      - **Fridge Inventory Tag:** Displays active batch meal preps waiting in the fridge.
      - **Interactive 1-Tap Shortcuts:**
        - "📷 Scan" button launches directly into the Camera / ML Kit barcode scanner.
        - "🍲 Prep" button launches directly into the Meal Prep Studio.
        - Card body launches the Dashboard.
      - **Instant Updates (`MacroAiWidgetManager`):** Automatically dispatches widget updates on food log and batch prep mutations in `NutritionRepository`.
    - **Quick Settings Tile (`service/QuickScanTileService.kt`):**
      - Created system notification shade tile (`android.service.quicksettings.TileService`) with custom icon `ic_quick_scan_tile.xml`.
      - Tapping the tile unlocks the device and directly launches `MainActivity` with deep link action `ACTION_QUICK_SCAN` into `CameraCaptureRoute`.
    - **Deep Link Navigation Handling (`MainActivity.kt`, `AppNavigation.kt`):**
      - Handled both cold starts and warm background launches (`onNewIntent`) via `ACTION_QUICK_SCAN`, `ACTION_MEAL_PREP`, and `EXTRA_DESTINATION`.
    - **Testing & Verification:**
      - Added unit tests in `NutritionEnginesTest.kt` verifying `BatchPrepEntity` cooking loss & container portioning math, and `MacroAiWidgetSnapshot` calculation logic.
      - `testReleaseUnitTest` and `assembleRelease` executed with zero failures.
      - Signed release APK copied to `MacroAI-v1.0.0-release.apk` (80.7MB).

17. **Removal of Meal Prep Option & Codebase Cleanup:**
    - **Rationale:** The meal prep section was removed per user request.
    - **Clean Removal & Migrations:**
      - **Database (`data/local/MacroAiDatabase.kt`, `di/DatabaseModule.kt`):** Added `MIGRATION_3_4` dropping `batch_preps` table, bumped database version to 4, removed `BatchPrepEntity` and `batchPrepDao()`.
      - **Repository (`data/repository/NutritionRepository.kt`):** Removed `batchPrepDao` parameter and all batch prep methods (`observeActiveBatchPreps`, `saveBatchPrep`, `logBatchPrepPortion`, etc.).
      - **Deleted Files:** Removed `MealPrepScreen.kt`, `MealPrepViewModel.kt`, `BatchPrepEntity.kt`, `BatchPrepDao.kt`, and `ic_widget_prep.xml`.
      - **UI & Navigation (`ui/dashboard/DashboardScreen.kt`, `DashboardViewModel.kt`, `AppNavigation.kt`, `MainActivity.kt`):** Removed `ActiveFridgePrepsDashboardCard`, TopAppBar `Prep` pill, `MealPrepRoute`, and `ACTION_MEAL_PREP` intent routing.
      - **Home Screen Widget (`ui/widget/MacroAiGlanceWidget.kt`, `MacroAiWidgetSnapshot.kt`, `WidgetDataProvider.kt`):** Removed "🍲 Prep" button and active preps count badge, keeping the streamlined "📷 Scan" button and live calorie/macro rings.
    - **Testing & Verification:**
      - All unit tests in `NutritionEnginesTest.kt` executed cleanly (all passing).
      - Signed release APK assembled cleanly at `MacroAI-v1.0.0-release.apk` (80.5MB).

18. **Gemini Flash 3.8 Upgrade & AI Food Search Integration:**
    - **Gemini Model Upgrade (`gemini-3.8-flash`):**
      - Upgraded default vision and text prompt analysis engine to `gemini-3.8-flash`.
      - Migrated thinking configuration in `GeminiMealSchemaDto.kt` from legacy `thinkingBudget: Int` to `thinkingLevel: String = "low"` as required by Gemini 3.8 Flash specifications (preventing HTTP 400 validation errors).
      - Overhauled execution and fallback logic in `GeminiMealAnalyzer.kt`:
        - Primary attempt: `gemini-3.8-flash` with `thinkingLevel = "low"`.
        - Retry attempt: `gemini-3.8-flash` without thinking config.
        - Graceful network/quota fallback: `gemini-2.5-flash` only if `gemini-3.8-flash` is unreachable.
        - Removed the permanent model override bug that previously locked the app to `2.5-flash` permanently after a single transient failure.
    - **Direct AI Food Search in Search UI (`FoodSearchViewModel.kt`, `FoodSearchScreen.kt`):**
      - Integrated natural language AI search into the Search tab (`searchWithGeminiAi()`), enabling users to search for complex multi-ingredient dishes or home-cooked meals (e.g. "Homemade beef chili with kidney beans and sour cream") and receive accurate scaled macronutrient profiles with ingredient breakdown.
      - Added a dedicated "Search with Gemini Flash 3.8" card and loading indicator in `FoodSearchScreen.kt`.
    - **UI & Prompt Clarifications:**
      - Updated labels and prompts in `SavedMealsScreen.kt` ("AI Recipe (Gemini 3.8)") and `MealReviewBottomSheet.kt` to clearly indicate Gemini Flash 3.8.
    - **Testing & Verification:**
      - Verified all 15 unit tests pass with zero regressions.
      - Assembled signed release APK at `MacroAI-v1.0.0-release.apk`.

19. **Gemini Flash 3.1 Lite Migration & Complete Removal of Fallback Models:**
    - **Rationale:** Due to strict rate limits / quota exhaustion on Gemini 3.8 Flash, migrated the AI model to **Gemini Flash 3.1 Lite** (`gemini-3.1-flash-lite`), which offers substantially higher rate limits, lower latency, and cost-efficiency while retaining multimodal capabilities and structured output support.
    - **Complete Removal of Fallback Models:**
      - Removed `FALLBACK_FAST_MODEL` (`gemini-2.5-flash`) and `ULTRA_LITE_MODEL` (`gemini-2.5-flash-lite`) from `GeminiMealAnalyzer.kt`.
      - Streamlined `executeGeminiRawJsonRequest`:
        - Primary attempt targets solely `gemini-3.1-flash-lite` with `thinkingLevel = "low"`.
        - If an HTTP 429 (Rate Limit) or 403 (Auth/Quota) occurs, the error is immediately returned without burning additional requests.
        - Secondary retry on the same model (`gemini-3.1-flash-lite`) is only performed with `thinkingConfig = null` in case thinking level parameters are rejected.
        - Absolutely no secondary/fallback models are invoked.
    - **UI & Model Branding Updates:**
      - Updated `MealReviewBottomSheet.kt` ("Gemini Flash 3.1 Lite").
      - Updated `SavedMealsScreen.kt` ("AI Recipe (Gemini 3.1 Lite)").
      - Updated `FoodSearchScreen.kt` and `FoodSearchViewModel.kt` ("AI Food Search (Gemini Flash 3.1 Lite)").
      - Updated `EndOfDayOverviewModal.kt` ("Gemini Flash 3.1 Lite" for confirmation, scoring progress, and report badges).
      - Confirmed that the "End Day" pipeline (`DashboardViewModel` -> `NutritionRepository.generateEndOfDayReport` -> `GeminiMealAnalyzer.analyzeDailyNutrition`) routes directly and exclusively through `MODEL_NAME = "gemini-3.1-flash-lite"`.
      - Updated `AGENTS.md` to reference `gemini-3.1-flash-lite`.
    - **Testing & Verification:**
      - Unit tests verified via `testReleaseUnitTest`.
      - Signed release APK built and copied to `MacroAI-v1.0.0-release.apk`.

20. **Separation of TDEE (Energy Burned) and Daily Calorie Target (Energy Consumed Goal):**
    - **Problem Identified:** Previously, choosing a fixed intake goal (e.g. 2100 kcal) was stored in `manualOverrideKcal` and treated directly as `tdeeKcal` and `maintenanceKcal`. Because maintenance expenditure was forced to equal the intake goal (2100), the calculated daily deficit was `2100 - 2100 = 0`, causing the 4-week weight loss projection to incorrectly calculate 0 kg change.
    - **Architecture & Domain Redesign:**
      - **Separated Concepts:**
        - **TDEE (`tdeeKcal` / `maintenanceKcal`):** How much energy the user's body expends per day (Energy Out). Determined by Mifflin-St Jeor Formula (BMR × Activity Multiplier) or Health Connect Dynamic (BMR + live Active Calories burned).
        - **Daily Calorie Target (`energyTargetKcal`):** How many calories the user intends to eat (Energy In / Intake Goal).
      - **`EnergyTargetMode` (`domain/model/TdeeMode.kt`):**
        - `SET_CALORIE_GOAL`: Fixed total calorie consumed target (e.g. 2100 kcal). TDEE expenditure is calculated from biological biometrics (BMR) and activity level.
        - `TDEE_DEFICIT`: Dynamic target = TDEE minus chosen weight-loss deficit (e.g. -500 kcal).
      - **`UserProfile` (`domain/model/UserProfile.kt`):** Added `energyTargetMode: EnergyTargetMode` and `customCalorieTargetKcal: Int = 2100`.
      - **`TdeeCalculator` (`domain/util/TdeeCalculator.kt`):**
        - Computes true TDEE maintenance expenditure and daily `energyTargetKcal`.
        - Calculates planned deficit: `plannedDeficitKcal = tdeeKcal - energyTargetKcal`.
        - Allocates macros based on the daily consumed intake target (`energyTargetKcal`).
      - **`EndOfDayCalculator` (`domain/util/EndOfDayCalculator.kt`):**
        - Computes daily deficit as `maintenanceKcal (TDEE burn) - consumedKcal`.
        - Wishnofsky's rule now correctly projects 4-week weight loss: `-(dailyDeficit * 28) / 7700`. If TDEE is 2759 kcal and user eats 2100 kcal, deficit is 659 kcal/day, correctly projecting a -2.4 kg weight loss in 4 weeks.
      - **`TdeeSettingsScreen.kt`:**
        - Refactored into two dedicated sections: **1. Daily Calorie Target (Energy In)** and **2. Total Daily Energy Expenditure (Energy Out)**.
        - Summary card displays both Target (`2100 kcal`) and TDEE (`2759 kcal`), along with planned daily deficit and 4-week weight loss.
      - **`DashboardScreen.kt` & Widget:**
        - Radial arc and budget metrics track consumption against `energyTargetKcal` (2100 kcal).
        - Dashboard pill displays: `Target 2100 • TDEE 2759 kcal`.
        - Quick stats column displays "Consumed Today", "Energy Target", and "Food Weight".
    - **Testing & Verification:**
      - Added dedicated unit test `TdeeCalculator and EndOfDayCalculator separate TDEE expenditure from set calorie consumed goal` in `NutritionEnginesTest.kt`.
      - All 16 unit tests passed (`testReleaseUnitTest`).
      - Assembled signed release APK at `MacroAI-v1.0.0-release.apk`.

21. **AI Ingredient Swapper, Seed Oil Traffic Light System & Serving Size vs Grams Selector:**
    - **Goal 1: AI Ingredient Swapper (`SwapIngredientDialog.kt`, `MealReviewBottomSheet.kt`, `CameraCaptureViewModel.kt`, `UnifiedCameraScreen.kt`):**
      - Added interactive "Swap" button on each ingredient card during AI meal analysis review.
      - Built `SwapIngredientDialog` featuring real-time search across the offline staples database (verified cuts of beef, chicken, dairy, rice, oils) and Open Food Facts remote products with UK/Irish CoFID ranking.
      - Embedded instant packet barcode scanner within the swap dialog for swapping ingredients directly from a packet barcode.
      - Integrated `IngredientPortionSelectorDialog` into the swap flow, allowing the user to select typical servings (`1 serving Xg`), match original guessed weight, or input custom grams.
      - Live recomputes meal totals (calories, macros, NOVA classification, and seed oils) upon swapping or removing items.
    - **Goal 2: Seed Oil Traffic Light System & Load/Ratio Engine (`SeedOilDetector.kt`, `FoodLogEntity.kt`, `QualityAlertBanner`, `DashboardScreen.kt`, `FoodSearchScreen.kt`, `EditFoodLogScreen.kt`, `SavedMealsScreen.kt`):**
      - Overhauled binary seed oil detection into a 3-tier traffic light engine:
        - **Green (`LOW / CLEAN`):** Seed-oil free (0g) or trace amounts (`<= 2.5g` or `<= 2.5%` ratio, e.g. trace sunflower oil in bread or minor salad dressing). Badged in emerald with checkmark.
        - **Amber (`MODERATE`):** Moderate cooking oil (`> 2.5g` to `<= 10.0g`, e.g. stir-fried in 1 tsp canola or pan-seared). Badged in amber warning.
        - **Red (`HIGH`):** Heavy industrial or oxidised deep-frying oil (`> 10.0g` or `> 10%` ratio, or commercial deep-fried chips, crisps, battered fish). Badged in crimson danger with deep-fried indicator.
      - Added `seedOilAssessment()` helper to `FoodLogEntity`, `GeminiMealAnalysisDto`, `NormalizedOffProduct`, and `SavedMealEntity` for zero-migration instant calculation across all logs, recipes, and search results.
      - Updated `UpfAndSeedOilBentoRow` and log cards on `DashboardScreen.kt` to reflect daily traffic light distribution (`high`, `mod`, `trace`) and estimated seed oil grams instead of penalizing trace items with a red alert.
    - **Goal 3: Typical Serving Size vs Custom Grams Option (`MealReviewBottomSheet.kt`, `CameraCaptureViewModel.kt`, `FoodSearchScreen.kt`):**
      - In barcode review: added serving option chips (`1 Serving (${servingSizeName})`, `Custom Grams`, `100g Portion`) referencing the typical package serving (e.g. KitKat 1 serving 17g) alongside a servings stepper (`[ - ]` `X servings` `[ + ]`), while keeping the direct grams text field for manual entry.
      - In AI meal review: added serving preset chips (`1 serving ~Xg`) and +/- 10g adjustment steppers per ingredient.
      - In search and recipes: standardized Cronometer / MyFitnessPal-style serving selector defaulting to the manufacturer's typical serving size.
    - **Testing & Verification:**
      - Added dedicated unit test `SeedOilDetector differentiates 2g trace sunflower oil (GREEN) from 50g deep fried oil (RED) and moderate cooking oil (AMBER)` in `NutritionEnginesTest.kt`.
      - Executed `.\gradlew.bat testReleaseUnitTest` (all 24 unit tests passed).
      - Built signed release APK and refreshed root `MacroAI-v1.0.0-release.apk` (80.6MB).

22. **Dual AI Model Switcher (3.1 / 3.5 Flash Lite), Backend vs AI End Day Options, Precision Multi-Source Food Search, Barcode Default & Per-Meal Insights Drill-Down:**
    - **1. Avoid AI Rate Limits — Model Switcher (`AiModel.kt`, `UserPreferencesDataStore.kt`, `GeminiMealAnalyzer.kt`):**
      - Created `AiModel` enum supporting `GEMINI_3_1_FLASH_LITE` (`gemini-3.1-flash-lite`, `"3.1 Flash Lite"`) and `GEMINI_3_5_FLASH_LITE` (`gemini-3.5-flash-lite`, `"3.5 Flash Lite"`).
      - Persisted `selectedAiModel` in `UserProfile` / `UserPreferencesDataStore` (`selected_ai_model`).
      - Updated `GeminiMealAnalyzer` (`analyzeMealPhoto`, `analyzeEncodedPhoto`, `analyzeTextPrompt`, `analyzeDailyNutrition`) to route requests dynamically to the selected model's endpoint, allowing instant switching when one quota pool experiences high load.
      - Added interactive `3.1 Flash Lite` / `3.5 Flash Lite` model selector chips in the AI Camera deck (`UnifiedCameraScreen.kt`), AI Photo Context sheet (`MealReviewBottomSheet.kt`), and End of Day controls (`EndOfDayOverviewModal.kt`).
    - **2. Dual End Day Options — Backend Calculation vs AI Analysis (`EndOfDayCalculator.kt`, `NutritionRepository.kt`, `DashboardViewModel.kt`, `EndOfDayOverviewModal.kt`):**
      - Added `EndOfDayCalculator.generateLocalReport()` for instantaneous (0ms, zero API calls) on-device backend calculation of calorie deficit, 4-week weight projection, 3-pillar scores (Health/UPF/Seed Oils, Protein & Macros, Calorie Control), and deterministic wins/focus areas.
      - Provided two explicit End Day actions on the Dashboard and inside the End Day confirmation dialog:
        - **End Day (Backend Stats Only):** Instantaneous local calculation with `"On-Device Backend Score"` badge.
        - **End Day AI Analysis:** Calls Gemini using the selected model (`3.1 Flash Lite` or `3.5 Flash Lite`) with `"Gemini 3.X Flash Lite Score"` badge.
    - **3. High-Precision Food Search (`NutritionRepository.kt`, `OpenFoodFactsApi.kt`, `StapleIngredientsDatabase.kt`, `FoodLogDao.kt`, `SavedMealDao.kt`):**
      - Diagnosed why Open Food Facts Search-a-licious struggled on products like *"Avonmore Chocolate Gold Protein Milk"* (barcode `5011056023559` was indexed on OFF with `product_name: "Protein gold"` and `brands: "Avonmore"`, so multi-word queries diluted Elasticsearch's OR matching).
      - Upgraded `NutritionRepository.searchFoods(query)` to search 4 tiers in parallel and rank with `StapleIngredientsDatabase.scoreProductRelevance`:
        1. **Previously Logged & Scanned Barcodes (`food_logs`):** Any barcode scanned once is permanently searchable by its full name or brand in Room.
        2. **Saved Custom Meals & Recipes (`saved_meals`).**
        3. **Curated UK & Irish Staples (`StapleIngredientsDatabase`):** Added verified Irish/UK protein dairy products including `Avonmore Chocolate Gold Protein Milk` (`5011056023559`), `Avonmore Vanilla Gold Protein Milk`, `Avonmore Protein Milk Original`, `Avonmore Strawberry Protein Milk`, `Avonmore Super Milk`, `Glenisk High Protein Yogurt`, and `Arla Protein Chocolate Pudding`.
        4. **Targeted Open Food Facts Lucene Search (`OpenFoodFactsApi`):** Detects known UK/Irish brands in the query and runs a parallel `brands:"<brand>" <remaining_tokens>` Lucene query alongside a 50-item candidate pool, re-ranked by multi-field token coverage across brand + product name + ingredients.
    - **4. Barcode Scanner Default (`CameraCaptureViewModel.kt`, `UnifiedCameraScreen.kt`):**
      - Reordered `CameraCaptureMode` so `BARCODE_SCAN` ("Barcode Scanner") is the first tab and default launch mode (`captureMode = CameraCaptureMode.BARCODE_SCAN`), with `AI_PHOTO` ("AI Meal Photo") as the second tab.
    - **5. Expanded Meal Section Insights & Drill-Down Menus (`DashboardScreen.kt`):**
      - Enhanced each meal section header (`Breakfast`, `Lunch`, `Dinner`, `Snack`) with inline macro badges (`P Xg`, `C Xg`, `F Xg`, `Fibre Xg`), share of daily calories (`X% of day`), total meal weight (`Xg`), and an expandable `Insights` toggle.
      - Built `MealInsightsDrillDownCard` (`AnimatedVisibility`) exposing per-meal macro calorie split bar (`% Protein`, `% Carbs`, `% Fat`), per-meal macro cards (`% of daily goal`, kcal, Sugar, Fibre, Saturated Fat, PUFA), per-meal NOVA 4 UPF % & weight, per-meal Seed Oil Traffic Light assessment, and key electrolytes (`Na`, `K`, `Ca`, `Fe`).

23. **v1.0.0 Release: Configurable In-App Credentials, Setup Guide Tutorial, GitHub Actions CI/CD & Git Initial Release:**
    - **Configurable Credentials & Dual-Layer Hierarchy (`UserProfile.kt`, `UserPreferencesDataStore.kt`, `GeminiMealAnalyzer.kt`, `SheetsWebhookClient.kt`, `SheetsSyncWorker.kt`):**
      - Added `customGeminiApiKey` and `customSheetsWebhookUrl` to `UserProfile` and persisted them in `UserPreferencesDataStore` (`custom_gemini_api_key`, `custom_sheets_webhook_url`).
      - Implemented resolution hierarchy in `GeminiMealAnalyzer.getEffectiveApiKey` and `SheetsWebhookClient.getEffectiveWebhookUrl`:
        1. Custom In-App Key/URL (User-configured via UI).
        2. Build-time `local.properties` / `BuildConfig` fallback.
      - Added test ping functions: `GeminiMealAnalyzer.testApiKey(key)` and `SheetsWebhookClient.testWebhookUrl(url)` with live HTTP status verification.
      - Updated `SheetsSyncWorker` to gracefully skip sync when unconfigured rather than failing.
      - Updated `scripts/google_apps_script_webhook.js` with optional spreadsheet ID (defaults to active sheet), `doGet` health check, and ping handler.
    - **Settings UI & In-App Setup Tutorial (`TdeeSettingsScreen.kt`, `TdeeSettingsViewModel.kt`):**
      - Added **"4. Cloud & AI Integration"** card to Settings:
        - Masked Gemini API key field with show/hide toggle, status badge (`Custom Key Set` / `Build Default Active` / `Key Missing`), and interactive `Test Key` button.
        - Google Sheets Webhook URL field with status badge and `Test Webhook` button.
        - `Save Credentials` and `Clear Keys` buttons.
      - Added interactive `SetupGuideDialog` with tabbed tutorials:
        - Tab 1: Free Gemini AI Studio API key generation in 4 steps.
        - Tab 2: Google Sheets Apps Script Webhook deployment in 6 steps.
    - **Documentation & Setup Guides (`README.md`, `docs/SETUP_GUIDE.md`, `LICENSE`):**
      - Created comprehensive GitHub `README.md` with feature tour, privacy philosophy, and quick-start instructions.
      - Created `docs/SETUP_GUIDE.md` walking users through obtaining free API keys, setting up Google Sheets, and configuring GitHub secrets.
      - Added Apache License 2.0 (`LICENSE`).
    - **GitHub Actions CI/CD & Build Pipeline (`.github/workflows/android-release.yml`, `app/build.gradle.kts`):**
      - Automated CI workflow on push/PR to `main` and release tags (`v*`).
      - Set up JDK 17, Gradle cache, release unit testing (`testReleaseUnitTest`), and APK build (`assembleRelease`).
      - Automated GitHub Releases creation with attached signed APK on `v*` tag push.
      - Updated `app/build.gradle.kts` to read secrets from environment variables and gracefully fall back to debug signing if release keystore is absent on CI.
    - **Test Baseline & Verification (`NutritionEnginesTest.kt`):**
      - Fixed `OpenFoodFactsApi searchFoods` and `EndOfDayCalculator generateLocalReport` test assertions.
      - Added unit tests for credential resolution and webhook connection testing (30/30 unit tests passing).
      - Built standalone signed release APK (`MacroAI-v1.0.0-release.apk`, 80.8MB).
      - Initialized Git repository on `main`, committed all files with clean `.gitignore`, and tagged release `v1.0.0`.


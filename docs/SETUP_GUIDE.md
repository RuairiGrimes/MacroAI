# MacroAI — Setup & Configuration Guide

Welcome to **MacroAI**! Because MacroAI is a privacy-first, 100% on-device Android application with no middleman backend server, you have complete ownership of your data and credentials.

This guide walks you through setting up:
1. **Gemini AI API Key (Free):** Powers instant meal photo scanning, ingredient breakdowns, and daily metabolic reviews.
2. **Google Sheets Backup (Free):** Automatically archives every meal, micronutrient, and UPF assessment to your own Google Sheet.
3. **GitHub Actions CI/CD (Optional):** How to fork and build your own signed APKs using GitHub Actions.

---

## 1. Getting Your Free Gemini AI API Key (Takes 60 Seconds)

MacroAI uses **Google Gemini Flash 3.1 & 3.5 Lite** to analyze meal photos and evaluate daily nutrition. Google AI Studio provides a free tier with generous rate limits (up to 15 requests/minute), which is more than enough for daily tracking at zero cost.

### Step-by-Step Instructions:

1. **Visit Google AI Studio:**
   - Go to [https://aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey) in your browser.
   - Sign in with any personal Google account.

2. **Generate Your Key:**
   - Click the blue **"Create API key"** (or **"Get API key"**) button.
   - Choose **"Create API key in new project"** (or select an existing Google Cloud project).
   - Once generated, click the copy icon next to your new key. The key will start with `AIzaSy...`.

3. **Paste Into MacroAI:**
   - On your phone, open **MacroAI**.
   - Tap **Goals** (or open Settings from the top bar).
   - Scroll down to section **4. Cloud & AI Integration**.
   - Paste your key into the **Gemini AI API Key** field.
   - Tap **"Test Key"**. You should see a green confirmation:
     > `✓ Gemini API Key Verified & Working!`
   - Tap **"Save Credentials"**.

> [!TIP]
> Your key is stored securely on your device using Android Preferences DataStore. It is never transmitted anywhere except directly to Google's official Gemini API endpoint (`generativelanguage.googleapis.com`) over HTTPS.

---

## 2. Setting Up Your Google Sheets Backup Webhook (Takes 2 Minutes)

MacroAI automatically syncs your daily food logs, lipid profiles (PUFA / MUFA / SatFat), 7 micronutrients, and NOVA 4 UPF scores to your personal Google Sheet in the background.

### Step-by-Step Instructions:

1. **Create a New Google Sheet:**
   - In your browser, navigate to [https://sheets.new](https://sheets.new).
   - Give your sheet a name, for example: `My MacroAI Nutrition Journal`.

2. **Open the Apps Script Editor:**
   - In the top menu toolbar of your Google Sheet, click **Extensions** → **Apps Script**.
   - A new tab will open with a code editor showing a default `myFunction()`.

3. **Paste the MacroAI Webhook Script:**
   - Delete all existing text in the editor (`Code.gs`).
   - Copy the entire code from [`scripts/google_apps_script_webhook.js`](../scripts/google_apps_script_webhook.js) and paste it into `Code.gs`.
   - Click the floppy disk icon (💾 **"Save project"**) or press `Ctrl + S` / `Cmd + S`.

4. **Deploy as a Web App:**
   - Click the blue **"Deploy"** button at the top right of the Apps Script window.
   - Select **"New deployment"**.
   - If "Web app" is not selected, click the gear icon (⚙️) next to *Select type* and choose **"Web app"**.
   - Configure the following settings:
     - **Description:** `MacroAI Webhook`
     - **Execute as:** `Me (<your-email>@gmail.com)`
     - **Who has access:** `Anyone` *(Note: This is required so MacroAI's Android background sync worker can POST food logs to your sheet without requiring OAuth token refreshes).*
   - Click **"Deploy"**.

5. **Authorize Permissions:**
   - Google will prompt you to **"Authorize access"** so the script can edit your spreadsheet.
   - Select your Google account.
   - If you see *Google hasn't verified this app*, click **"Advanced"** → **"Go to MacroAI Webhook (unsafe)"** → click **"Allow"**.

6. **Copy Your Web App URL:**
   - Copy the generated **Web app URL**. It will look like:
     ```
     https://script.google.com/macros/s/AKfycb.../exec
     ```
   *(Important: Ensure the URL ends in `/exec`, not `/edit`).*

7. **Paste Into MacroAI:**
   - In MacroAI, go to **Settings** → **4. Cloud & AI Integration**.
   - Paste the URL into the **Google Sheets Webhook URL** field.
   - Tap **"Test Webhook"**. You should see:
     > `✓ Google Sheets Webhook Connected Successfully!`
   - Tap **"Save Credentials"**.

### What Happens When You Log Food:
- The first time you log food or tap **"Sync"** on the Dashboard, the script will automatically create a new sheet tab named `FoodLogs` and populate all 28 clinical columns (UUID, Meal Type, Calories, Protein, Carbs, Fat, NOVA Group, Seed Oils, Saturated Fat, PUFA, Micronutrients, and AI Clinical Reasoning).
- Any food logged while offline is safely queued and automatically uploaded in the background as soon as network connectivity is restored.

---

## 3. Advanced: Configuring GitHub Actions Secrets (For Developers)

If you are forking the MacroAI repository and want GitHub Actions to automatically build and release pre-configured APKs with your keys baked in:

1. In your GitHub repository, go to **Settings** → **Secrets and variables** → **Actions**.
2. Click **"New repository secret"** and add:
   - `GEMINI_API_KEY`: Your Google AI Studio API key.
   - `SHEETS_WEBHOOK_URL`: Your Apps Script Web App URL.
   - *(Optional for signed releases)*:
     - `KEYSTORE_BASE64`: Base64-encoded `.jks` release keystore file.
     - `KEYSTORE_PASSWORD`: Keystore password.
     - `KEY_ALIAS`: Key alias.
     - `KEY_PASSWORD`: Key password.
3. When you push to `main` or push a tag like `v1.0.0`, the automated workflow in `.github/workflows/android-release.yml` will run unit tests, assemble the APK, and publish it under your GitHub Releases.

---

## 4. Troubleshooting & FAQ

#### Q: "Gemini API rejected key (HTTP 400 or 403)"
- Ensure you copied the entire key without extra spaces.
- Confirm your Google AI Studio project has the Gemini API enabled.
- Verify that your selected model (`3.1 Flash Lite` or `3.5 Flash Lite`) is available in your region.

#### Q: "Webhook returned HTTP 401 or 302"
- In Google Apps Script, ensure **Who has access** was set to **"Anyone"** (not *"Only myself"*).
- Make sure you copied the `/exec` URL from the deployment dialog, not the browser URL of the script editor.

#### Q: Can I run MacroAI completely offline?
- **Yes!** If you do not configure an API key or Google Sheets webhook, MacroAI functions 100% offline. You can log food using the curated UK & Irish staples database, scan packet barcodes (ML Kit is offline), search saved recipes, track NOVA 4 UPF percentages, and use the **End Day (Backend Stats Only)** on-device calculation engine.


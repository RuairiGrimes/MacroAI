/**
 * MacroAI — Google Sheets Apps Script Webhook Receiver
 *
 * QUICK SETUP (Takes 60 seconds):
 * 1. Open Google Sheets (https://sheets.new) in your browser.
 * 2. Name your sheet (e.g. "My MacroAI Food Journal").
 * 3. In the top menu, go to Extensions -> Apps Script.
 * 4. Delete any code in Code.gs and paste this ENTIRE file.
 * 5. Click the floppy disk icon ("Save project").
 * 6. Click the blue "Deploy" button at the top right -> "New deployment":
 *    - Click the gear icon next to "Select type" -> select "Web app".
 *    - Description: "MacroAI Webhook"
 *    - Execute as: "Me (your email)"
 *    - Who has access: "Anyone" (Required so MacroAI can post logs securely)
 * 7. Click "Deploy" and authorize permissions when prompted.
 * 8. Copy the "Web app URL" (ends in /exec).
 * 9. Paste this URL into MacroAI:
 *    - In the MacroAI App: Open Settings -> Cloud & AI Integration -> paste under Google Sheets Webhook URL, then tap "Test Webhook".
 */

// Leave blank ("") when pasting this into your Google Sheet's Apps Script editor.
// It will automatically use the active spreadsheet.
const SPREADSHEET_ID = "";
const SHEET_NAME = "FoodLogs";

const HEADERS = [
  "ID (UUID)",
  "Date",
  "ISO Timestamp",
  "Meal Type",
  "Food Name",
  "Source",
  "Weight (g)",
  "Calories (kcal)",
  "Protein (g)",
  "Carbs (g)",
  "Fat (g)",
  "Saturated Fat (g)",
  "Polyunsaturated PUFA (g)",
  "Monounsaturated MUFA (g)",
  "Sugar (g)",
  "Fibre (g)",
  "NOVA Group (1-4)",
  "Is UPF (NOVA 4)",
  "Contains Seed Oils",
  "Detected Seed Oils",
  "Sodium (mg)",
  "Potassium (mg)",
  "Iron (mg)",
  "Calcium (mg)",
  "Vitamin C (mg)",
  "Vitamin D (ug)",
  "Magnesium (mg)",
  "AI Clinical Reasoning"
];

function doGet(e) {
  return jsonResponse({
    status: "ok",
    message: "MacroAI Google Sheets Webhook is active and reachable."
  });
}

function doPost(e) {
  const lock = LockService.getScriptLock();
  lock.waitLock(25000);

  try {
    if (!e || !e.postData || !e.postData.contents) {
      return jsonResponse({ status: "error", message: "Empty request body" });
    }

    const payload = JSON.parse(e.postData.contents);

    // Instant connection ping handling
    if (payload.action === "ping" || payload.ping === true) {
      return jsonResponse({
        status: "ok",
        message: "MacroAI Webhook Connected Successfully"
      });
    }

    const entries = Array.isArray(payload.entries)
      ? payload.entries
      : Array.isArray(payload)
      ? payload
      : [payload];

    const ss = SPREADSHEET_ID
      ? SpreadsheetApp.openById(SPREADSHEET_ID)
      : SpreadsheetApp.getActiveSpreadsheet();
    let sheet = ss.getSheetByName(SHEET_NAME);
    if (!sheet) {
      sheet = ss.insertSheet(SHEET_NAME);
    }

    ensureHeaders(sheet);

    // Read existing IDs in Column A to guarantee idempotency if WorkManager retries
    const existingIds = getExistingIdsSet(sheet);
    const rowsToAppend = [];

    for (let i = 0; i < entries.length; i++) {
      const item = entries[i];
      if (!item || !item.id) continue;
      if (existingIds.has(String(item.id))) continue;

      rowsToAppend.push([
        item.id || "",
        item.date || "",
        item.isoDateTime || new Date(item.timestamp || Date.now()).toISOString(),
        item.mealType || "",
        item.name || "",
        item.source || "",
        Number(item.servingWeightGrams || 0),
        Number(item.calories || 0),
        Number(item.proteinG || 0),
        Number(item.carbsG || 0),
        Number(item.fatG || 0),
        Number(item.saturatedFatG || 0),
        Number(item.polyunsaturatedFatG || 0),
        Number(item.monounsaturatedFatG || 0),
        Number(item.sugarG || 0),
        Number(item.fibreG || 0),
        item.novaGroup !== null && item.novaGroup !== undefined ? Number(item.novaGroup) : "",
        Boolean(item.isUpf),
        Boolean(item.containsSeedOils),
        item.detectedSeedOils || "",
        Number(item.sodiumMg || 0),
        Number(item.potassiumMg || 0),
        Number(item.ironMg || 0),
        Number(item.calciumMg || 0),
        Number(item.vitaminCMg || 0),
        Number(item.vitaminDUg || 0),
        Number(item.magnesiumMg || 0),
        item.aiConfidenceReasoning || ""
      ]);

      existingIds.add(String(item.id));
    }

    if (rowsToAppend.length > 0) {
      const startRow = sheet.getLastRow() + 1;
      sheet
        .getRange(startRow, 1, rowsToAppend.length, HEADERS.length)
        .setValues(rowsToAppend);
    }

    return jsonResponse({
      status: "success",
      appendedCount: rowsToAppend.length,
      totalReceived: entries.length
    });
  } catch (err) {
    return jsonResponse({
      status: "error",
      message: String(err && err.stack ? err.stack : err)
    });
  } finally {
    lock.releaseLock();
  }
}

function ensureHeaders(sheet) {
  if (sheet.getLastRow() === 0) {
    sheet.getRange(1, 1, 1, HEADERS.length).setValues([HEADERS]);
    const headerRange = sheet.getRange(1, 1, 1, HEADERS.length);
    headerRange.setFontWeight("bold");
    headerRange.setBackground("#064E3B");
    headerRange.setFontColor("#FFFFFF");
    sheet.setFrozenRows(1);
  }
}

function getExistingIdsSet(sheet) {
  const lastRow = sheet.getLastRow();
  const idSet = new Set();
  if (lastRow <= 1) return idSet;

  const values = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
  for (let i = 0; i < values.length; i++) {
    const id = values[i][0];
    if (id) idSet.add(String(id));
  }
  return idSet;
}

function jsonResponse(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(
    ContentService.MimeType.JSON
  );
}


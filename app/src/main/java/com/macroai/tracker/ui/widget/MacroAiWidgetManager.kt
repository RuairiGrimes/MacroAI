package com.macroai.tracker.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object MacroAiWidgetManager {
    suspend fun updateWidget(context: Context) {
        runCatching {
            MacroAiGlanceWidget().updateAll(context)
        }
    }
}


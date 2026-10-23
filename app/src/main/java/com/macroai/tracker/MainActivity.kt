package com.macroai.tracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.macroai.tracker.ui.navigation.AppNavigation
import com.macroai.tracker.ui.theme.MacroAiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_QUICK_SCAN = "com.macroai.tracker.ACTION_QUICK_SCAN"
        const val EXTRA_DESTINATION = "extra_destination"
        const val DEST_SCANNER = "scanner"
    }

    private var targetDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            MacroAiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        initialDestination = targetDestination,
                        onClearInitialDestination = { targetDestination = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == ACTION_QUICK_SCAN || intent.getStringExtra(EXTRA_DESTINATION) == DEST_SCANNER) {
            targetDestination = DEST_SCANNER
        }
    }
}

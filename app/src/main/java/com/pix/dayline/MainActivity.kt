package com.pix.dayline

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pix.dayline.ui.DaylineApp

class MainActivity : ComponentActivity() {
    private var launchAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        launchAction = intent?.getStringExtra(EXTRA_ACTION)

        setContent {
            DaylineApp(
                externalAction = launchAction,
                onExternalActionConsumed = { launchAction = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchAction = intent.getStringExtra(EXTRA_ACTION)
    }

    companion object {
        const val EXTRA_ACTION = "dayline_action"
        const val ACTION_QUICK_ADD = "quick_add"
    }
}

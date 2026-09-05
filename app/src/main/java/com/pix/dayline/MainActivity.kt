package com.pix.dayline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pix.dayline.ui.DaylineApp
import com.pix.dayline.ui.theme.DaylineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DaylineTheme {
                DaylineApp()
            }
        }
    }
}

package com.example.inteltrace_v3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.inteltrace_v3.presentation.navigation.IntelTraceNavigation
import com.example.inteltrace_v3.ui.theme.IntelTrace_v3Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IntelTrace_v3Theme {
                IntelTraceNavigation()
            }
        }
    }
}
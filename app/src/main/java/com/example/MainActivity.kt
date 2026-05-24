package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.BreviaryScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: com.example.ui.BreviaryViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[com.example.ui.BreviaryViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                BreviaryScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val hourName = intent?.getStringExtra("hour_name")
        if (hourName != null) {
            try {
                val hour = com.example.data.LiturgicalHour.valueOf(hourName)
                viewModel.selectHour(hour)
            } catch (e: Exception) {
                // Ignore invalid
            }
        }
    }
}

package com.example.tgexporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tgexporter.ui.nav.AppNavGraph
import com.example.tgexporter.ui.theme.TelegramChatExporterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as ExporterApp).container
        setContent {
            TelegramChatExporterTheme {
                AppNavGraph(container)
            }
        }
    }
}

package com.devmarkabrasaldo.PataGilid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.devmarkabrasaldo.PataGilid.ui.navigation.PataGilidNavigation
import com.devmarkabrasaldo.PataGilid.ui.theme.PataGilidTheme

import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PataGilidApplication).container
        setContent {
            PataGilidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PataGilidNavigation(container = container)
                }
            }
        }
    }
}
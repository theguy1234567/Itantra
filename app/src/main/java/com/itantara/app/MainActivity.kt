package com.itantara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.itantara.app.presentation.navigation.NavGraph
import com.itantara.app.presentation.theme.ITantraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ITantraTheme {
                NavGraph()
            }
        }
    }
}

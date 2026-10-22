package com.rdgen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rdgen.app.ui.GeneratorScreen
import com.rdgen.app.ui.theme.RdGeneratorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RdGeneratorTheme {
                GeneratorScreen()
            }
        }
    }
}

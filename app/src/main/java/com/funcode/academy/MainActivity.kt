package com.funcode.academy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.funcode.academy.ui.FuncodeAppUi
import com.funcode.academy.ui.theme.FuncodeTheme

/** Única actividad de la app; la navegación entre pantallas la gestiona Compose. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FuncodeApp).container
        setContent {
            FuncodeTheme {
                FuncodeAppUi(
                    progressRepository = container.progressRepository,
                    settingsRepository = container.settingsRepository,
                )
            }
        }
    }
}

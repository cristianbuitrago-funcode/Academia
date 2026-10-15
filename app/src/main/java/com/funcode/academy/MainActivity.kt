package com.funcode.academy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.funcode.academy.ui.FuncodeRoot

/** Única actividad de la app; la navegación entre pantallas la gestiona Compose. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FuncodeApp).container
        setContent {
            FuncodeRoot(
                progressRepository = container.progressRepository,
                settingsRepository = container.settingsRepository,
                contentRepository = container.contentRepository,
                codeRunner = container.codeRunner,
            )
        }
    }
}

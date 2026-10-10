package com.funcode.academy.ui.screens.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.funcode.academy.R
import com.funcode.academy.ui.components.ComingSoon

/** Mapa de la ruta de aprendizaje. Pendiente: Fases 3–4 (contenido) y 5 (progreso visual). */
@Composable
fun MapScreen() {
    ComingSoon(
        title = stringResource(R.string.map_title),
        description = stringResource(R.string.map_pending_description),
    )
}

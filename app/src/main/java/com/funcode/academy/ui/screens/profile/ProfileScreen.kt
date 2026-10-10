package com.funcode.academy.ui.screens.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.funcode.academy.R
import com.funcode.academy.ui.components.ComingSoon

/** Perfil, avatar y logros. Pendiente: Fase 5. */
@Composable
fun ProfileScreen() {
    ComingSoon(
        title = stringResource(R.string.profile_title),
        description = stringResource(R.string.profile_pending_description),
    )
}

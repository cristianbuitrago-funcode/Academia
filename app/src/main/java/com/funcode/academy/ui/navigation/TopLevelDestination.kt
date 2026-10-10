package com.funcode.academy.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.funcode.academy.R
import kotlinx.serialization.Serializable

// Rutas tipadas de Navigation Compose: cada pantalla es un objeto serializable.
@Serializable data object HomeRoute
@Serializable data object MapRoute
@Serializable data object ProfileRoute
@Serializable data object SettingsRoute

/** Destinos de la barra de navegación inferior. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    HOME(HomeRoute, R.string.nav_home, R.drawable.ic_home),
    MAP(MapRoute, R.string.nav_map, R.drawable.ic_map),
    PROFILE(ProfileRoute, R.string.nav_profile, R.drawable.ic_person),
    SETTINGS(SettingsRoute, R.string.nav_settings, R.drawable.ic_settings),
}

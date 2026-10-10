package com.funcode.academy.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.settings.SettingsRepository
import com.funcode.academy.ui.navigation.HomeRoute
import com.funcode.academy.ui.navigation.MapRoute
import com.funcode.academy.ui.navigation.ProfileRoute
import com.funcode.academy.ui.navigation.SettingsRoute
import com.funcode.academy.ui.navigation.TopLevelDestination
import com.funcode.academy.ui.screens.home.HomeRoute as HomeScreenRoute
import com.funcode.academy.ui.screens.map.MapScreen
import com.funcode.academy.ui.screens.profile.ProfileScreen
import com.funcode.academy.ui.screens.settings.SettingsRoute as SettingsScreenRoute

/** Estructura principal: barra de navegación inferior y contenido de la pantalla activa. */
@Composable
fun FuncodeAppUi(
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                // Evita acumular pantallas: "Atrás" desde cualquier pestaña vuelve a Inicio.
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        // El texto de la etiqueta ya describe el botón para TalkBack.
                        icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> { HomeScreenRoute(progressRepository) }
            composable<MapRoute> { MapScreen() }
            composable<ProfileRoute> { ProfileScreen() }
            composable<SettingsRoute> { SettingsScreenRoute(settingsRepository) }
        }
    }
}

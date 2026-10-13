package com.funcode.academy.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.navigation.toRoute
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.rpg.RewardEngine
import com.funcode.academy.domain.settings.SettingsRepository
import com.funcode.academy.ui.navigation.HomeRoute
import com.funcode.academy.ui.navigation.LessonDestination
import com.funcode.academy.ui.navigation.MapRoute
import com.funcode.academy.ui.navigation.ModuleDestination
import com.funcode.academy.ui.navigation.ProfileRoute
import com.funcode.academy.ui.navigation.SettingsRoute
import com.funcode.academy.ui.navigation.TopLevelDestination
import com.funcode.academy.ui.screens.home.HomeRoute as HomeScreenRoute
import com.funcode.academy.ui.screens.lesson.LessonRoute as LessonScreenRoute
import com.funcode.academy.ui.screens.module.ModuleListRoute
import com.funcode.academy.ui.screens.module.ModuleRoute
import com.funcode.academy.ui.screens.profile.ProfileRoute as ProfileScreenRoute
import com.funcode.academy.ui.screens.settings.SettingsRoute as SettingsScreenRoute

/** Estructura principal: barra de navegación inferior y contenido de la pantalla activa. */
@Composable
fun FuncodeAppUi(
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    contentRepository: ContentRepository,
) {
    // Único motor de recompensas de la app (las llamadas simultáneas se ordenan dentro).
    val rewardEngine = remember(contentRepository, progressRepository) { RewardEngine(contentRepository, progressRepository) }
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
            composable<HomeRoute> {
                HomeScreenRoute(
                    progressRepository = progressRepository,
                    contentRepository = contentRepository,
                    rewardEngine = rewardEngine,
                    onOpenLesson = { lessonId -> navController.navigate(LessonDestination(lessonId)) },
                    onOpenModule = { moduleId -> navController.navigate(ModuleDestination(moduleId)) },
                )
            }
            composable<MapRoute> {
                ModuleListRoute(contentRepository, progressRepository) { moduleId ->
                    navController.navigate(ModuleDestination(moduleId))
                }
            }
            composable<ModuleDestination> { entry ->
                ModuleRoute(
                    moduleId = entry.toRoute<ModuleDestination>().moduleId,
                    content = contentRepository,
                    progress = progressRepository,
                    onOpenLesson = { lessonId -> navController.navigate(LessonDestination(lessonId)) },
                    onExit = { navController.popBackStack() },
                )
            }
            composable<ProfileRoute> { ProfileScreenRoute(progressRepository, rewardEngine) }
            composable<SettingsRoute> { SettingsScreenRoute(settingsRepository) }
            composable<LessonDestination> { entry ->
                LessonScreenRoute(
                    lessonId = entry.toRoute<LessonDestination>().lessonId,
                    contentRepository = contentRepository,
                    progressRepository = progressRepository,
                    settingsRepository = settingsRepository,
                    rewardEngine = rewardEngine,
                    onExit = { navController.popBackStack() },
                )
            }
        }
    }
}

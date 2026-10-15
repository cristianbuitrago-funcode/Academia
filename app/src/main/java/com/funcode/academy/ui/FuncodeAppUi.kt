package com.funcode.academy.ui

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.funcode.academy.domain.runner.CodeRunner
import com.funcode.academy.domain.rpg.RewardEngine
import com.funcode.academy.domain.settings.SettingsRepository
import com.funcode.academy.ui.components.FuturisticBackground
import com.funcode.academy.ui.components.neonBrush
import com.funcode.academy.ui.navigation.HomeRoute
import com.funcode.academy.ui.navigation.JavaLabDestination
import com.funcode.academy.ui.navigation.LessonDestination
import com.funcode.academy.ui.navigation.MapRoute
import com.funcode.academy.ui.navigation.MissionDestination
import com.funcode.academy.ui.navigation.MissionsDestination
import com.funcode.academy.ui.navigation.ModuleDestination
import com.funcode.academy.ui.navigation.ProfileRoute
import com.funcode.academy.ui.navigation.SettingsRoute
import com.funcode.academy.ui.navigation.TopLevelDestination
import com.funcode.academy.ui.screens.home.HomeRoute as HomeScreenRoute
import com.funcode.academy.ui.screens.lab.JavaLabRoute
import com.funcode.academy.ui.screens.lesson.LessonRoute as LessonScreenRoute
import com.funcode.academy.ui.screens.mission.MissionRoute
import com.funcode.academy.ui.screens.mission.MissionsRoute
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
    codeRunner: CodeRunner? = null,
) {
    // Único motor de recompensas de la app (las llamadas simultáneas se ordenan dentro).
    val rewardEngine = remember(contentRepository, progressRepository) { RewardEngine(contentRepository, progressRepository) }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    FuturisticBackground(Modifier.fillMaxSize()) {
        Scaffold(
            // Transparente: se ve el fondo de neón dibujado detrás.
            containerColor = Color.Transparent,
            // Con fondo transparente hay que indicar el color del texto (si no, sería negro).
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                Column {
                    // Línea de neón sobre la barra inferior.
                    Box(Modifier.fillMaxWidth().height(1.dp).background(neonBrush()))
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
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
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
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
                        onOpenMissions = { navController.navigate(MissionsDestination) },
                        onOpenMission = { missionId -> navController.navigate(MissionDestination(missionId)) },
                    )
                }
                composable<MapRoute> {
                    ModuleListRoute(
                        content = contentRepository,
                        progress = progressRepository,
                        onOpenModule = { moduleId -> navController.navigate(ModuleDestination(moduleId)) },
                        onOpenMissions = { navController.navigate(MissionsDestination) },
                    )
                }
                composable<MissionsDestination> {
                    MissionsRoute(contentRepository, progressRepository, rewardEngine) { missionId ->
                        navController.navigate(MissionDestination(missionId))
                    }
                }
                composable<MissionDestination> { entry ->
                    MissionRoute(
                        missionId = entry.toRoute<MissionDestination>().missionId,
                        content = contentRepository,
                        progress = progressRepository,
                        engine = rewardEngine,
                        onOpenActivity = { lessonId -> navController.navigate(LessonDestination(lessonId)) },
                        onExit = { navController.popBackStack() },
                    )
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
                composable<ProfileRoute> { ProfileScreenRoute(progressRepository) }
                composable<SettingsRoute> {
                    // El laboratorio de Java (prototipo Fase 8) solo se ofrece en compilaciones de depuración.
                    val debuggable = (LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                    SettingsScreenRoute(
                        settingsRepository = settingsRepository,
                        onOpenJavaLab = if (codeRunner != null && debuggable) {
                            { navController.navigate(JavaLabDestination) }
                        } else {
                            null
                        },
                    )
                }
                if (codeRunner != null) {
                    composable<JavaLabDestination> { JavaLabRoute(codeRunner) }
                }
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
}

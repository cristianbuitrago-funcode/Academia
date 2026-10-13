# Funcode Academy: componentes del proyecto (fases 2 a 7)

Qué hace cada pieza y por qué existe. Este documento se ampliará en cada fase.

## Configuración de Gradle

| Archivo | Función |
|---|---|
| `settings.gradle.kts` | Declara los módulos (`:app`, `:core:domain`) y los repositorios de dependencias. Con `-Pfuncode.domainOnly=true` incluye solo el dominio, para poder probarlo sin Android SDK. |
| `build.gradle.kts` (raíz) | Declara todos los plugins (Android, Kotlin, KSP, Room) una sola vez, para que se carguen juntos y con la misma versión. |
| `build-domain-only.gradle.kts` | Archivo raíz alternativo del modo solo dominio: no declara plugins de Android. |
| `gradle/libs.versions.toml` | **Catálogo de versiones**: único lugar donde se escriben las versiones de las bibliotecas. |
| `gradle/wrapper/*`, `gradlew` | *Gradle Wrapper*: descarga y usa siempre Gradle 9.6.1 (con suma de comprobación SHA-256), así todos compilan igual. |
| `gradle.properties` | Opciones globales (memoria, caché, AndroidX). |
| `.github/workflows/ci.yml` | Integración continua: pruebas, lint, APK, control del esquema de Room y resumen de resultados. |

## `:core:domain` (Kotlin puro, sin Android)

| Archivo | Función |
|---|---|
| `model/Audience.kt` | Públicos (`KIDS`, `TEENS`, `ADULTS`). En el MVP solo se pueden elegir `TEENS` y `ADULTS`. |
| `model/ModuleStatus.kt` | Estados de un módulo de la ruta (bloqueado, disponible, completado, dominado). Se usará en las fases 4–5. |
| `progress/XpSource.kt` | Identifica una actividad que concede XP y valida que el XP sea positivo. |
| `progress/ProgressRepository.kt` | **Contrato** para leer y registrar XP. El dominio no sabe que existe Room. |
| `settings/SettingsRepository.kt`, `settings/LearningPreferences.kt` | Contrato y modelo de las preferencias: público, tema, tamaño de texto y pistas. |
| `progress/ExerciseHistory.kt` | Historial de intentos de cada ejercicio (deducido de `exercise_attempt`). |
| `learning/StudyAdvisor.kt` | Reglas sencillas de recomendación: retomar, repasar y repetir la evaluación. |
| `progress/ExerciseAttempt.kt` | Intento comprobado de un ejercicio (sin el texto de la respuesta). `ProgressRepository.recordAttempt` lo guarda. |
| `lesson/Lesson.kt` | Modelo de lección: objetivos, secciones de teoría y código (con comentarios por línea), variantes de presentación, versión, estado de revisión y evaluación opcional. |
| `lesson/Exercise.kt` | Datos comunes de un ejercicio y el contenido de cada uno de los **7 tipos**. |
| `lesson/Answer.kt` | Respuestas del estudiante y resultados: correcta, incorrecta (con aciertos parciales) o incompleta. |
| `lesson/AnswerValidator.kt` | **Comprueba** respuestas con las reglas del contenido. Función pura: no ejecuta código. |
| `lesson/CodeText.kt` | Normaliza texto: *tokens* de Java para los huecos y salidas de programa sin espacios finales. |
| `lesson/LessonParser.kt`, `lesson/json/` | Lee el JSON del contenido y lo valida por completo; devuelve todos los errores en lugar de fallar. |
| `lesson/LessonSession.kt` | Motor de la sesión (ejercicio actual, pistas, intentos, resumen) y contrato `ContentRepository`. En una evaluación: una respuesta por pregunta, sin pistas y resultado `AssessmentScore`. |
| `lesson/Module.kt` | Módulo del curso (`CourseModule`: lecciones, evaluación y reto) y `ModuleAssembler`, que lo arma a partir de su definición y comprueba sus referencias. |
| `rpg/RewardRules.kt` | Valores de XP aprobados (P6): por ejercicio según la dificultad, penalización por pistas, lección y evaluación. |
| `rpg/LevelCurve.kt` | Curva de niveles (100 × n) y progreso hacia el siguiente nivel, calculados desde el XP total. |
| `rpg/Achievement.kt` | Logros con id estable y condición de desbloqueo sobre `LearningState`. |
| `rpg/RewardCalculator.kt` | Cálculo puro de las recompensas que corresponden a los resultados verificados y del estado de cada misión. |
| `mission/Mission.kt` | Misión de contenido: tipo, objetivo, requisitos, tareas vinculadas, estados y avance de cada tarea. |
| `mission/MissionAssembler.kt` | Valida las referencias, tipos y requisitos (sin ciclos) de las misiones y descarta las inválidas. |
| `rpg/RewardEngine.kt` | Único punto que concede XP y logros: recalcula desde lo guardado y registra lo que falta (idempotente). |
| `progress/ModuleProgress.kt` | Progreso de un módulo **derivado** de los ejercicios resueltos y del mejor resultado: lecciones completadas, estado del reto (bloqueo y pasos) y siguiente actividad. |

## `:app` (Android)

| Archivo | Función |
|---|---|
| `FuncodeApp.kt` | Clase `Application`: se crea al arrancar el proceso y construye el `AppContainer`. |
| `AppContainer.kt` | **Inyección de dependencias manual**: crea Room y DataStore una sola vez y los entrega como interfaces del dominio. |
| `MainActivity.kt` | Única actividad; muestra la interfaz Compose. |
| `data/local/ProgressEntities.kt` | Tablas de Room (esquema inicial del plan, §6.3). `xp_ledger` tiene un índice **único** sobre `(sourceType, sourceId)`. |
| `data/local/XpLedgerDao.kt` | Consultas SQL del registro de XP. `insert` ignora los duplicados y devuelve -1; `observeAll` da los movimientos. |
| `data/local/FuncodeDatabase.kt` | Define la base de datos `funcode.db` (versión 1). |
| `data/progress/RoomProgressRepository.kt` | Implementa `ProgressRepository` con Room. |
| `data/settings/DataStoreSettingsRepository.kt` | Implementa `SettingsRepository` con DataStore (archivo `settings.preferences_pb`). |
| `data/content/AssetContentRepository.kt` | Implementa `ContentRepository` leyendo `assets/content/es/java` (sin conexión). |
| `data/local/ExerciseAttemptDao.kt` | Inserta y consulta intentos en `exercise_attempt`; también da los ejercicios resueltos. |
| `data/local/AchievementDao.kt` | Guarda logros en `achievement_unlocked` (clave primaria: nunca se duplican). |
| `data/local/ModuleProgressDao.kt` | Guarda el mejor resultado de la evaluación de cada módulo en `module_progress` (transacción de lectura y escritura). |
| `ui/progress/LearningProgress.kt` | Combina módulos, ejercicios resueltos y resultados en el progreso de cada módulo. |
| `ui/rpg/*` | Tarjeta de nivel con barra de progreso, aviso de recompensas y textos de los logros. |
| `ui/screens/profile/*` | Perfil: nivel, XP, resumen de recompensas por tipo (también misiones) y logros con su fecha. |
| `ui/screens/mission/*` | Misiones: lista con estado y detalle con objetivo, requisitos, tareas enlazadas a sus actividades, recompensa y resultado. |
| `ui/progress/MissionsProgress.kt` | Recalcula el estado de las misiones cuando cambia cualquier resultado guardado. |
| `ui/screens/module/*` | Mapa (lista de módulos) y pantalla de módulo (lecciones en orden, evaluación y reto, cerrado o abierto). |
| `assets/content/es/java/` | Contenido educativo en JSON (índice, módulos y una lección por archivo). Formato: `docs/contenido/GUIA_AUTORES.md`. |
| `ui/screens/lesson/LessonViewModel.kt` | Carga la lección, mantiene la sesión del dominio y registra intentos. |
| `ui/screens/lesson/LessonScreen.kt` | Introducción (objetivos, teoría, ejemplo), ejercicio, retroalimentación y resumen (con el resultado si es una evaluación). |
| `ui/screens/lesson/ExerciseInputs.kt` | Controles de respuesta de cada tipo (opciones, huecos, flechas para ordenar, salida, líneas, desplegables). |
| `ui/components/CodeBlock.kt`, `DraftBanner.kt` | Bloque de código monoespaciado con números de línea; aviso de borrador. |
| `ui/theme/Theme.kt` | Tema Material 3 claro y oscuro (paleta provisional, P10), preferencia de tema y escala de texto. |
| `ui/navigation/TopLevelDestination.kt` | Rutas tipadas y destinos de la barra inferior. |
| `ui/FuncodeAppUi.kt` | `Scaffold` con la barra de navegación y el `NavHost` que cambia de pantalla. |
| `ui/screens/home/*` | Inicio: nivel y XP **calculados desde Room**, tarjeta «Continuar aprendiendo» y sincronización de recompensas pendientes al abrir. |
| `ui/screens/settings/*` | Ajustes: público (niños, adolescentes o adultos), tema, tamaño de texto, pistas y restablecer preferencias. |
| `ui/FuncodeRoot.kt` | Raíz de la interfaz: aplica el tema y el tamaño de texto elegidos. |
| `ui/progress/Recommendations.kt` | Recalcula las recomendaciones cuando cambian los resultados guardados. |
| `res/values/strings.xml` | Todos los textos visibles, en español. |
| `res/xml/*backup*` | Reglas de copia de seguridad: solo la base de datos y los ajustes. |
| `schemas/` | Esquema de Room exportado (se compara en CI con el generado). |

## Patrón seguido por cada pantalla

`XxxRoute` (obtiene el *ViewModel*) → `XxxViewModel` (convierte los datos del repositorio en estado con `StateFlow`) → `XxxScreen` (dibuja el estado; no conoce la base de datos, así que se puede probar con datos falsos).

## Pruebas

| Ubicación | Qué comprueba | Cómo se ejecuta |
|---|---|---|
| `core/domain/src/test` | Reglas del dominio (JUnit puro) | `./gradlew :core:domain:test` |
| `app/src/test/.../data/RoomProgressRepositoryTest.kt` | Room real: el XP no se duplica, mejor resultado de la evaluación, progreso tras cerrar y reabrir la base de datos | Robolectric: `./gradlew :app:testDebugUnitTest` |
| `app/src/test/.../data/DataStoreSettingsRepositoryTest.kt` | El ajuste persiste al cerrar y reabrir DataStore | JVM: misma tarea |
| `app/src/test/.../ui/NavigationTest.kt` | Navegación, botón Atrás, recreación y ajustes en la interfaz | Robolectric + Compose: misma tarea |
| `core/domain/.../lesson/*Test.kt` | Validadores, parser, sesión y coherencia del contenido real | `./gradlew :core:domain:test` |
| `core/domain/.../lesson/JavaContentCompilationTest.kt` | Compila con `javac --release 17` el Java del contenido y compara las salidas declaradas (en la JVM de las pruebas, no en la app) | `./gradlew :core:domain:test` |
| `app/src/test/.../ui/a11y/*` | Preferencias aplicadas, roles y descripciones, texto al 260 % y contraste | Robolectric / JVM |
| `core/domain/.../learning/StudyAdvisorTest.kt` | Reglas de recomendación | `./gradlew :core:domain:test` |
| `core/domain/.../mission/*Test.kt` | Lectura, validación, estados y transiciones de las misiones | `./gradlew :core:domain:test` |
| `app/src/test/.../ui/mission/MissionsViewModelTest.kt` | Pantalla de misiones con el contenido real | JVM |
| `core/domain/.../rpg/*Test.kt` | Reglas de XP, niveles, logros y motor de recompensas (con el contenido real) | `./gradlew :core:domain:test` |
| `app/src/test/.../data/RewardPersistenceTest.kt` | Motor de recompensas con Room en archivo: sin duplicados y recuperación tras reabrir | Robolectric |
| `app/src/test/.../ui/profile/ProfileStateTest.kt` | Estado del perfil a partir del registro | JVM |
| `core/domain/.../progress/ModuleProgressTest.kt` | Lecciones completadas y siguiente actividad | `./gradlew :core:domain:test` |
| `app/src/test/.../data/AssetContentRepositoryTest.kt` | Lectura del contenido real de `assets` | Robolectric |
| `app/src/test/.../ui/lesson/LessonViewModelTest.kt` | Recorrido completo de la lección y registro de intentos | JVM |
| `app/src/test/.../ui/lesson/LessonFlowTest.kt` | Inicio/Mapa → módulo → lección o evaluación → ejercicio → retroalimentación | Robolectric + Compose |

Robolectric simula Android en la JVM: es rápido, pero **no sustituye** la prueba en emulador o dispositivo.

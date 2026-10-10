# Funcode Academy: componentes del proyecto (Fase 2)

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
| `settings/SettingsRepository.kt` | Contrato para los ajustes del estudiante. |

## `:app` (Android)

| Archivo | Función |
|---|---|
| `FuncodeApp.kt` | Clase `Application`: se crea al arrancar el proceso y construye el `AppContainer`. |
| `AppContainer.kt` | **Inyección de dependencias manual**: crea Room y DataStore una sola vez y los entrega como interfaces del dominio. |
| `MainActivity.kt` | Única actividad; muestra la interfaz Compose. |
| `data/local/ProgressEntities.kt` | Tablas de Room (esquema inicial del plan, §6.3). `xp_ledger` tiene un índice **único** sobre `(sourceType, sourceId)`. |
| `data/local/XpLedgerDao.kt` | Consultas SQL del registro de XP. `insert` ignora los duplicados y devuelve -1. |
| `data/local/FuncodeDatabase.kt` | Define la base de datos `funcode.db` (versión 1). |
| `data/progress/RoomProgressRepository.kt` | Implementa `ProgressRepository` con Room. |
| `data/settings/DataStoreSettingsRepository.kt` | Implementa `SettingsRepository` con DataStore (archivo `settings.preferences_pb`). |
| `ui/theme/Theme.kt` | Tema Material 3 claro y oscuro (paleta provisional, P10). |
| `ui/navigation/TopLevelDestination.kt` | Rutas tipadas y destinos de la barra inferior. |
| `ui/FuncodeAppUi.kt` | `Scaffold` con la barra de navegación y el `NavHost` que cambia de pantalla. |
| `ui/screens/home/*` | Inicio: muestra el XP total **leído de Room** (hoy siempre 0, porque aún no hay ejercicios). |
| `ui/screens/settings/*` | Ajustes: elegir el público (adolescente/adulto), guardado en DataStore. |
| `ui/screens/map`, `ui/screens/profile` | Pantallas pendientes: muestran "Disponible en una próxima versión". |
| `ui/components/ComingSoon.kt` | Componente reutilizable para ese estado vacío. |
| `res/values/strings.xml` | Todos los textos visibles, en español. |
| `res/xml/*backup*` | Reglas de copia de seguridad: solo la base de datos y los ajustes. |
| `schemas/` | Esquema de Room exportado (se compara en CI con el generado). |

## Patrón seguido por cada pantalla

`XxxRoute` (obtiene el *ViewModel*) → `XxxViewModel` (convierte los datos del repositorio en estado con `StateFlow`) → `XxxScreen` (dibuja el estado; no conoce la base de datos, así que se puede probar con datos falsos).

## Pruebas

| Ubicación | Qué comprueba | Cómo se ejecuta |
|---|---|---|
| `core/domain/src/test` | Reglas del dominio (JUnit puro) | `./gradlew :core:domain:test` |
| `app/src/test/.../data/RoomProgressRepositoryTest.kt` | Room real en memoria: el XP no se duplica | Robolectric: `./gradlew :app:testDebugUnitTest` |
| `app/src/test/.../data/DataStoreSettingsRepositoryTest.kt` | El ajuste persiste al cerrar y reabrir DataStore | JVM: misma tarea |
| `app/src/test/.../ui/NavigationTest.kt` | Navegación, botón Atrás, recreación y ajustes en la interfaz | Robolectric + Compose: misma tarea |

Robolectric simula Android en la JVM: es rápido, pero **no sustituye** la prueba en emulador o dispositivo.

# Funcode Academy — Registro de estado del proyecto

_Última actualización: 2026-10-10_

**Significado de los estados**
- **Planificado:** descrito en un documento; no hay código.
- **Implementado:** el código existe en el repositorio.
- **Probado:** pasó pruebas cuya evidencia está registrada (dónde y cuándo).
- **Aprobado:** el responsable del proyecto lo revisó y lo aceptó.
- **Pendiente:** falta una decisión, una tarea o una verificación.

---

## Fases

| Fase | Estado | Notas |
|---|---|---|
| 1. Planificación y arquitectura | **Aprobado** (2026-10-10) | D1–D8, P1 y P3 resueltas. |
| 2. Esqueleto y CI | **Implementado y probado en CI — pendiente de verificación en dispositivo y de aprobación** | Ver detalle abajo. Faltan los criterios 17–18 (verificación manual). |
| 3–9 | Planificado — sin autorizar | — |

## Decisiones

| Decisión | Estado |
|---|---|
| D1–D8 | Aprobado (ver §0 del plan técnico) |
| P1 Rama `main` + flujo con *pull requests* | Aprobado. `main` creada; ver `docs/FLUJO_DE_TRABAJO.md` |
| P3 Robolectric | Aprobado con condiciones. Incorporado en la Fase 2 (justificación abajo) |
| P4 Permiso para subir *workflows* | Resuelto: el *push* de `.github/workflows/ci.yml` funcionó |
| P2, P5–P10 | Pendiente (se deciden en su fase) |

---

## Fase 2 — Detalle

### Versiones fijadas (`gradle/libs.versions.toml`)

Verificadas el 2026-10-10 en las páginas oficiales de versiones estables (developer.android.com, Maven Central, GitHub de KSP) y **confirmadas por la compilación real en CI**.

| Componente | Versión | Nota |
|---|---|---|
| Gradle (*wrapper*) | 9.6.1 | Suma SHA-256 de la distribución fijada. Mínimo exigido por AGP 9.4. |
| Android Gradle Plugin | 9.4.0 | Kotlin integrado ("built-in Kotlin"); no se aplica `kotlin-android`. |
| Kotlin | 2.4.21 | |
| KSP | 2.3.12 | Necesario para generar el código de Room. |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 | `targetSdk` 36 para que Robolectric simule la misma versión que la app declara. |
| Compose BOM | 2026.09.00 | Compose UI 1.12.1, Material 3 1.4.0. |
| Room / DataStore / Navigation / Lifecycle / Activity / Core | 2.8.5 / 1.2.1 / 2.10.2 / 2.11.0 / 1.13.0 / 1.19.1 | |
| Coroutines / kotlinx.serialization | 1.10.2 / 1.11.0 | |
| JUnit / AndroidX Test / Robolectric | 4.13.2 / 1.7.0 + 1.3.0 / 4.17 | |

### Justificación de Robolectric (condición de P3)

Aporta valor comprobable: permite probar **Room real** (la restricción única del XP), y la **navegación y la interfaz** con Compose en la JVM, en cada *push*, sin emulador. Las pruebas de DataStore y de `:core:domain` no lo usan. **No sustituye** la verificación en emulador o dispositivo.

### Componentes

| Componente | Implementado | Probado | Evidencia | Aprobado |
|---|---|---|---|---|
| Estructura Gradle (`:app`, `:core:domain`), catálogo y *wrapper* | ✔ | ✔ | CI ejecución 38077143387 | — |
| `:core:domain`: modelos (`Audience`, `ModuleStatus`, `XpSource`, `XpAward`) e interfaces de repositorio | ✔ | ✔ 7 pruebas | Contenedor (Gradle 8.14.3, JDK 21) y CI | — |
| Tema Material 3 claro/oscuro (paleta provisional) | ✔ | Parcial (se renderiza en las pruebas de interfaz) | CI | — |
| Navegación inferior Inicio/Mapa/Perfil/Ajustes, botón Atrás, restauración de estado | ✔ | ✔ 6 pruebas Robolectric | CI | — |
| Pantallas Mapa y Perfil (estado vacío "Disponible en una próxima versión") | ✔ | ✔ | CI | — |
| Inicio con XP total leído de Room (hoy siempre 0) | ✔ | ✔ | CI | — |
| Ajustes: elegir público, guardado en DataStore | ✔ | ✔ 4 pruebas de DataStore + 1 de interfaz | CI | — |
| Room: esquema inicial (7 tablas), índice único en `xp_ledger`, esquema exportado a Git | ✔ | ✔ 5 pruebas | CI | — |
| CI en GitHub Actions (pruebas, lint, APK, control del esquema, resumen) | ✔ | ✔ | CI | — |
| Ejecución en emulador/dispositivo | — | **Pendiente** (responsable) | — | — |

**No implementado en esta fase (intencionadamente):** motor de lecciones, contenido, ejercicios, XP real por actividades, avatar, logros, *onboarding* (el público se elige en Ajustes), ejecución de Java.

### Criterios de aceptación de la Fase 2 (§13.1 del plan)

| # | Criterio | Estado |
|---|---|---|
| 1 | Módulos `:app` y `:core:domain` (sin Android) | ✔ Cumplido (CI) |
| 2 | `com.funcode.academy`, `minSdk 26` | ✔ Cumplido |
| 3 | Versiones estables en el catálogo; *wrapper* incluido | ✔ Cumplido |
| 4 | Sin dependencias de pago ni permiso `INTERNET` | ✔ Cumplido (el manifiesto no declara permisos)\* |
| 5 | Material 3 claro/oscuro; tamaño de fuente del sistema | ✔ Implementado; verificación visual pendiente (responsable) |
| 6 | Navegación entre 4 pantallas; Atrás correcto | ✔ Cumplido (pruebas Robolectric) |
| 7 | Pantallas pendientes con estado vacío honesto | ✔ Cumplido |
| 8 | Textos en `strings.xml` | ✔ Cumplido |
| 9 | Descripciones de accesibilidad | ✔ Implementado (etiquetas, encabezados, grupos de opciones, objetivos ≥ 48 dp); revisión con TalkBack pendiente |
| 10 | Room con esquema inicial, `UNIQUE` en `xp_ledger`, esquema en Git | ✔ Cumplido |
| 11 | Prueba: el XP no se duplica | ✔ Cumplido (3 pruebas) |
| 12 | DataStore guarda un ajuste real y una prueba lo comprueba | ✔ Cumplido |
| 13 | Modelos e interfaces del dominio con pruebas | ✔ Cumplido |
| 14 | *Workflow* de CI en cada *push*/PR | ✔ Cumplido |
| 15 | CI en verde con enlace registrado | ✔ Cumplido: https://github.com/cristianbuitrago-funcode/Academia/actions/runs/38077143387 |
| 16 | APK de depuración como artefacto | ✔ Cumplido |
| 17 | Abrir en Android Studio y ejecutar en API ≥ 26 | **Pendiente** (responsable) |
| 18 | Navegar, rotar y cerrar/reabrir sin cierres; ajuste conservado | **Pendiente** (responsable). Robolectric lo cubre parcialmente; no lo sustituye. |
| 19 | `ESTADO_PROYECTO.md` actualizado | ✔ Este documento |
| 20 | Explicación de componentes | ✔ `docs/COMPONENTES.md` |

\* Algunas bibliotecas pueden añadir permisos al manifiesto final al fusionarse. No se ha inspeccionado el manifiesto fusionado del APK; queda como comprobación pendiente (en Android Studio: *Merged Manifest*).

---

## Pruebas ejecutadas

| Fecha | Dónde | Prueba | Resultado |
|---|---|---|---|
| 2026-10-10 | Contenedor | Inspección del entorno | JDK 21 y Gradle 8.14.3; sin Android SDK; `dl.google.com` y `downloads.gradle.org` bloqueados; Maven Central con límite de peticiones (429). |
| 2026-10-10 | Contenedor | `:core:domain:test` (modo solo dominio, Gradle 8.14.3 local, réplica `repo1.maven.org` mediante *init script* fuera del repositorio) | **7/7 superadas** |
| 2026-10-10 | CI #1–#2 | Compilación completa | **Fallo** de configuración: el plugin de Kotlin no veía las clases de AGP (`NoClassDefFoundError: BaseVariant`). Corregido declarando los plugins en la raíz. |
| 2026-10-10 | CI #3 | Pruebas de `:app` | **Fallo**: Robolectric necesita JDK 21 para Android 15+ (`UnsupportedOperationException`). Corregido pasando el CI a JDK 21. |
| 2026-10-10 | CI #4 | Pruebas de `:app` | **11/15 fallos**: Robolectric con Android 16 necesita acceso a `jdk.internal.access`. Corregido con `--add-exports/--add-opens` en las pruebas. |
| 2026-10-10 | CI #5 | Control del esquema de Room | **Fallo esperado**: el esquema aún no estaba en Git. Se guardó a partir de la salida del CI. |
| 2026-10-10 | CI 38076992682 | Todo | **Verde** |
| 2026-10-10 | CI 38077143387 | Todo + resumen | **Verde: 22/22 pruebas** (7 dominio, 5 Room, 4 DataStore, 6 interfaz); lint sin errores, **1 aviso**; APK generado |

## Errores conocidos

Ninguno abierto en el código.

## Limitaciones y avisos conocidos

- **Lint `OldTargetApi`:** `targetSdk` 36 es menor que `compileSdk` 37. Es intencionado (Robolectric simula la misma versión que declara la app). Se revisará cuando Robolectric y el resto de pruebas soporten 37.
- **Aviso de GitHub:** `actions/upload-artifact@v4` usa Node.js 20 (obsoleto, de momento solo un aviso). Se actualizará cuando se confirme una versión estable más reciente.
- **API de pruebas obsoleta evitada:** se usa `createAndroidComposeRule` v2 (la v1 está marcada como obsoleta).
- **Contenedor de Claude:** no puede compilar Android; solo puede probar `:core:domain` en modo solo dominio. El APK y las pruebas de `:app` se verifican en CI.
- **Ramas `main` y `Main`:** existen ambas en GitHub (la segunda no la creó Claude). Generan conflictos en Windows/macOS. Ver `docs/FLUJO_DE_TRABAJO.md`.
- **Pruebas en caché:** GitHub Actions reutiliza resultados de pruebas cuando el código no cambia ("FROM-CACHE"); el resumen muestra los recuentos de esos resultados.

## Acciones pendientes del responsable

1. Verificación manual (criterios 17–18) siguiendo `docs/CONFIGURACION_ENTORNO.md` §4.
2. Borrar la rama `Main` duplicada y cambiar la rama por defecto a `main` (`docs/FLUJO_DE_TRABAJO.md`).
3. Revisar la Fase 2 y decidir si se abre el *pull request* hacia `main`.

## Próximos pasos autorizados

Ninguno. La Fase 3 espera autorización explícita.

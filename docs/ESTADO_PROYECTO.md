# Funcode Academy — Registro de estado del proyecto

_Última actualización: 2026-10-15_

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
| 2. Esqueleto y CI | **Aprobado** (2026-10-13, según el responsable: verificado manualmente en Android Studio) | El *pull request* #1 sigue abierto: `main` aún no contiene la Fase 2. Los detalles de la verificación manual (resolución del fallo de Room y del emulador en su equipo) no constan en este registro. |
| 3. Motor de lecciones y ejercicios | **Aprobado** (2026-10-13, por el responsable) | Ver «Fase 3 — Detalle». El contenido sigue en borrador. |
| 4. Primer módulo de Java y evaluación | **Aprobado** (2026-10-13, por el responsable) | Ver «Fase 4 — Detalle». El contenido sigue en borrador. |
| 5. RPG básico: XP, niveles y logros | **Aprobado** (2026-10-13, por el responsable) | Ver «Fase 5 — Detalle». |
| 6. Misiones, retos y proyectos | **Aprobado** (2026-10-13, por el responsable) | Ver «Fase 6 — Detalle». Misiones y proyecto en borrador. |
| 7. Personalización, accesibilidad y adaptación | **Implementado y probado en CI (Linux y Windows) — pendiente de verificación en Android Studio (con TalkBack) y de aprobación** | Ver «Fase 7 — Detalle». Variantes de la lección 1 en borrador. |
| Corrección: cierre al abrir | **Implementado y probado** (emuladores API 26 y 34 en CI) — pendiente de confirmación en tu teléfono | Ver «Corrección del cierre al abrir y fase visual — Detalle». |
| Fase visual «Academia futurista» (pedida por el responsable el 2026-10-15; no estaba en el plan original) | **Implementado y probado en CI** — pendiente de tu revisión visual y aprobación | Paleta de neón. Ver el detalle. |
| 8. Ejecución real de Java | **Prototipo de verificación implementado y probado en emuladores de CI** (CI 38686836314, todo en verde). **No integrado en lecciones ni declarado seguro. Pendiente de tus decisiones F8-P1 a F8-P4**, sobre todo Android 8–12, donde el compilador no arranca. | Ver `docs/FASE8_EJECUCION_JAVA.md` §8–§10 y `docs/LICENCIAS_TERCEROS.md`. |
| 9 | Planificado — sin autorizar | — |

## Decisiones

| Decisión | Estado |
|---|---|
| D1–D8 | Aprobado (ver §0 del plan técnico). **D3 modificada en la Fase 7:** la presentación infantil se puede elegir (preferencia voluntaria, sin pedir la edad). |
| P1 Rama `main` + flujo con *pull requests* | Aprobado. `main` creada; ver `docs/FLUJO_DE_TRABAJO.md` |
| P3 Robolectric | Aprobado con condiciones. Incorporado en la Fase 2 (justificación abajo) |
| P4 Permiso para subir *workflows* | Resuelto: el *push* de `.github/workflows/ci.yml` funcionó |
| P6 Valores de XP, curva de niveles y umbral | **Aprobado** (2026-10-13): propuesta A de XP con penalización por pistas, curva 100×n y umbral del 80 %. Ver «Fase 5 — Detalle». El umbral no bloquea lecciones ni módulos. |
| Fase 6: retos, misiones y proyectos | **Aprobado** (2026-10-13): proyecto guiado por módulo (abierto al completar las lecciones, 50 XP una vez); misiones de contenido con requisitos declarados y 10 XP de bonificación una vez; pantalla Misiones desde Inicio y Mapa. Sustituye a la decisión anterior de misiones fijas sin XP. |
| Monedas virtuales | **No implementadas** por decisión del responsable (2026-10-13); propuesta documentada en «Fase 5 — Detalle». |
| P7 Versión de Java del contenido | **Aprobado (2026-10-15, decisión F8-D5): Java 17** para los ejemplos y el prototipo de la Fase 8. Las pruebas ya usan `javac --release 17`. |
| P8 Comprobación automática del Java del contenido | Implementada en la Fase 4 (`JavaContentCompilationTest`); pendiente de tu aprobación. |
| F8-D1 a F8-D8 (Fase 8) | **Aprobadas con condiciones (2026-10-15):** alternativa A **solo como prototipo de verificación** (B como plan alternativo); ECJ y D8/R8 **en evaluación** (versiones y licencias por verificar); `:core:javarunner` y el servicio aislado solo para el prototipo; umbrales de §3 del estudio; Java 17; el intento se registra al terminar la ejecución, sin XP por ejecutar ni por errores de compilación; `Scanner` aplazado; servidor externo solo documentado. **No autorizado:** integrar en lecciones, declararlo seguro, publicar ni pasar de fase. |
| P2, P5, P9, P10 | Pendiente (se deciden en su fase) |

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
| 14 | *Workflow* de CI en cada *push*/PR | ✔ Cumplido (Linux: pruebas, lint, APK; Windows: pruebas) |
| 15 | CI en verde con enlace registrado | ✔ Cumplido: https://github.com/cristianbuitrago-funcode/Academia/actions/runs/38077143387 |
| 16 | APK de depuración como artefacto | ✔ Cumplido |
| 17 | Abrir en Android Studio y ejecutar en API ≥ 26 | Parcial: el proyecto se abre y sincroniza; **bloqueado** el arranque del emulador |
| 18 | Navegar, rotar y cerrar/reabrir sin cierres; ajuste conservado | **Pendiente** (responsable). Robolectric lo cubre parcialmente; no lo sustituye. |
| 19 | `ESTADO_PROYECTO.md` actualizado | ✔ Este documento |
| 20 | Explicación de componentes | ✔ `docs/COMPONENTES.md` |

\* Algunas bibliotecas pueden añadir permisos al manifiesto final al fusionarse. No se ha inspeccionado el manifiesto fusionado del APK; queda como comprobación pendiente (en Android Studio: *Merged Manifest*).

---

## Fase 3 — Detalle

### Discrepancias encontradas al empezar (documentadas antes de programar)

1. **Estado de la Fase 2:** el registro la marcaba como pendiente; el responsable la declaró aprobada el 2026-10-13. El PR #1 no se ha fusionado, así que la Fase 3 se desarrolla en la misma rama (`ccr-db27c4de-czvzgb`) y el PR #1 incluye ahora las fases 2 y 3.
2. **Tipos de ejercicio:** el plan (§6.2) y D2 incluían una respuesta escrita validada con reglas estructurales (`STRUCTURAL_CODE`) y no incluían verdadero/falso. La autorización de la Fase 3 enumera siete tipos con **verdadero/falso** y sin `STRUCTURAL_CODE`. Se implementan los siete autorizados; `STRUCTURAL_CODE` queda **sin implementar, pendiente de decisión (F3-D1)**.
3. **Tablas `exercise_progress` y `lesson_progress`:** su contenido se puede deducir de `exercise_attempt`. Siguiendo la regla de no guardar datos redundantes, solo se escribe en `exercise_attempt`; esas tablas siguen sin uso y **no se ha cambiado el esquema** (sigue en versión 1, sin migración). Su eliminación queda pendiente de decisión (F3-D2).

### Componentes

| Componente | Implementado | Probado | Evidencia |
|---|---|---|---|
| Modelo de lección (`Lesson`, objetivos, secciones de teoría y código con comentarios por línea, variantes de presentación, versión, estado de revisión, evaluación opcional) | ✔ | ✔ | `LessonParserTest`, `DemoLessonContentTest` |
| Modelo de ejercicio común (id estable, instrucciones, dificultad, pistas, explicación, retroalimentación) | ✔ | ✔ | idem |
| Validación de los 7 tipos (`AnswerValidator`): correcta / incorrecta / incompleta | ✔ | ✔ 25 pruebas | `AnswerValidatorTest` |
| Normalización (tokens de Java para huecos; saltos de línea y espacios finales para salidas) | ✔ | ✔ 5 pruebas | `CodeTextTest` |
| Lectura del JSON con validación completa (errores acumulados, nunca excepciones por contenido) | ✔ | ✔ 22 pruebas | `LessonParserTest` |
| Sesión de ejercicios y resumen (`LessonSession`, `SessionSummary`) | ✔ | ✔ 8 pruebas | `LessonSessionTest` |
| Lección demostrativa `java.m02.l01` (borrador) | ✔ | ✔ 5 pruebas + `javac` | `DemoLessonContentTest` (sustituida en la Fase 4 por `ModuleContentTest`); verificación manual con `javac` 21 (ver abajo) |
| Repositorio de contenido en `assets` (sin conexión) | ✔ | ✔ 4 pruebas | `AssetContentRepositoryTest` (Robolectric) |
| Registro de intentos en `exercise_attempt` (sin guardar el texto de la respuesta) | ✔ | ✔ 1 prueba nueva | `RoomProgressRepositoryTest` |
| `LessonViewModel` (carga, ejercicios, pistas, reintentos, resumen) | ✔ | ✔ 7 pruebas | `LessonViewModelTest` |
| Pantalla de lección (introducción, ejercicios, retroalimentación, resumen) y acceso desde Inicio | ✔ | ✔ 3 pruebas de flujo | `LessonFlowTest` (Robolectric) |
| Verificación en Android Studio / dispositivo | — | **Pendiente** (responsable) | — |

**No implementado (fuera del alcance de la Fase 3):** XP por ejercicios, desbloqueo de módulos, mapa de módulos, misiones, variantes por edad, `CodeRunner` y cualquier ejecución de Java, `STRUCTURAL_CODE`.

### Contenido en borrador

| Bloque | Estado | Verificación técnica |
|---|---|---|
| `java.m02.l01` «Tu primer programa: mostrar texto en pantalla» (1 objetivo, 7 ejercicios, uno de cada tipo, dificultad 1→4) | **DRAFT — pendiente de tu revisión** | 2026-10-13, contenedor, `javac`/`java` 21.0.12: `HolaMundo`, `Ejemplo`, `Saludo` (huecos completados) y `Contar` (orden correcto) compilan y producen exactamente la salida declarada; `Mensaje` falla con `Mensaje.java:4: error: ';' expected`, la línea que marca el ejercicio. |

### Decisiones que requieren aprobación

| # | Cuestión | Propuesta |
|---|---|---|
| **F3-D1** | `STRUCTURAL_CODE` (respuesta escrita con reglas estructurales, aprobado en D2 pero no incluido en la autorización de la Fase 3) | Mantenerlo fuera hasta la Fase 4 y diseñarlo junto al contenido que lo necesite, con reglas muy acotadas. |
| **F3-D2** | Tablas `exercise_progress` y `lesson_progress` sin uso (redundantes con `exercise_attempt`) | Eliminarlas con una migración 1→2 cuando haya que cambiar el esquema por otro motivo; mientras tanto, no se usan. |
| **F3-D3 (= P2)** | Comportamiento de los borradores | Hoy se muestran **con la etiqueta «Borrador: contenido pendiente de revisión»** en todas las compilaciones (no hay otro contenido). Antes de publicar, decidir si las compilaciones de publicación los ocultan. |
| **F3-D4** | Formato del contenido (JSON v1, `docs/contenido/GUIA_AUTORES.md`) y su lectura en `:core:domain` | Aprobar el formato como base de la Fase 4. |
| **F3-D5 (= P8)** | Comprobación automática del Java del contenido | **Implementada en la Fase 4** (`JavaContentCompilationTest`); pendiente de tu aprobación. |

### Criterios de aceptación de la Fase 3 (según la autorización)

| Requisito | Estado |
|---|---|
| Cargar lecciones estructuradas, sin conexión | ✔ CI (`AssetContentRepositoryTest`, `LessonFlowTest`) |
| Mostrar objetivos, explicación y ejemplos | ✔ CI (`LessonFlowTest` comprueba objetivos y aviso de borrador); revisión visual pendiente |
| Siete tipos de ejercicio con validación probada | ✔ CI (25 pruebas de validación + coherencia de la lección real) |
| Correcta / incorrecta / incompleta y retroalimentación con explicación | ✔ CI |
| Pistas progresivas que no revelan la solución | ✔ Validado en el contenido (`LessonParser` rechaza pistas que contienen la respuesta) |
| Sin afirmar ejecución de código; sin botón ni ejecutor simulado | ✔ Textos «Respuesta comprobada» y avisos de que no se ejecuta código |
| Registrar resultados para el progreso | ✔ `exercise_attempt` (CI) |
| Contenido separado de la lógica | ✔ JSON en `assets`; validación en el dominio |
| Resumen de resultados de la sesión | ✔ CI (`LessonSessionTest`, `LessonViewModelTest`) |
| Contenido marcado como borrador | ✔ `reviewStatus: DRAFT` y aviso visible |
| Compilación y pruebas | ✔ CI 38428986983: 102/102 pruebas, lint sin errores (1 aviso conocido), APK generado; Windows en verde |
| Verificación manual en Android Studio | **Pendiente** (responsable) |
| Aprobación del contenido | **Pendiente** (responsable) |


## Fase 4 — Detalle

### Revisión de entrada

- Fase 3: aprobada por el responsable; último CI de la Fase 3 en verde (commit `d9b81ab`).
- El PR #1 sigue abierto; la Fase 4 se desarrolla en la misma rama (`ccr-db27c4de-czvzgb`).

### Discrepancias y decisiones tomadas (pendientes de tu aprobación)

1. **Umbral del 80 % (P6):** no está aprobado. No se ha inventado: la evaluación no declara `passThreshold`, así que la app muestra el resultado (aciertos y porcentaje), pero **no** dice «aprobado» ni «suspendido», y no bloquea ni desbloquea nada. El motor ya admite un umbral opcional para cuando lo apruebes.
2. **Versión de Java (P7):** no está aprobada. Los ejemplos se comprueban con `javac --release 17`; solo usan construcciones válidas desde Java 1.0 (`class`, `main`, `System.out.println`).
3. **Renumeración:** la lección demostrativa `java.m02.l01` (borrador, nunca aprobada) pasa a ser `java.m01.l05`. Los intentos guardados con los ids antiguos quedan huérfanos; solo existían en pruebas, porque no hay usuarios.
4. **Orden de las lecciones:** el orden es una recomendación. No se bloquean lecciones, porque el bloqueo dependería del umbral (P6).
5. **La evaluación es una lección de tipo `assessment`:** reutiliza el motor, pero no permite reintentos ni pistas.
6. **Persistencia sin cambio de esquema:**
   - Una lección cuenta como completada cuando todos sus ejercicios tienen al menos un intento correcto en `exercise_attempt`. No se guarda aparte.
   - El mejor resultado de la evaluación se guarda en la tabla existente `module_progress` (`bestAssessmentScore`, una fila por módulo).
   - La base de datos sigue en la versión 1, sin migración.
7. **Inicio:**
   - La lista plana de lecciones de la Fase 3 se sustituye por una tarjeta «Continuar aprendiendo», que muestra la siguiente lección sin completar, la evaluación o «módulo completado».
   - La tarjeta incluye el botón «Ver el módulo».
   - Motivo: con 8 actividades, la lista ya no era manejable y duplicaba la pantalla del módulo.
8. **Mapa:** deja de ser «Disponible en una próxima versión» y lista los módulos reales (hoy, uno).
9. **Sin recompensas del RPG:** no se concede XP; el contador de XP sigue en 0.

### Contenido en borrador (`app/src/main/assets/content/es/java/`)

Las lecciones son, en este orden:

| Id | Título | Ejercicios |
|---|---|---|
| `java.m01.l01` | ¿Qué es programar? | 5 |
| `java.m01.l02` | Algoritmos: los pasos para resolver un problema | 6 |
| `java.m01.l03` | Instrucciones y orden: la ejecución secuencial | 6 |
| `java.m01.l04` | Java y la estructura de un programa | 6 |
| `java.m01.l05` | El método main y System.out.println | 7 |
| `java.m01.l06` | Código fuente, compilación y ejecución | 6 |
| `java.m01.l07` | Leer y comprender programas sencillos | 6 |
| `java.m01.eval` | Evaluación del módulo 1 (6 tipos de pregunta, sin pistas) | 10 |

- **Módulo:** `java.m01` «Módulo 1: Introducción a la programación y tu primer programa en Java» (`modules/java.m01.json`).
- **Estado:** todo es **DRAFT, pendiente de tu revisión** (52 ejercicios).
- **Contenido de cada lección:**
  - objetivos, explicación y un ejemplo comentado línea a línea;
  - ejercicios de dificultad creciente;
  - pistas opcionales, explicación y retroalimentación.
- **Fuera del módulo:** no hay variables, bucles, métodos propios ni programación orientada a objetos.

### Componentes

| Componente | Implementado | Probado | Evidencia |
|---|---|---|---|
| Tipo de lección (`lesson`/`assessment`) y reglas de la evaluación en el parser (todas las preguntas puntúan, sin pistas, umbral opcional en (0, 1]) | ✔ | ✔ | `LessonParserTest` |
| Módulos: `modules/*.json`, índice con `modules`, `ModuleAssembler` (lecciones existentes, del mismo módulo, sin duplicados, tipo correcto) | ✔ | ✔ | `LessonParserTest`, `ModuleContentTest` |
| Evaluación en la sesión: una respuesta por pregunta, sin pistas, `AssessmentScore` (aciertos al primer intento; sin responder = fallo) | ✔ | ✔ | `LessonSessionTest`, `LessonViewModelTest` |
| Progreso del módulo derivado (`ModuleProgress`: lecciones completadas, siguiente actividad) | ✔ | ✔ | `ModuleProgressTest` |
| Comprobación del Java del contenido con `javac --release 17` (los ejemplos compilan; las salidas declaradas coinciden con las reales; `findError` falla justo en las líneas marcadas; los huecos rellenados y las líneas ordenadas compilan) | ✔ | ✔ | `JavaContentCompilationTest`. Se ejecuta en las pruebas JVM del dominio (contenedor y CI), **nunca en la app**. |
| Coherencia del contenido real (borrador, ids, dificultad no decreciente, cada objetivo practicado) | ✔ | ✔ | `ModuleContentTest` |
| Mejor resultado en `module_progress` (transacción de lectura y escritura; se conserva solo el mejor) | ✔ | ✔ CI | `RoomProgressRepositoryTest` |
| Progreso tras cerrar y reabrir la base de datos (archivo real) | ✔ | ✔ CI | `RoomProgressRepositoryTest.elProgresoSeRecuperaAlCerrarYReabrirLaBaseDeDatos` |
| Mapa → módulo → lecciones en orden → evaluación → resultado; tarjeta «Continuar» en Inicio | ✔ | ✔ CI | `LessonFlowTest`, `NavigationTest` |
| El resultado de la evaluación se guarda una sola vez por sesión terminada | ✔ | ✔ CI | `LessonViewModelTest` |
| Verificación en Android Studio / dispositivo | — | **Pendiente** (responsable) | `docs/CONFIGURACION_ENTORNO.md` §6 |

**No implementado (fuera del alcance de la Fase 4):**
- recompensas del RPG y XP;
- bloqueo de lecciones o de módulos;
- «aprobado/no aprobado» de la evaluación (depende de P6);
- módulo 2;
- variantes por edad del nuevo contenido;
- `STRUCTURAL_CODE`;
- cualquier ejecución de Java en la app.

### Criterios de aceptación de la Fase 4 (según la autorización)

| Requisito | Estado |
|---|---|
| Lecciones con objetivos, explicación, ejemplo comentado, ejercicios graduados, pistas y retroalimentación | ✔ Validado por el parser y `ModuleContentTest` |
| Ejemplos Java válidos y comprobados con herramientas | ✔ `JavaContentCompilationTest` (`javac --release 17`) |
| Contenido en borrador | ✔ `reviewStatus: DRAFT` en el módulo y en las 8 lecciones |
| Evaluación con varios tipos de pregunta y corrección coherente | ✔ 6 tipos; se usa el mismo validador que en las lecciones |
| Resultado final a partir de las respuestas comprobadas; respuestas incompletas | ✔ Una respuesta incompleta no cuenta como intento; una pregunta sin responder cuenta como fallo |
| Guardar el resultado de la sesión | ✔ Mejor resultado por módulo (`module_progress`) |
| Regla del 80 % | **No aplicada:** P6 no está aprobada |
| Sin recompensas del RPG | ✔ |
| Navegación completa y retomar sin perder el progreso | ✔ CI (Robolectric); falta la revisión manual |
| Sin botones falsos | ✔ Todos los botones nuevos llevan a una pantalla real |
| Sin migraciones innecesarias; sin completar dos veces; progreso tras reiniciar | ✔ Esquema v1 sin cambios; resultados derivados o del tipo «mejor de»; prueba de reapertura |
| Verificación manual en Android Studio | **Pendiente** (responsable) |
| Aprobación del contenido | **Pendiente** (responsable) |

## Corrección del cierre al abrir y fase visual — Detalle

### Cierre al abrir la app (corregido)

- **Síntoma** (informado el 2026-10-15): la app se cerraba al abrirla en un teléfono real. Inicio mostraba el título y luego se cerraba.
- **Causa, confirmada en un emulador** (CI 38677916047, API 26 y 34, commit `d9bfa54`, sin la corrección): `PatternSyntaxException` en `LessonParser.<clinit>` (`LessonParser.kt:43`).
  - La expresión `\{\{(\d+)}}` tenía las llaves de cierre sin escapar.
  - La JVM la acepta, pero el motor de Android (ICU) la rechaza. Por eso las 260 pruebas JVM/Robolectric pasaban.
  - La misma expresión estaba en `ExerciseInputs.kt`.
- **Corrección:**
  - una sola expresión `LessonParser.BLANK_MARKER` con las llaves escapadas (`\{\{(\d+)\}\}`);
  - Inicio y las recomendaciones ya no cierran la app si falla la lectura del contenido: muestran «contenido no disponible» o no muestran recomendaciones.
- **Pruebas nuevas:**
  - `RegexPortabilityTest` (dominio): revisa todas las expresiones regulares del código de producción. Se comprobó que falla con la expresión antigua.
  - `LaunchSmokeTest` (instrumentada): abre Inicio y la primera lección en el emulador.
  - Job de CI «Emulador Android (API 26 y 34)»: instala el APK, comprueba que sigue abierto a los 20 s, ejecuta la prueba instrumentada y publica logcat y una captura de Inicio (artefacto `emulador-api-NN`).

### Fase visual «Academia interactiva futurista»

Pedida por el responsable el 2026-10-15: «azul oscuro, neón y verde neón». No sustituye a la Fase 8 del plan, que sigue sin autorizar.

- **Paleta** (`ui/theme/Theme.kt`):
  - fondo azul marino `#050A18`;
  - neón azul `#29B6FF`, neón verde `#39FF8E` y violeta `#E36BFF`;
  - versión clara equivalente.
  - `ContrastTest` comprueba 15 pares texto/fondo por tema, todos ≥ 4,5:1 (AA).
- **Tema oscuro de neón por defecto** (`LearningPreferences.themeMode = DARK`). «Claro» y «Como el sistema» siguen disponibles en Ajustes. Esto cambia el comportamiento aprobado en la Fase 7 (antes el valor por defecto era «Como el sistema») y queda pendiente de tu confirmación.
- **Componentes** (`ui/components/Neon.kt`):
  - fondo con degradado, rejilla y halos;
  - `NeonCard` con borde degradado azul→verde, que sustituye a las tarjetas de todas las pantallas;
  - `SectionTitle` con barra de neón;
  - `NeonProgressBar`, que TalkBack anuncia como barra de progreso;
  - `NeonBadge` para el nivel.
- **Inicio:** cabecera con un panel degradado y una etiqueta de estilo «terminal».
- **Navegación:** barra inferior con línea de neón.
- **Código:** bloques con fondo casi negro y texto verde neón.
- **Títulos:** letra monoespaciada del sistema. No se descarga ni se incluye ninguna fuente externa.
- **Icono:** símbolo `</>` en neón sobre azul marino. Ventana azul marino para evitar el destello blanco al abrir.
- **Sin cambios** en textos de contenido, reglas, datos ni permisos.

---

## Fase 7 — Detalle

### Revisión de entrada (2026-10-13)

- Fases 2 a 6: **aprobadas** por el responsable; último CI en verde (38449932591, 234/234 pruebas).
- Las verificaciones manuales en Android Studio de las fases 3 a 6 no tienen resultados registrados en este documento. Constan como aprobadas por el responsable, sin evidencia de prueba en dispositivo.
- No falta ninguna funcionalidad esencial.

**Lo que ya existía:**
- variantes de presentación en el modelo (`default`, `kids`, `teens`, `adults`; la lección elige la del público);
- selector de público adolescente/adulto (D3);
- tema según el sistema;
- ninguna animación propia.

**Contenido:** todo tenía solo la variante `default`.

### Decisiones del responsable (2026-10-13)

| Tema | Decisión |
|---|---|
| Variante infantil | **Se habilita** en Ajustes (cambia D3). Es una preferencia voluntaria: no se pregunta la edad. |
| Variantes de ejemplo | Solo la **lección 1**, en borrador. |
| Recomendaciones | Las **reglas de dificultad** propuestas (ver abajo). Cubren la «misión de refuerzo» pendiente de la Fase 6, sin crear una misión. |
| Preferencias | Tema, tamaño de texto, botón de pistas y restablecer preferencias. |

### Preferencias (Ajustes)

Se guardan en DataStore, solo en el dispositivo, y se aplican de verdad:

| Preferencia | Valores | Dónde se aplica |
|---|---|---|
| Estilo de las explicaciones | Niños y niñas · Adolescente · Persona adulta (sin elegir = presentación común) | Teoría de las lecciones con variante; los objetivos, ejercicios y corrección no cambian. |
| Tema | Como el sistema · Claro · Oscuro | Toda la app (`FuncodeRoot` → `FuncodeTheme`). |
| Tamaño del texto | Normal (100 %) · Grande (115 %) · Muy grande (130 %) | Toda la app. Se multiplica por el tamaño de letra del sistema, que se sigue respetando (`ProvideTextScale`). |
| Mostrar el botón de pistas | Activado (por defecto) / desactivado | Lecciones y proyectos (las evaluaciones nunca tienen pistas). No cambia el XP ni la corrección. |
| Restablecer preferencias | Botón con confirmación | Vuelve a los valores por defecto. **No borra el progreso**, que está en Room. No se ha añadido «borrar progreso». |

**Animaciones:** la app no tiene animaciones propias, así que no se ha añadido una opción para reducirlas. Los componentes de Material 3 siguen el ajuste del sistema.

### Variantes educativas

- **`java.m01.l01` (versión 2, DRAFT)** tiene presentación común y variantes para niños, adolescentes y adultos.
- Todas conservan el ejemplo del robot al que se refieren los ejercicios.
- Comparten los mismos objetivos y ejercicios: la corrección es idéntica por construcción, porque los ejercicios no dependen de la variante.
- Las demás lecciones usan la presentación común con cualquier público.

### Recomendaciones de estudio (reglas, sin inteligencia artificial)

`StudyAdvisor` usa solo los intentos guardados (`exercise_attempt`) y los mejores resultados de las evaluaciones.

1. **Retomar:** lección con algún intento y sin completar.
2. **Repasar:** lección completada con **2 o más** ejercicios que costaron. Un ejercicio costó si su primer acierto llegó tras un fallo o con pistas.
3. **Repetir la evaluación:** mejor resultado por debajo del umbral (80 %). Se acompaña de las lecciones de la regla 2.

Se muestran en Inicio («Recomendado para ti», máximo 3) con un botón que abre la actividad. Además:
- **no bloquean** nada;
- **no dan XP**: repetir actividades ya recompensadas da 0 XP;
- no cambian resultados ni umbrales.

En cada ejercicio se muestra **qué objetivo se practica**.

### Accesibilidad

**Barreras corregidas en esta fase:**
- **Contraste:** el color secundario del tema claro con texto blanco daba **4,3:1**, por debajo del 4,5:1 (AA). Se oscureció a `#00796B` (**5,3:1**). `ContrastTest` comprueba ahora 9 pares texto/fondo en los dos temas.
- **Bloques de código:** TalkBack leía el número pegado al código con espacios de alineación. Ahora cada línea se anuncia como «Línea N: …».
- **Ajustes:**
  - el interruptor de pistas se anuncia como interruptor con su estado y toda la fila es pulsable (≥ 48 dp);
  - cada grupo de opciones es un grupo de botones de opción;
  - hay encabezados por sección.

**Ya cumplido y comprobado:**
- objetivos táctiles de al menos 48 dp en opciones y filas;
- la información no depende solo del color: los estados siempre llevan texto;
- retroalimentación y avisos con *live region*;
- barras de progreso con descripción o decorativas junto a un texto equivalente;
- tarjetas que se leen de una vez.

**Texto ampliado:** `LargeTextTest` recorre Inicio → lección → ejercicio → retroalimentación con el texto del sistema al 200 % y el de la app en «Muy grande» (260 % en total). Todo sigue siendo alcanzable desplazándose.

**Limitaciones:**
- **No se afirma el cumplimiento completo de ninguna norma de accesibilidad (WCAG, EN 301 549).**
- Las pruebas son automáticas en Robolectric; falta la revisión con TalkBack en un dispositivo real (procedimiento en `docs/CONFIGURACION_ENTORNO.md` §9).
- Con «Muy grande» y el texto del sistema al máximo, las etiquetas de la barra inferior pueden recortarse; está pendiente de revisión visual.

### Componentes

| Componente | Implementado | Probado | Evidencia |
|---|---|---|---|
| `settings/LearningPreferences.kt`, `SettingsRepository` ampliado | ✔ | ✔ CI | `DataStoreSettingsRepositoryTest` (valores por defecto, persistencia al reabrir, restablecer) |
| `Audience.KIDS` seleccionable | ✔ | ✔ | `AudienceTest`, `PreferencesAndAccessibilityTest` |
| Variantes de la lección 1 (mismos objetivos, ejercicios y ejemplo) | ✔ | ✔ | `ModuleContentTest`, `LessonViewModelTest` |
| `ExerciseHistory` (deducido de los intentos) y `observeExerciseHistories` | ✔ | ✔ | `ExerciseHistoryTest`, `RoomProgressRepositoryTest` |
| `StudyAdvisor` (reglas de recomendación) | ✔ | ✔ | `StudyAdvisorTest` |
| Inicio: «Recomendado para ti»; seguir una recomendación no da XP | ✔ | ✔ CI | `LessonFlowTest` |
| `FuncodeRoot`: tema y escala de texto aplicados | ✔ | ✔ CI | `PreferencesAndAccessibilityTest`, `LargeTextTest` |
| Ajustes: público, tema, tamaño, pistas y restablecer con confirmación | ✔ | ✔ CI | `PreferencesAndAccessibilityTest` |
| Pistas ocultas con la preferencia; objetivo que se practica | ✔ | ✔ CI | `LessonFlowTest`, `LessonViewModelTest` |
| Contraste y descripciones de código y barras | ✔ | ✔ CI | `ContrastTest`, `PreferencesAndAccessibilityTest` |
| Verificación en Android Studio / dispositivo con TalkBack | — | **Pendiente** (responsable) | `docs/CONFIGURACION_ENTORNO.md` §9 |

- **Base de datos:** sin cambio de esquema ni migración. Solo hay una consulta nueva de lectura sobre `exercise_attempt`.
- **DataStore:** tres claves nuevas (`theme_mode`, `text_scale`, `hints_enabled`).
- **Privacidad:** no se pide la edad ni ningún dato personal. No hay servicios externos ni permisos nuevos.

### Criterios de aceptación de la Fase 7 (según la autorización)

| Requisito | Estado |
|---|---|
| Variantes que conservan objetivos y corrección; selección voluntaria y modificable, sin pedir la edad | ✔ |
| Preferencias reales, guardadas y aplicadas | ✔ CI (38452947745) |
| Accesibilidad: etiquetas, roles, contraste, texto ampliado, objetivos táctiles, estados con texto | ✔ Corregido lo detectado; revisión con TalkBack pendiente |
| Ritmo: recomendaciones con datos reales, sin bloqueos, sin XP extra, sin cambiar criterios | ✔ |
| Privacidad y funcionamiento sin conexión; restablecer distinto de borrar el progreso | ✔ |
| Integración con el currículo y el RPG sin alterar respuestas, notas ni recompensas | ✔ |
| Verificación manual en Android Studio | **Pendiente** (responsable) |
| Aprobación del contenido de las variantes | **Pendiente** (responsable) |

## Fase 6 — Detalle

### Revisión de entrada (2026-10-13)

- Fase 5: aprobada por el responsable; CI 38438737589 en verde (188/188).
- Ya existía una primera versión de la Fase 6, hecha con la descripción de una línea del plan (CI 38441919559, 208/208): el reto guiado del módulo y cinco misiones fijas.
- La especificación completa de la Fase 6 pide más. Las misiones deben:
  - definirse como contenido;
  - tener objetivo, tipo, requisitos, actividades vinculadas, estados y versión;
  - contar con una pantalla propia.
- Además, el proyecto guiado necesitaba conceptos previos, pistas progresivas y un resumen final.
- No había errores bloqueantes.
- Antes de programar se consultaron las decisiones que afectan a las recompensas y a la navegación.

### Decisiones aprobadas por el responsable (2026-10-13)

| Tema | Decisión |
|---|---|
| Proyecto guiado | Es el reto del módulo (`kind: challenge`), que se abre al completar las lecciones y da **50 XP** una vez. Usa los 7 tipos de ejercicio; no hay ejecución de Java (D2). |
| Recompensa de las misiones | **10 XP de bonificación** por misión completada, una sola vez. Se registra en el mismo registro de XP con el tipo `MISSION`. |
| Misiones anteriores | Las cinco misiones fijas se **sustituyen** por misiones de contenido. |
| Desbloqueo | Cada misión declara sus **requisitos** (otras misiones). La primera está disponible desde el inicio. |
| Acceso | Pantalla **Misiones** abierta desde Inicio y desde el Mapa; la barra inferior no cambia. |

### Modelo y reglas

- **Misión** (`missions/<id>.json`). Campos:
  - id estable (también identifica su bonificación);
  - módulo, orden y versión del contenido;
  - estado de revisión;
  - tipo (`learning`, `practice`, `assessment`, `project`);
  - título, descripción y objetivo educativo;
  - requisitos (otras misiones);
  - tareas.
- **Tareas verificables.** Siempre apuntan a contenido existente:

  | Tarea | Se cumple cuando… |
  |---|---|
  | `completeLesson` | todos los ejercicios de la lección están resueltos |
  | `solveExercises` | están resueltos los ejercicios indicados (de lecciones o del proyecto) |
  | `passAssessment` | la evaluación alcanza el umbral aprobado (80 %) |
  | `completeChallenge` | el proyecto guiado está completo, con las lecciones del módulo completadas |

- **Estados:** se **calculan** con los resultados guardados (`exercise_attempt` y `module_progress`); no se guardan aparte, así que no se duplican ni se desincronizan.
  - *Bloqueada:* algún requisito no está completado.
  - *Completada:* todas las tareas cumplidas.
  - *En progreso:* alguna tarea avanzada.
  - *Disponible:* sin avance.
- **Abrir una misión o su pantalla no cambia nada.**
- Una misión bloqueada no se completa aunque sus tareas estén hechas. Al cumplir el requisito pasa directamente a completada, porque las actividades ya hechas cuentan.
- **Validación:** `MissionAssembler` descarta, con la lista de errores, las misiones que:
  - hacen referencia a contenido inexistente, de otro tipo o de otro módulo;
  - incluyen preguntas de evaluación en `solveExercises`;
  - tienen requisitos inexistentes o en ciclo;
  - dependen de una misión descartada.
- **Recompensa:** `RewardEngine` concede `MISSION` (10 XP) cuando una misión está completada.
  - Es idempotente: repetir actividades o volver a entrar no suma más.
  - Las actividades se pueden repetir para practicar, sin bonificación.

### Misiones del módulo 1 (DRAFT, pendientes de tu revisión)

| Id | Tipo | Misión | Tareas | Requiere |
|---|---|---|---|---|
| `java.m01.mi01` | Aprendizaje | Descubre qué es programar y qué es un algoritmo | Lecciones 1 y 2 | — |
| `java.m01.mi02` | Práctica | Ordena los pasos de una solución | Lección 3 + 4 ejercicios de ordenar y de seguir secuencias | mi01 |
| `java.m01.mi03` | Aprendizaje | Conoce la estructura de un programa Java | Lección 4 | mi02 |
| `java.m01.mi04` | Aprendizaje | Muestra mensajes con System.out.println | Lección 5 | mi03 |
| `java.m01.mi05` | Práctica | Lee programas: predice salidas y encuentra errores | Lecciones 6 y 7 + 7 ejercicios de predecir la salida y de encontrar errores | mi04 |
| `java.m01.mi06` | Evaluación | Supera la evaluación del módulo 1 | Evaluación con un 80 % o más | mi05 |
| `java.m01.mi07` | Proyecto | Proyecto guiado: tu tarjeta de presentación | Proyecto `java.m01.reto` | mi05 |

**Total del módulo 1 sin pistas:** 910 (lecciones y evaluación) + 50 (proyecto) + 70 (7 misiones) = **1030 XP**, nivel 5. Comprobado en `RewardEngineTest`.

**No implementado:** la misión de «reforzar conceptos con dificultades». Requiere decidir cómo se detectan esas dificultades (por ejemplo, ejercicios fallados al primer intento) y es una regla educativa nueva; queda como propuesta pendiente.

### Proyecto guiado `java.m01.reto` (DRAFT, versión 2)

- **Título:** «Proyecto guiado: tu tarjeta de presentación en Java».
- **Situación:** presentarse en un club de programación con un programa que muestre una tarjeta.
- **Contenido:**
  - 4 conceptos previos;
  - 7 pasos ordenados (dificultad 1→4) con **2 pistas progresivas** cada uno;
  - explicación y retroalimentación en cada paso;
  - **resumen final de lo aprendido**, que se muestra en el resumen al resolver todos los pasos.
- **Finalización:** todos los pasos resueltos con las 7 lecciones completadas.
- **Comprobación:** el código se compila con `javac` en las pruebas. La app no ejecuta Java ni lo afirma.

### Componentes

| Componente | Implementado | Probado | Evidencia |
|---|---|---|---|
| `mission/Mission.kt`: modelo, tipos, tareas y estados | ✔ | ✔ | `MissionParserTest`, `MissionProgressTest` |
| `LessonParser.parseMission`: contenido inválido o incompleto con errores acumulados | ✔ | ✔ | `MissionParserTest` |
| `MissionAssembler`: referencias, tipos, módulo, requisitos y ciclos | ✔ | ✔ | `MissionAssemblerTest` |
| Estados y transiciones; la actividad que hay que abrir en cada tarea | ✔ | ✔ | `MissionProgressTest`, `RewardEngineTest` (cadena real mi01→mi02) |
| Bonificación `MISSION` sin duplicados | ✔ | ✔ | `MissionProgressTest`, `RewardEngineTest`, `RewardPersistenceTest` (Room en archivo, reapertura) |
| Proyecto guiado: bloque `project`, pistas progresivas, resumen | ✔ | ✔ | `LessonParserTest`, `ModuleContentTest`, `JavaContentCompilationTest` |
| Contenido real: 7 misiones válidas que cubren las 7 lecciones y los 4 tipos | ✔ | ✔ | `ModuleContentTest`, `AssetContentRepositoryTest` |
| Pantallas Misiones y detalle (requisitos, tareas con enlace, recompensa, resultado); acceso desde Inicio y Mapa | ✔ | ✔ CI | `MissionsViewModelTest`, `LessonFlowTest` |
| Aviso «¡Misión completada!» en la lección; misiones en el resumen de recompensas del Perfil | ✔ | ✔ CI | `LessonViewModelTest`, `ProfileStateTest` |
| Verificación en Android Studio / dispositivo | — | **Pendiente** (responsable) | `docs/CONFIGURACION_ENTORNO.md` §8 |

**Base de datos:** sin cambio de esquema ni migración.
- El tipo `MISSION` es un valor nuevo de la columna de texto `sourceType` de `xp_ledger`.
- Los estados de las misiones se deducen y no se guardan.

**No implementado (fuera de la Fase 6):**
- campañas narrativas;
- mapa mundial;
- misiones diarias y rachas;
- tienda y monedas;
- personalización por edades;
- multijugador;
- ejecución real de Java.

## Fase 5 — Detalle

### Revisión de entrada

- Fase 4: aprobada por el responsable; CI 38436000222 en verde (143/143 pruebas, Linux y Windows).
- Sin errores bloqueantes.
- El plan ya preveía:
  - `xp_ledger` con `UNIQUE(sourceType, sourceId)`;
  - la tabla `achievement_unlocked`;
  - el cálculo del XP y del nivel desde el registro (§6.3–6.4).
- Los valores concretos (P6) y las monedas no estaban definidos. Se consultaron antes de programar.

### Decisiones aprobadas por el responsable (2026-10-13)

| Tema | Valor |
|---|---|
| XP por ejercicio de lección | 10 + 5 × (dificultad − 1) → 10/15/20/25 XP. |
| Penalización por pista | −25 % del valor por cada pista revelada antes del primer acierto, con un mínimo del 50 %; se redondea hacia abajo. Ejemplos: dificultad 1 con 1 pista → 7 XP; dificultad 3 con 2 pistas o más → 10 XP. |
| Lección completada | +20 XP (todos sus ejercicios resueltos). |
| Evaluación aprobada | +100 XP, una sola vez. Las preguntas de la evaluación no dan XP por separado. |
| Umbral de aprobación | 80 % de respuestas correctas al primer intento (`passThreshold: 0.8` en `java.m01.eval`). No bloquea lecciones ni módulos. |
| Curva de niveles | Pasar del nivel n al n+1 cuesta 100 × n XP. Se empieza el nivel 2 con 100 XP, el 3 con 300, el 4 con 600 y el 5 con 1000. |
| Monedas | No se implementan en esta fase. |

**Totales del módulo 1:**
- Sin pistas suma **910 XP** (670 por ejercicios, 140 por lecciones y 100 por la evaluación), lo que lleva al **nivel 4**.
- La prueba `RewardEngineTest` lo comprueba con el contenido real.

### Reglas aplicadas

1. **Fuente única:** el XP se guarda solo en `xp_ledger`. El total y el nivel se calculan de él; no hay contador aparte.
2. **Primera finalización válida:** `UNIQUE(sourceType, sourceId)` impide un segundo registro para la misma actividad. Repetir una actividad da 0 XP, y la app lo indica («repetirlo sirve para practicar, pero no suma más»).
3. **Recompensas recalculadas desde lo verificado:** `RewardEngine.sync()` no recibe «XP a sumar».
   - Recalcula lo que corresponde a partir de:
     - los intentos correctos guardados (`exercise_attempt`), donde el **primer** acierto decide la penalización por pistas;
     - el mejor resultado de cada evaluación (`module_progress`).
   - Después registra solo lo que falta.
   - Por eso es seguro frente a pulsaciones repetidas, reinicios y fallos a mitad. Si la app se cierra antes de conceder algo, se concede en la siguiente sincronización, que se lanza también al abrir Inicio.
   - Las llamadas simultáneas se ordenan con un `Mutex`, y la base de datos rechaza los duplicados de todos modos.
4. **Los fallos y las respuestas incompletas no dan XP.** Solo cuentan los aciertos comprobados por el validador; una evaluación por debajo del umbral no da XP.
5. **Datos anteriores:** los aciertos guardados antes de la Fase 5 se recompensan en la primera sincronización, con la misma regla. Los ids retirados (`java.m02.l01.*`) se ignoran.

### Logros

| Id (estable) | Nombre | Condición |
|---|---|---|
| `first_lesson` | Primeros pasos | 1 lección completada |
| `three_lessons` | Buen ritmo | 3 lecciones distintas completadas |
| `first_assessment_passed` | Evaluación superada | 1 evaluación aprobada (≥ umbral) |
| `first_module_mastered` | Módulo dominado | Todas las lecciones de un módulo y su evaluación aprobada |
| `level_2` | Subida de nivel | Nivel 2 o superior |

- Se guardan en `achievement_unlocked` (clave primaria = id), así que nunca se duplican.
- **No conceden XP**, para evitar recompensas encadenadas.
- No hay logro de racha: el progreso por días no está implementado de forma fiable.

### Componentes

| Componente | Implementado | Probado | Evidencia |
|---|---|---|---|
| `RewardRules`: valores de XP y penalización | ✔ | ✔ | `RewardRulesTest` (valores, límites, entradas no válidas) |
| `LevelCurve` y `LevelProgress`: nivel, XP del nivel, restante y fracción | ✔ | ✔ | `LevelCurveTest` (estado inicial, límites exactos, determinismo, valores no válidos) |
| `Achievement` y `LearningState` | ✔ | ✔ | `AchievementTest` |
| `RewardCalculator` (puro) y `RewardEngine` (idempotente) | ✔ | ✔ | `RewardCalculatorTest`, `RewardEngineTest` (contenido real; repetición; pistas; evaluación aprobada y suspendida; 910 XP; llamadas simultáneas) |
| Room: `AchievementDao`, `ExerciseAttemptDao.firstCorrectAttempts`, `XpLedgerDao.observeAll` | ✔ | ✔ CI | `RoomProgressRepositoryTest`, `RewardPersistenceTest` (archivo real: sin duplicados tras reabrir; recompensa pendiente concedida después) |
| Avisos en la lección (XP, lección completada, evaluación aprobada, subida de nivel, logros; «ya recompensado») y XP de la sesión en el resumen | ✔ | ✔ CI | `LessonViewModelTest`, `LessonFlowTest` |
| Inicio: tarjeta de nivel con barra de progreso; sincronización pendiente al abrir | ✔ | ✔ CI | `NavigationTest` |
| Perfil: nivel, barra, XP, resumen de recompensas por tipo y logros con fecha | ✔ | ✔ CI | `ProfileStateTest`, `NavigationTest`, `LessonFlowTest` |
| Verificación en Android Studio / dispositivo | — | **Pendiente** (responsable) | `docs/CONFIGURACION_ENTORNO.md` §7 |

**Base de datos:**
- **Sin cambio de esquema ni migración.** Las tablas `xp_ledger` y `achievement_unlocked` ya existían en la versión 1; solo se añadieron consultas (DAO).
- El esquema exportado sigue igual; CI lo comprueba.

**Accesibilidad:**
- La barra de progreso tiene una descripción para TalkBack («Progreso hacia el nivel N: X de Y»).
- Los avisos de recompensa se anuncian (*live region*).
- Cada tarjeta de logro se lee como un solo elemento.

**No implementado (fuera de la Fase 5 o no aprobado):**
- monedas;
- tienda;
- avatar (el plan lo incluía en el perfil, pero la autorización de la Fase 5 no lo pide);
- rachas;
- misiones;
- desbloqueo de módulos (hay un solo módulo);
- animaciones;
- historial detallado movimiento a movimiento (el perfil muestra un resumen por tipo).

### Propuesta de monedas (pendiente de decisión, para la fase que traiga la tienda)

- Monedas = XP total ÷ 10 (redondeo hacia abajo), **derivadas del mismo registro de XP**, menos lo gastado.
- Lo gastado se guardaría en una tabla nueva de compras, con una migración 1→2.
- Nunca habría dinero real, anuncios ni intercambio.

### Criterios de aceptación de la Fase 5 (según la autorización)

| Requisito | Estado |
|---|---|
| XP solo por la primera finalización válida; repetir no da más | ✔ Restricción `UNIQUE` + `RewardEngine` (pruebas de dominio y de Room) |
| Penalización por pistas aprobada | ✔ `RewardRules` |
| Total derivado del registro, sin contador | ✔ `SUM(amount)` de `xp_ledger` |
| Sin duplicados por pulsaciones, reinicios o navegación | ✔ Motor idempotente + `Mutex` + restricciones de la base de datos |
| Recompensas solo a partir de resultados comprobados; los fallos no dan XP | ✔ |
| Niveles: total, nivel, XP restante, barra, aviso al subir, estado inicial | ✔ |
| Cálculo determinista y en el dominio, con pruebas | ✔ `LevelCurveTest` |
| Monedas | No implementadas (decisión del responsable) |
| Logros con id estable, nombre, descripción, condición y estado; sin duplicados | ✔ |
| Integración visual (Inicio, Perfil, lección) sin reconstruir la navegación | ✔ |
| Persistencia, sin conexión, tras cerrar y abrir | ✔ CI (`RewardPersistenceTest`, `RoomProgressRepositoryTest`) |
| Sin pagos, anuncios, analítica ni permisos nuevos | ✔ |
| Verificación manual en Android Studio | **Pendiente** (responsable) |

## Pruebas ejecutadas

| Fecha | Dónde | Prueba | Resultado |
|---|---|---|---|
| 2026-10-15 | CI 38686836314 (commit `32d029a`) | Linux, Windows, emuladores API 26 y 34, tamaño del APK | **Todo en verde.** JVM: dominio 182, `:core:javarunner` 19, app (Robolectric) 97. Emulador API 34: 15 superadas + 1 omitida (la prueba solo para API < 33). Emulador API 26: 3 superadas + 13 omitidas (el ejecutor necesita API 33). APK *release* +8,45 MB. |
| 2026-10-15 | CI 38683464193 … 38686325724 | Emuladores | Fallos reales encontrados y corregidos o documentados: ECJ 3.46 no funciona en Android (`Runtime.version`); ECJ 3.28 necesita API 33 (`readAllBytes`); los errores de la VM bloqueados por la lista permitida; `\r\n` en Windows. Ver `FASE8_EJECUCION_JAVA.md` §9.5. |
| 2026-10-15 | Contenedor | `:core:domain:test`, `:core:javarunner:test` | 182/182 y 19/19 |
| 2026-10-15 | CI 38678905732 (commit `99b74cd`) | Linux + Windows + emulador API 26 y 34 | **Verde: 263/263 pruebas JVM**; lint: 0 errores, 1 aviso conocido (`OldTargetApi`). En ambos emuladores la app sigue abierta 20 s después de abrirse y `LaunchSmokeTest` pasa (API 34: 1 ejecutada, 0 fallos). |
| 2026-10-15 | CI 38678434103 (commit `839056f`) | Linux + Windows + emulador | Verde, pero la prueba instrumentada **no llegó a ejecutarse** (fallo de instalación por firma, no detectado por Gradle). Corregido en `99b74cd`. |
| 2026-10-15 | CI 38677916047 (commit `d9bfa54`, sin la corrección) | Emulador API 26 y 34 | **Fallo esperado**: reproduce el cierre (`PatternSyntaxException` en `LessonParser.kt:43`). |
| 2026-10-15 | Contenedor | `:core:domain:test` con `RegexPortabilityTest` | 176/176 superadas; la prueba nueva falla si se vuelve a poner la expresión antigua (comprobado). |
| 2026-10-10 | Contenedor | Inspección del entorno | JDK 21 y Gradle 8.14.3; sin Android SDK; `dl.google.com` y `downloads.gradle.org` bloqueados; Maven Central con límite de peticiones (429). |
| 2026-10-10 | Contenedor | `:core:domain:test` (modo solo dominio, Gradle 8.14.3 local, réplica `repo1.maven.org` mediante *init script* fuera del repositorio) | **7/7 superadas** |
| 2026-10-10 | CI #1–#2 | Compilación completa | **Fallo** de configuración: el plugin de Kotlin no veía las clases de AGP (`NoClassDefFoundError: BaseVariant`). Corregido declarando los plugins en la raíz. |
| 2026-10-10 | CI #3 | Pruebas de `:app` | **Fallo**: Robolectric necesita JDK 21 para Android 15+ (`UnsupportedOperationException`). Corregido pasando el CI a JDK 21. |
| 2026-10-10 | CI #4 | Pruebas de `:app` | **11/15 fallos**: Robolectric con Android 16 necesita acceso a `jdk.internal.access`. Corregido con `--add-exports/--add-opens` en las pruebas. |
| 2026-10-10 | CI #5 | Control del esquema de Room | **Fallo esperado**: el esquema aún no estaba en Git. Se guardó a partir de la salida del CI. |
| 2026-10-10 | CI 38076992682 | Todo | **Verde** |
| 2026-10-10 | CI 38077143387 | Todo + resumen | **Verde: 22/22 pruebas** (7 dominio, 5 Room, 4 DataStore, 6 interfaz); lint sin errores, **1 aviso**; APK generado |
| 2026-10-12 | Equipo Windows del responsable | `:core:domain:test :app:testDebugUnitTest` | **5 de 15 fallos** en `:app`: `UnsatisfiedLinkError` en `SQLiteConnection.nativeOpen` (SQLite nativo de Robolectric). Gradle Sync correcto. Emulador: "failed to connect within 5 minutes". |
| 2026-10-12 | CI, nuevo trabajo Windows (38318582592) | Pruebas en Windows | Room **pasa**; **falla** `DataStoreSettingsRepositoryTest.cambiarDePublicoReemplazaElAnterior` (`Unable to rename ... settings.preferences_pb`): en la JVM de Windows `FileStorage` no puede reemplazar un archivo existente. |
| 2026-10-12 | CI Windows con rutas con espacios (38319034324) | Hipótesis "espacio en la ruta" | Room **pasa** con espacios en el proyecto y en TEMP → hipótesis descartada. |
| 2026-10-13 | Contenedor | `:core:domain:test` (modo solo dominio) con el motor de la Fase 3 | **72/72 superadas** |
| 2026-10-13 | Contenedor | `javac`/`java` 21 sobre los 5 programas de la lección demostrativa | Salidas y error de compilación coinciden con el contenido |
| 2026-10-13 | CI 38428986983 (commit `63c4a71`) | Linux + Windows | **Verde: 102/102 pruebas** (72 dominio, 30 app); lint: 0 errores, 1 aviso (`OldTargetApi`); APK generado; esquema de Room sin cambios |
| 2026-10-13 | Contenedor | `:core:domain:test` (modo solo dominio) con la Fase 4, incluida la compilación con `javac --release 17` del contenido | **97/97 superadas** |
| 2026-10-13 | CI 38435568555 (commit `730a23b`) | Linux + Windows | **Verde: 143/143 pruebas** (97 dominio, 46 app); APK generado; esquema de Room sin cambios; lint: 0 errores, 2 avisos (`OldTargetApi` conocido y `PluralsCandidate` en el texto del resultado, corregido después reformulando el texto) |
| 2026-10-13 | Contenedor | `:core:domain:test` (modo solo dominio) con la Fase 5 | **127/127 superadas** |
| 2026-10-13 | Contenedor | `:core:domain:test` (modo solo dominio) con la Fase 6, incluida la compilación del reto con `javac` | **144/144 superadas** |
| 2026-10-13 | Contenedor | `:core:domain:test` (modo solo dominio) con misiones de contenido y proyecto guiado | **164/164 superadas** |
| 2026-10-13 | CI 38449467297 (commit `6fe2eb6`) | Linux + Windows | **Verde: 234/234 pruebas** (164 dominio, 70 app); APK generado; esquema de Room sin cambios; lint: 0 errores, 2 avisos (`OldTargetApi` conocido y `PluralsCandidate` en un texto de misión, corregido después reformulando el texto) |
| 2026-10-13 | Contenedor | `:core:domain:test` (modo solo dominio) con la Fase 7 | **173/173 superadas** |
| 2026-10-13 | CI 38451937299 (commit `075c81b`) | Linux + Windows | **Fallo de compilación** de `:app`: `stringResource` dentro de un bloque `semantics { }` (CodeBlock). Corregido en `313c03d`. |
| 2026-10-13 | CI 38452256496 (commit `313c03d`) | Linux + Windows | **Fallo de compilación** de las pruebas: faltaba importar `assert` en `PreferencesAndAccessibilityTest`. Corregido en `6638871`. |
| 2026-10-13 | CI 38452449808 (commit `6638871`) | Linux + Windows | **Verde: 260/260 pruebas** (173 dominio, 87 app); APK generado; esquema de Room sin cambios; lint: 0 errores, 4 avisos (`OldTargetApi` conocido y 3 `ModifierParameter` en firmas nuevas, corregidos después) |
| 2026-10-13 | CI 38452947745 (commit `95b71dd`) | Linux + Windows | **Verde: 260/260 pruebas**; lint: 0 errores, 1 aviso conocido (`OldTargetApi`); APK generado; esquema de Room sin cambios |
| 2026-10-13 | CI 38449932591 (commit `d712599`) | Linux + Windows | **Verde: 234/234 pruebas**; lint: 0 errores, 1 aviso conocido (`OldTargetApi`); APK generado; esquema de Room sin cambios |
| 2026-10-13 | CI 38441919559 (commit `ae93235`) | Linux + Windows | **Verde: 208/208 pruebas** (144 dominio, 64 app); lint: 0 errores, 1 aviso conocido (`OldTargetApi`); APK generado; esquema de Room sin cambios |
| 2026-10-13 | CI 38438737589 (commit `91b7049`) | Linux + Windows | **Verde: 188/188 pruebas** (127 dominio, 61 app); lint: 0 errores, 1 aviso conocido (`OldTargetApi`); APK generado; esquema de Room sin cambios |
| 2026-10-13 | CI 38436000222 (commit `ed66018`) | Linux + Windows | **Verde: 143/143 pruebas**; lint: 0 errores, 1 aviso conocido (`OldTargetApi`); APK generado; esquema de Room sin cambios |
| 2026-10-12 | CI 38319271182 | Linux + Windows | **Verde en ambos** tras usar `OkioStorage` en la prueba de DataStore (la app sigue usando `FileStorage`, correcto en Android). |

## Errores conocidos

- **Corregido (2026-10-15):** la app se cerraba al abrirla en Android real por una expresión regular que ICU no acepta. Ver «Corrección del cierre al abrir y fase visual — Detalle».
- **Según el responsable, resuelto** (2026-10-13; la causa no consta en este registro): las pruebas de Room fallaban en su equipo Windows con `UnsatisfiedLinkError` (SQLite nativo de Robolectric), aunque pasan en Windows y Linux de CI. Causa sin confirmar; pendiente de `gradlew --version` (JDK y arquitectura usados por la terminal).
- **Según el responsable, resuelto** (2026-10-13): el emulador no arrancaba ("failed to connect within 5 minutes"); pendiente de comprobar la virtualización.
- **Corregido:** fallo de la prueba de DataStore en la JVM de Windows (ver tabla de pruebas).

## Limitaciones y avisos conocidos

- **Lint `OldTargetApi`:** `targetSdk` 36 es menor que `compileSdk` 37. Es intencionado (Robolectric simula la misma versión que declara la app). Se revisará cuando Robolectric y el resto de pruebas soporten 37.
- **Aviso de GitHub:** `actions/upload-artifact@v4` usa Node.js 20 (obsoleto, de momento solo un aviso). Se actualizará cuando se confirme una versión estable más reciente.
- **API de pruebas obsoleta evitada:** se usa `createAndroidComposeRule` v2 (la v1 está marcada como obsoleta).
- **Contenedor de Claude:** no puede compilar Android; solo puede probar `:core:domain` en modo solo dominio. El APK y las pruebas de `:app` se verifican en CI.
- **Ramas `main` y `Main`:** existen ambas en GitHub (la segunda no la creó Claude). Generan conflictos en Windows/macOS. Ver `docs/FLUJO_DE_TRABAJO.md`.
- **Pruebas en caché:** GitHub Actions reutiliza resultados de pruebas cuando el código no cambia ("FROM-CACHE"); el resumen muestra los recuentos de esos resultados.

## Dependencias pendientes de integración (revisado el 2026-10-15, antes del prototipo de la Fase 8)

| Pendiente | Estado real |
|---|---|
| Fusión en `main` | `main` está en `c6a4e73` (fases 2–7). La rama `ccr-db27c4de-czvzgb` lleva **6 commits sin fusionar**: corrección del cierre, CI con emulador, fase visual y estudio de la Fase 8. No hay *pull request* abierto. No se ha reescrito ni sobrescrito ninguna rama. |
| Confirmación manual en tu teléfono | **Pendiente:** la corrección del cierre y el nuevo aspecto solo están verificados en emuladores de CI. |
| Aprobación de la Fase 7 | **Pendiente** de aprobación explícita (se fusionó en `main` con el PR #1). |
| Aprobación de la fase visual | **Pendiente**, incluido el tema oscuro como predeterminado. |
| Versión de Java (P7) | **Resuelta:** Java 17 (F8-D5). |
| Contenido en borrador del módulo 1 | **Pendiente** de tu revisión (D7). |

El prototipo de la Fase 8 se desarrolla en la misma rama. Si lo prefieres, se puede separar en otra rama antes de fusionar.

## Acciones pendientes del responsable

0. **Fase 8:**
   - Decidir F8-P1: Android 8–12, donde el compilador no arranca.
   - Decidir F8-P2: licencias de R8.
   - Decidir F8-P3: el tiempo de la primera ejecución, que llegó a 5,9 s.
   - Decidir F8-P4: la política de Google Play.
   - Probar el «Laboratorio de Java» en un teléfono con Android 13 o superior. Está en Ajustes, solo en el APK de depuración. Ver `docs/FASE8_EJECUCION_JAVA.md` §11.

1. Instalar el nuevo APK en tu teléfono y confirmar que ya no se cierra; revisar el nuevo aspecto y decir si el tema oscuro de neón debe ser el predeterminado.
2. Verificar la Fase 7 en Android Studio y con TalkBack siguiendo `docs/CONFIGURACION_ENTORNO.md` §9.
3. Revisar el contenido en borrador del módulo `java.m01` (7 lecciones, las variantes de la lección 1, la evaluación, el proyecto guiado y las 7 misiones) y aprobarlo o pedir cambios (D7).
4. Decidir P7 (versión de Java), los logros, el avatar y la propuesta de monedas (ver «Fase 5 — Detalle»). La misión de refuerzo queda cubierta por las recomendaciones de la Fase 7.
5. Responder las decisiones F3-D1 a F3-D5 que siguen abiertas.
6. Borrar la rama `Main` duplicada y cambiar la rama por defecto a `main` (`docs/FLUJO_DE_TRABAJO.md`).

## Próximos pasos autorizados

Ninguno más.
- El prototipo de verificación de la Fase 8 está terminado.
- No está autorizada la integración definitiva, la publicación ni la fase siguiente.
- Pendiente: tus decisiones F8-P1 a F8-P4 (`docs/FASE8_EJECUCION_JAVA.md` §10).

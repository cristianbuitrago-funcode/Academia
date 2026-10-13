# Funcode Academy — Plan técnico (Fase 1: Planificación y arquitectura)

> **Estado:** plan con las decisiones D1–D8 **aprobadas por el responsable del proyecto (2026-10-10)**.
> P1 y P3 resueltas y Fase 2 autorizada (2026-10-10). El estado real de la implementación está en `docs/ESTADO_PROYECTO.md`.

---

## 0. Decisiones aprobadas (D1–D8)

| # | Decisión aprobada | Consecuencias en este plan |
|---|---|---|
| **D1** | **Kotlin + Jetpack Compose + Material 3.** Dos módulos iniciales: `:app` y `:core:domain`, con capas de interfaz, dominio y datos separadas. La app se escribe en Kotlin, pero **enseña Java de forma auténtica**. Las decisiones técnicas importantes se documentan brevemente. | §2, §3, §4. Registro de decisiones en §15. Todo el código Java que aparezca en lecciones debe ser Java válido y compilable (§8.4). |
| **D2** | **Opción A para el MVP:** ejercicios con respuestas comprobadas, **sin ejecutar Java real**. Tipos: selección y relación de conceptos, predecir salida, completar código, ordenar instrucciones, identificar errores y respuestas escritas validables con reglas estructurales. Cada ejercicio debe tener una respuesta verificable, retroalimentación y pruebas de sus reglas. Se diseña la interfaz `CodeRunner` sin implementarla. La Fase 8 investiga la ejecución real. | §8. La interfaz nunca dice "ejecutado" (§8.5). |
| **D3** | **MVP: una ruta para adolescentes y adultos principiantes.** El modelo de contenido admite las variantes `kids`, `teens` y `adults` sin redactarlas todas ahora. La adaptación cambia el lenguaje, los ejemplos, las actividades y la presentación, **nunca** los conceptos ni los criterios de evaluación. Sin fecha de nacimiento ni datos innecesarios. La ruta infantil requerirá una revisión previa de accesibilidad, privacidad y normativa. | §6.1, §9, §10. |
| **D4** | **Español inicial.** Textos de interfaz en recursos de Android (`strings.xml`); contenido educativo separado de la lógica, organizado por idioma. | §4, §6.1. |
| **D5** | **GitHub Actions + Android Studio**, sin servicios de pago. Las pruebas de `:core:domain` verifican las reglas aunque no se pueda compilar Android en el contenedor. No se afirma que la app compila o funciona sin evidencia real. No se insiste en instalar el SDK aquí. | §1, §12 y `docs/CONFIGURACION_ENTORNO.md`. |
| **D6** | **`minSdk 26`** (Android 8.0), si las dependencias lo permiten. Versiones estables y compatibles fijadas en `gradle/libs.versions.toml` en la Fase 2; sin actualizaciones innecesarias. | §2. |
| **D7** | Claude redacta **borradores** en JSON; el responsable los revisa. Antes de integrar un bloque se presenta un resumen (objetivos, conceptos, ejemplos y tipos de ejercicio). Nada se marca como aprobado sin revisión. Las ambigüedades técnicas se señalan, no se inventan soluciones. | §6.1 (`reviewStatus`), §11 (flujo de contenido). |
| **D8** | Identificador **`com.funcode.academy`** (`applicationId` y paquete raíz de Kotlin). | §4. |

### Condiciones permanentes de desarrollo

1. Desarrollo por fases, con autorización explícita antes de cada una.
2. Sin funcionalidades simuladas (una pantalla o unos datos ficticios no son una funcionalidad).
3. Pruebas verificables: se ejecutan, se informan los resultados reales y se registran los errores.
4. XP solo por actividades verificadas; la base de datos impide cobrar dos veces la misma actividad.
5. Progreso persistente en Room (fuente de verdad).
6. MVP sin conexión: sin cuentas, servidores ni suscripciones.
7. Costes controlados: antes de proponer algo con coste se explican su necesidad y las alternativas.
8. Accesibilidad y privacidad desde el principio.
9. `docs/ESTADO_PROYECTO.md` distingue entre **planificado, implementado, probado, aprobado y pendiente**.
10. Protección del alcance: nada fuera del MVP por iniciativa propia.

---

## 1. Inspección del entorno (resultados reales, 2026-10-10)

| Elemento | Resultado | Impacto |
|---|---|---|
| Repositorio `cristianbuitrago-funcode/Academia` | **Público**; solo contiene esta documentación. Rama por defecto actual: `ccr-db27c4de-czvzgb` (no existe `main`). | En repositorios públicos, GitHub Actions es gratuito con los *runners* estándar. |
| JDK | OpenJDK 21.0.12 | Permite compilar y probar `:core:domain` en el contenedor. |
| Gradle | 8.14.3 en `/opt/gradle` | Sirve para generar el *wrapper*. |
| Android SDK | **No instalado**; `dl.google.com` bloqueado por la política de red (403) | **No se compilará Android en este contenedor** (aceptado en D5). |
| `maven.google.com`, `plugins.gradle.org` | Accesibles | — |
| Maven Central | Respondió **429** en las pruebas | Puede impedir incluso las pruebas de `:core:domain` aquí. Si persiste, la evidencia vendrá de GitHub Actions y se dejará registrado. |

---

## 2. Tecnologías

| Área | Elección | Para qué sirve |
|---|---|---|
| Lenguaje | Kotlin | Lenguaje de la app (no del contenido). |
| Interfaz | Jetpack Compose + Material 3 | La UI se describe con funciones Kotlin que se redibujan cuando cambia el estado. |
| Arquitectura | MVVM en capas, una sola `Activity` | El *ViewModel* conserva el estado de la pantalla y llama al dominio; la UI solo muestra ese estado. |
| Navegación | Navigation Compose (rutas tipadas) | Gestiona el cambio de pantallas y el botón "Atrás". |
| Progreso | Room (SQLite) | Base de datos local; **fuente de verdad** del progreso y del XP. |
| Ajustes | DataStore (Preferences) | Pares clave/valor para ajustes (tamaño de texto, público…). |
| Contenido | JSON en `assets/` + kotlinx.serialization | Lecciones legibles y revisables en Git; se cargan sin conexión. |
| Asincronía | Kotlin Coroutines + `Flow` | Lecturas y escrituras de la base de datos sin bloquear la UI. |
| Inyección de dependencias | Manual (`AppContainer`) | Un objeto que crea y comparte los repositorios; sin bibliotecas extra. |
| Pruebas | JUnit (dominio); pruebas de Room y de UI (ver P3 en §14) | — |
| Versiones | `minSdk 26`; versiones fijadas en la Fase 2 en `gradle/libs.versions.toml` (tabla en `docs/ESTADO_PROYECTO.md`) | Evita versiones *alpha/beta*. |

No hay ninguna dependencia de pago ni servicio remoto.

---

## 3. Arquitectura

```
┌──────────────────────── :app (Android) ─────────────────────────┐
│  UI (Compose)  ──►  ViewModels  ──►  Casos de uso (dominio)     │
│                                          │                      │
│                       Repositorios (interfaces del dominio)     │
│                                          │                      │
│   Implementaciones: Room · DataStore · ContentLoader (JSON)     │
└─────────────────────────────────────────────────────────────────┘
            ▲ depende de
┌──────────── :core:domain (Kotlin/JVM puro, sin Android) ────────┐
│ Modelos · Validadores de ejercicios · Reglas de XP, niveles y   │
│ desbloqueo · Interfaces de repositorio · Interfaz CodeRunner    │
└─────────────────────────────────────────────────────────────────┘
```

- **`:core:domain` no conoce Android.** Las reglas educativas y de gamificación se prueban con JUnit en cualquier máquina con JDK.
- **Las dependencias apuntan hacia el dominio:** `:app` implementa las interfaces que el dominio declara (patrón de *repositorio*), así que cambiar Room por otra cosa no tocaría las reglas.
- **El contenido es independiente del lenguaje enseñado** (`languageId: "java"`) y del idioma (`locale: "es"`).

---

## 4. Estructura de carpetas

```
Academia/
├── .github/workflows/ci.yml          (Fase 2)
├── settings.gradle.kts · build.gradle.kts · gradle/libs.versions.toml · gradlew
├── docs/
│   ├── PLAN_TECNICO_FASE1.md · ESTADO_PROYECTO.md · CONFIGURACION_ENTORNO.md
│   └── contenido/GUIA_AUTORES.md      (Fase 3)
├── core/domain/src/
│   ├── main/kotlin/com/funcode/academy/domain/
│   │   ├── model/      content/    exercise/    progress/    runner/    usecase/
│   └── test/kotlin/com/funcode/academy/domain/
└── app/src/
    ├── main/
    │   ├── assets/content/es/java/   course.json · modules/…/lesson-XX.json
    │   ├── java/com/funcode/academy/
    │   │   ├── FuncodeApp.kt · MainActivity.kt · AppContainer.kt
    │   │   ├── data/  local/ (Room) · prefs/ (DataStore) · content/ (carga de JSON)
    │   │   └── ui/    theme/ · navigation/ · components/ · screens/
    │   └── res/values/strings.xml    (español; otros idiomas en values-xx/)
    ├── test/            (pruebas JVM de :app)
    └── androidTest/     (pruebas en dispositivo o emulador)
```

---

## 5. Navegación y pantallas

```
Onboarding (1.ª vez) ──► Inicio ──┬── Mapa de ruta ──► Módulo ──► Lección ──► Ejercicios ──► Resultado
                                  │                                                  │
                                  │                                   Evaluación del módulo ──► Desbloqueo
                                  ├── Perfil (avatar, XP, logros)
                                  └── Ajustes (texto, contraste, animaciones, borrar progreso)
```

| Pantalla | Fase en que se vuelve funcional |
|---|---|
| Inicio, Mapa, Perfil, Ajustes (estructura y navegación) | 2 |
| Lección, Ejercicio, Resultado | 3 |
| Evaluación y desbloqueo | 4 |
| Perfil con XP, niveles y logros | 5 |
| Misiones, retos y proyectos | 6 |
| Onboarding completo y ajustes de accesibilidad | 2 (mínimo) + 7 |

Barra de navegación inferior en teléfonos y *navigation rail* en pantallas anchas (*window size classes*).

---

## 6. Modelo de datos

### 6.1 Contenido (JSON de solo lectura)

```
Course      { id, languageId, locale, version, title, modules[] }
Module      { id, order, title, objectiveIds[], lessons[], assessment, challenge? }
Lesson      { id, order, reviewStatus: DRAFT | APPROVED,
              objectives[{id, text}],            // núcleo común a todos los públicos
              presentations: {                   // adaptación por público
                default: { sections[] },         // obligatoria (MVP: adolescentes y adultos)
                kids?:   { sections[] },         // futuro
                teens?:  { sections[] },         // opcional
                adults?: { sections[] }          // opcional
              },
              exercises[] }
Section     { type: text | code | callout, text?, code?, lineComments?[{line, comment}] }
Exercise    { id, type, difficulty (1-5), objectiveIds[], prompt, payload (según tipo),
              hints[] (progresivas), feedback { correct, incorrect, perOption? }, baseXp }
Assessment  { id, exerciseIds[], passThreshold }
```

**Reglas que protegen D3** (verificadas con pruebas automáticas sobre los JSON):

- Los objetivos, los ejercicios evaluables y `passThreshold` pertenecen al **núcleo**, no a las variantes; una variante no puede cambiarlos.
- Si falta la variante de un público, se usa `default`.
- Cada variante debe cubrir todos los `objectiveIds` de la lección.
- `kids` puede añadir ejercicios de práctica, pero no sustituir a los evaluables (pendiente de concretar cuando se diseñe esa ruta).

**`reviewStatus` (D7):** todo el contenido nuevo entra como `DRAFT`; solo el responsable lo cambia a `APPROVED`. Cómo se comportan los borradores en la app está pendiente (P2).

### 6.2 Tipos de ejercicio del MVP (D2)

| Tipo | Respuesta verificable | Regla de validación |
|---|---|---|
| `SINGLE_CHOICE` / `MULTI_CHOICE` | Índice(s) correcto(s) | Igualdad exacta del conjunto. |
| `MATCH_PAIRS` (relacionar conceptos) | Pares correctos | Todos los pares coinciden. |
| `PREDICT_OUTPUT` | Texto esperado | Comparación tras normalizar saltos de línea (`\r\n`→`\n`) y espacios finales de línea. Mayúsculas y espacios internos **sí** cuentan, porque en Java importan. |
| `FILL_IN_CODE` | Valor(es) del hueco | Lista cerrada de respuestas aceptadas por hueco, comparadas por tokens (ignora los espacios que no afectan al significado). |
| `ORDER_LINES` | Orden correcto | Secuencia exacta, o varios órdenes válidos declarados explícitamente. |
| `FIND_ERROR` | Línea(s) con error y, opcionalmente, tipo de error | Igualdad con la línea o el tipo declarados. |
| `STRUCTURAL_CODE` (respuesta escrita) | Reglas declaradas | Solo reglas que se puedan comprobar de forma fiable sobre los tokens: "declara una variable `int` llamada `edad`", "usa `System.out.println`", "contiene un `if`". **No** se usa para programas cuyo resultado dependa de su ejecución. |

Cada validador tendrá pruebas unitarias con respuestas correctas, incorrectas y casos límite. Si una pregunta admite varias respuestas razonables que la regla no puede distinguir, se reformula la pregunta; no se acepta una regla dudosa.

### 6.3 Progreso del usuario (Room)

| Tabla | Campos principales |
|---|---|
| `profile` | id, nickname, audience (`KIDS`/`TEENS`/`ADULTS`; en el MVP solo `TEENS`/`ADULTS`), avatarId, createdAt |
| `exercise_attempt` | id, exerciseId, timestamp, correct, hintsUsed |
| `exercise_progress` | exerciseId (PK), firstSolvedAt, hintsUsedOnFirstSolve |
| `lesson_progress` | lessonId (PK), status, completedAt |
| `module_progress` | moduleId (PK), status (`LOCKED`/`AVAILABLE`/`COMPLETED`/`MASTERED`), bestAssessmentScore |
| `xp_ledger` | id, sourceType, sourceId, amount, timestamp — **`UNIQUE(sourceType, sourceId)`** |
| `achievement_unlocked` | achievementId (PK), unlockedAt |

- El XP total y el nivel se **calculan** a partir de `xp_ledger`; no hay un contador que se pueda editar.
- La restricción `UNIQUE` hace que la base de datos rechace un segundo registro de XP para la misma actividad (condición 4).
- No se guarda el texto libre de las respuestas, para minimizar datos.

### 6.4 Reglas de gamificación

- XP solo en la **primera** resolución correcta de un ejercicio, lección o evaluación.
- Las pistas reducen el XP del ejercicio (valores exactos: P6).
- Repetir un ejercicio ya resuelto sirve de práctica, pero da 0 XP.
- Un módulo se desbloquea al aprobar la evaluación del anterior (`passThreshold`, propuesta 0,8; P6).
- Los logros son hitos de aprendizaje, no de actividad bruta.

---

## 7. Persistencia y modo sin conexión

- Contenido en `assets` y progreso en Room: el MVP funciona **100 % sin conexión**.
- Esquema de Room versionado desde el principio (`exportSchema = true`, esquemas en Git) y migraciones probadas.
- Los IDs de contenido son estables; si un ejercicio cambia de ID, se añade una equivalencia para no perder el progreso.
- La copia de seguridad automática de Android incluye solo la base de datos de progreso.
- Sin sincronización en la nube en el MVP.

---

## 8. Ejecución de código Java

### 8.1 Decisión (D2)

MVP: **opción A** (respuestas comprobadas sin ejecución). Las opciones B (servicio remoto aislado) y C (compilación en el dispositivo en un proceso aislado) se estudiarán en la Fase 8, con mediciones y pruebas de seguridad antes de decidir.

### 8.2 Interfaz `CodeRunner` (solo diseño; sin implementación en el MVP)

Se ubicará en `core/domain/.../runner/`:

```kotlin
interface CodeRunner {
    /** Qué puede hacer este ejecutor; la UI decide si ofrece "Ejecutar". */
    val capabilities: RunnerCapabilities

    suspend fun run(request: RunRequest): RunResult
}

data class RunnerCapabilities(val canExecute: Boolean, val supportsStdin: Boolean)

data class RunRequest(
    val languageId: String,          // "java"
    val sourceFiles: Map<String, String>,
    val stdin: String = "",
    val limits: RunLimits = RunLimits()
)

data class RunLimits(val timeoutMs: Long = 3_000, val maxOutputChars: Int = 10_000)

sealed interface RunResult {
    data class Completed(val stdout: String, val stderr: String, val exitCode: Int) : RunResult
    data class CompilationFailed(val diagnostics: List<String>) : RunResult
    data class TimedOut(val partialStdout: String) : RunResult
    data class Unavailable(val reason: String) : RunResult
}
```

- En el MVP **no se registra ninguna implementación** y la UI no muestra ningún botón "Ejecutar". No se crean ejecutores falsos.
- La interfaz se añadirá en el módulo de dominio cuando se autorice la fase correspondiente (propuesta: Fase 3, junto a los validadores), solo como contrato documentado.

### 8.3 Fase 8 (se conserva)

Prototipos de B y/o C con criterios medibles: tamaño del APK, tiempo de compilación y ejecución, comportamiento ante bucles infinitos, intentos de acceder a archivos o a la red, consumo de memoria y, en el caso de B, coste mensual estimado y datos que salen del dispositivo. Ninguna opción se integra sin pruebas de aislamiento superadas.

### 8.4 Autenticidad del Java enseñado

- Todo fragmento Java de las lecciones debe ser válido para **Java 17 o posterior** (versión LTS de referencia: pendiente P7).
- Una prueba automática extraerá los fragmentos marcados como "programa completo" de los JSON y los **compilará con `javac`** en CI, y comparará su salida real con la `expected` de los ejercicios `PREDICT_OUTPUT`. Esto se ejecuta **en la máquina de pruebas, no en el teléfono**, y sirve para garantizar que las respuestas son correctas. No contradice D2 (propuesta pendiente P8).

### 8.5 Mensajes en la interfaz

- Se dice "Respuesta comprobada" o "Correcto/Incorrecto", **nunca** "Programa ejecutado" ni "Salida del programa" para algo que no se ejecutó.
- En los ejercicios de salida se habla de "salida esperada".

---

## 9. Seguridad y privacidad

- Sin cuentas, correo ni fecha de nacimiento: solo apodo opcional y público (adolescente/adulto).
- Sin publicidad, analítica de terceros ni permiso `INTERNET` en el MVP (no hace falta).
- Datos solo en el dispositivo; opción "Borrar todo mi progreso".
- La ruta infantil exige antes de publicarse una revisión específica: Families Policy de Google Play, COPPA/GDPR (consentimiento para menores) y accesibilidad para niños (D3).
- Sin claves ni secretos en el repositorio; la firma de versiones de publicación queda fuera del MVP.

---

## 10. Accesibilidad e internacionalización

- Todo texto de interfaz en `strings.xml`, nada escrito directamente en el código (D4). El contenido educativo, en JSON por idioma.
- Respeta el tamaño de fuente del sistema; contraste AA; objetivos táctiles de al menos 48 dp; `contentDescription` en los elementos interactivos.
- Bloques de código en fuente monoespaciada, con desplazamiento horizontal y legibles con TalkBack.
- Opción de reducir animaciones; temas por público diseñados con los mismos componentes.

---

## 11. Flujo de creación y revisión de contenido (D7)

1. Claude presenta un **resumen del bloque**: objetivos, conceptos, ejemplos previstos, tipos de ejercicio, evaluación y dudas técnicas detectadas.
2. El responsable aprueba o corrige el resumen.
3. Claude redacta los JSON con `reviewStatus: "DRAFT"` y pasa las validaciones automáticas (esquema, cobertura de objetivos, compilación de fragmentos si se aprueba P8).
4. El responsable revisa y cambia a `APPROVED`, o pide cambios.
5. `ESTADO_PROYECTO.md` registra el estado de cada bloque.

Ruta de Java (15 módulos): lógica de programación → primer programa → variables, tipos y operadores → entrada de datos → condicionales → bucles → métodos → arreglos y colecciones → POO → encapsulamiento, herencia y polimorfismo → excepciones → archivos → estructuras de datos y algoritmos → depuración, pruebas y buenas prácticas → proyectos integradores.

---

## 12. Compilación y pruebas (D5)

| Dónde | Qué se comprueba | Evidencia |
|---|---|---|
| Contenedor de Claude | `./gradlew :core:domain:test` (si Maven Central responde) | Salida de la consola copiada al registro. |
| GitHub Actions | `:core:domain:test`, pruebas JVM de `:app`, *lint* y `assembleDebug` | Enlace a la ejecución del *workflow* y su estado. |
| Android Studio (responsable) | Ejecución en emulador o dispositivo, revisión visual, TalkBack | Confirmación del responsable y, si es posible, capturas. |

Los pasos de preparación están en **`docs/CONFIGURACION_ENTORNO.md`**.

---

## 13. Plan por fases y criterios de aceptación

| Fase | Objetivo |
|---|---|
| 1. Planificación | ✔ Plan y decisiones D1–D8 aprobados. |
| **2. Esqueleto y CI** | Proyecto Gradle, navegación básica, Room/DataStore, CI. |
| 3. Motor de lecciones y ejercicios | Carga de JSON, 7 tipos de ejercicio con validadores, pistas, retroalimentación, registro de intentos; contrato `CodeRunner`. |
| 4. Contenido de los módulos 1–3 + evaluación | Contenido revisado (D7); desbloqueo por dominio. |
| 5. RPG básico | XP, niveles, perfil, avatar y logros. |
| 6. Misiones, retos y proyectos | Retos ligados a objetivos. |
| 7. Personalización y accesibilidad | Ajustes, tabletas, auditoría con TalkBack. |
| 8. Investigación de la ejecución real | Prototipos B/C con mediciones. |
| 9+. Contenido de los módulos 4–15 | Por bloques, con el flujo de §11. |

### 13.1 Criterios de aceptación de la Fase 2

**A. Estructura y configuración**
1. Proyecto con módulos `:app` (Android) y `:core:domain` (Kotlin/JVM, sin dependencias de Android).
2. `applicationId` y paquete raíz `com.funcode.academy`; `minSdk 26`.
3. Todas las versiones en `gradle/libs.versions.toml`, solo versiones estables; *Gradle wrapper* incluido en el repositorio.
4. Sin dependencias de pago ni permiso `INTERNET`.

**B. Interfaz mínima (sin funciones simuladas)**
5. Tema Material 3 con soporte de modo claro/oscuro y respeto del tamaño de fuente del sistema.
6. Navegación entre Inicio, Mapa, Perfil y Ajustes con barra inferior; el botón "Atrás" se comporta correctamente.
7. Las pantallas aún sin funcionalidad muestran un estado vacío honesto ("Disponible en una próxima versión") y no datos ficticios presentados como reales.
8. Todos los textos visibles en `strings.xml` (español).
9. Elementos interactivos con descripción para accesibilidad.

**C. Persistencia**
10. Base de datos Room creada con el esquema inicial (§6.3), incluida la restricción `UNIQUE` de `xp_ledger`; esquema exportado a Git.
11. Una prueba automatizada demuestra que insertar dos veces XP para la misma actividad **no** duplica el XP.
12. DataStore guarda al menos un ajuste real (p. ej., el público elegido) y una prueba comprueba que persiste.

**D. Dominio**
13. `:core:domain` contiene los modelos básicos y las interfaces de repositorio, con pruebas unitarias que pasan.

**E. Integración continua**
14. `.github/workflows/ci.yml` se ejecuta en cada *push* y *pull request*: `:core:domain:test`, pruebas de `:app`, *lint* y `assembleDebug`.
15. La ejecución de CI termina en **verde** en la rama de trabajo, con enlace registrado en `ESTADO_PROYECTO.md`.
16. El APK de depuración queda disponible como artefacto de la ejecución.

**F. Verificación manual (responsable)**
17. El responsable abre el proyecto en Android Studio, lo sincroniza sin errores y ejecuta la app en un emulador o dispositivo con API 26 o superior.
18. Navegación entre las 4 pantallas, rotación de pantalla y cierre/reapertura sin cierres inesperados; el ajuste guardado se conserva.

**G. Documentación**
19. `ESTADO_PROYECTO.md` actualizado con lo implementado, probado (con evidencia), pendiente y limitaciones.
20. Breve explicación de los componentes creados (qué hace cada archivo principal).

Los criterios 17–18 dependen de una verificación del responsable; hasta entonces la Fase 2 se registrará como "implementada y probada en CI, pendiente de verificación en dispositivo", no como "aprobada".

---

## 14. Cuestiones técnicas pendientes de decisión

| # | Cuestión | Propuesta | Cuándo hace falta |
|---|---|---|---|
| ~~P1~~ | Rama principal | **Aprobado (2026-10-10):** `main` estable y cambios por PR. Ver `docs/FLUJO_DE_TRABAJO.md`. | Resuelto |
| **P2** | ¿Cómo se comportan los borradores (`DRAFT`) en la app? | Se muestran con la etiqueta "Borrador" solo en las compilaciones de depuración; las de publicación los excluyen. | Fase 3. |
| ~~P3~~ | Pruebas de Room y de UI | **Aprobado con condiciones (2026-10-10):** Robolectric cuando aporte valor comprobable; no sustituye la prueba en dispositivo. | Resuelto |
| ~~P4~~ | Permisos del *workflow* | **Resuelto:** el *push* del *workflow* funcionó. | Resuelto |
| **P5** | Avatar: ¿ilustraciones propias, iconos de Material o una biblioteca con licencia libre? | Iconos/formas de Material en el MVP; ilustraciones después. | Fase 5. |
| **P6** | Valores de XP: XP base por dificultad, penalización por pista, curva de niveles y umbral de evaluación (propuesta 80 %). | Se propondrán con ejemplos numéricos. | Fase 5 (umbral: Fase 4). |
| **P7** | Versión de Java de referencia del contenido. | Java 17 LTS (sintaxis estable y ampliamente instalada); mencionar novedades de 21 solo cuando aporten. | Fase 3. |
| **P8** | ¿Compilar en CI con `javac` los fragmentos Java del contenido para verificar las respuestas (§8.4)? | Sí: garantiza la corrección técnica sin ejecutar nada en el teléfono. | Fase 4. |
| **P9** | Licencia del código y del contenido (p. ej., código abierto o "todos los derechos reservados"); el repositorio es público. | Decisión tuya; mientras no exista, nadie tiene permiso legal para reutilizarlo. | Antes de publicar. |
| **P10** | Identidad visual: nombre visible, icono y paleta de colores. | Paleta provisional de Material 3 hasta definirla. | Fase 2 (provisional) / 7. |

---

## 15. Registro de decisiones técnicas

| Fecha | Decisión | Motivo breve |
|---|---|---|
| 2026-10-10 | Kotlin + Compose + Material 3 (D1) | Estándar actual de Android; menos código; adaptación visual sencilla. |
| 2026-10-10 | Módulos `:app` y `:core:domain` (D1) | Reglas probables sin Android; separación de capas. |
| 2026-10-10 | Ejercicios comprobados sin ejecución en el MVP; interfaz `CodeRunner` sin implementación (D2) | Seguridad, funcionamiento sin conexión y coste cero; la ejecución real necesita investigación. |
| 2026-10-10 | Variantes de contenido por público con núcleo común (D3) | La adaptación no puede alterar los objetivos ni la evaluación. |
| 2026-10-10 | `strings.xml` + contenido JSON por idioma (D4) | Traducción futura sin rehacer la app. |
| 2026-10-10 | GitHub Actions + Android Studio (D5) | El contenedor no tiene Android SDK; CI gratuito en un repositorio público. |
| 2026-10-10 | `minSdk 26` (D6) | Compatibilidad amplia y APIs modernas. |
| 2026-10-10 | Contenido como borrador hasta su revisión (D7) | Control de calidad pedagógica y técnica. |
| 2026-10-10 | `com.funcode.academy` (D8) | Identificador aprobado. |
| 2026-10-10 | AGP 9.4 con Kotlin integrado; todos los plugins declarados en el archivo raíz | Versión estable actual; un único *classloader* evita errores entre los plugins de Kotlin y Android (detectado en CI). |
| 2026-10-10 | JDK 21 en CI y `targetSdk` 36 | Requisitos de Robolectric para simular Android 16. |
| 2026-10-10 | Modo "solo dominio" (`-Pfuncode.domainOnly=true`) | Permite probar las reglas sin Android SDK (p. ej., en el contenedor de Claude). |
| 2026-10-13 | Fase 3: siete tipos según la autorización (verdadero/falso incluido; `STRUCTURAL_CODE` pendiente, F3-D1) | La autorización de la fase prevalece sobre la lista de §6.2; la diferencia queda registrada. |
| 2026-10-13 | Lectura y validación del JSON del contenido en `:core:domain` (kotlinx.serialization) | Es Kotlin puro y comprobable sin Android; la app solo aporta el texto leído de `assets`. |
| 2026-10-13 | Solo se registran intentos (`exercise_attempt`); resuelto/no resuelto se deduce | Evita datos redundantes; sin cambios de esquema (F3-D2 para las tablas sin uso). |
| 2026-10-13 | Ordenar líneas se valida por el texto resultante, no por índices | Dos líneas idénticas (p. ej. `}`) son intercambiables sin cambiar el programa. |
| 2026-10-13 | Fase 4: la evaluación es una lección con `kind: assessment` (una respuesta por pregunta, sin pistas) | Reutiliza el motor y el validador; la corrección es la misma que en las lecciones. |
| 2026-10-13 | Umbral de aprobación opcional en el contenido; no se declara mientras P6 esté pendiente | No se inventa una regla educativa no aprobada; se muestra el resultado sin «aprobado». |
| 2026-10-13 | Progreso del módulo derivado de `exercise_attempt`; mejor resultado en `module_progress` | Sin cambio de esquema ni migración; repetir una actividad no duplica nada. |
| 2026-10-13 | `JavaContentCompilationTest` con `javac --release 17` (P7 provisional) en las pruebas del dominio | Cumple P8 sin herramientas externas; el código se compila en la JVM de las pruebas, nunca en la app. |
| 2026-10-13 | Inicio muestra una tarjeta «Continuar» en lugar de la lista de lecciones; Mapa lista los módulos | Con 8 actividades, la lista plana dejaba de ser manejable y duplicaba la pantalla del módulo. |
| 2026-10-13 | P6 aprobado: XP 10 + 5 × (dificultad − 1), −25 % por pista (mínimo 50 %), lección +20, evaluación aprobada +100; curva 100 × n; umbral 80 % | Decisión del responsable sobre la propuesta presentada antes de programar la Fase 5. |
| 2026-10-13 | Recompensas recalculadas desde los resultados verificados (`RewardEngine`) en lugar de sumarse en cada pantalla | Un solo sistema, idempotente: reintentos, reinicios y fallos no duplican ni pierden recompensas. |
| 2026-10-13 | Monedas fuera de la Fase 5 | Decisión del responsable: sin tienda no tienen uso; queda una propuesta documentada. |
| 2026-10-13 | Fase 6: reto guiado por módulo (`kind: challenge`), abierto al completar las lecciones; 50 XP una vez; misiones de objetivos sin reloj ni XP | Decisiones del responsable antes de programar; los retos usan los 7 tipos de ejercicio (sin ejecución de Java, D2). |
| 2026-10-13 | Fase 6 (especificación completa): misiones como contenido con requisitos declarados y 10 XP de bonificación una vez (`MISSION`); sustituyen a las misiones fijas; pantalla Misiones desde Inicio y Mapa; el reto es el proyecto guiado (situación, conceptos previos, resumen) | Decisiones del responsable antes de programar; estados derivados, sin cambio de esquema. |

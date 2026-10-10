# Funcode Academy — Plan técnico (Fase 1: Planificación y arquitectura)

> Estado: **propuesta pendiente de aprobación**. Este documento no implica que exista código de la aplicación.

---

## 1. Inspección del entorno (resultados reales, 2026-10-10)

| Elemento | Resultado | Impacto |
|---|---|---|
| Repositorio `Academia` | Vacío, sin commits previos | No hay funcionalidades existentes que preservar ni conflictos. |
| JDK | OpenJDK 21.0.12 (`java`, `javac`) | Sirve para compilar y probar módulos JVM puros (dominio, validadores). |
| Gradle | 8.14.3 instalado en `/opt/gradle` (Kotlin DSL 2.0.21 embebido) | Suficiente para el *wrapper* del proyecto. |
| Android SDK (`ANDROID_HOME`, `sdkmanager`, `adb`) | **No instalado** | No se puede compilar un APK ni ejecutar emulador en este contenedor. |
| `dl.google.com` (descarga del SDK) | **Bloqueado (403 del proxy de red)** | Impide instalar el Android SDK aquí. |
| `maven.google.com` (AGP, AndroidX) | Accesible | Dependencias Android resolubles si hubiera SDK. |
| `plugins.gradle.org`, `services.gradle.org` | Accesibles | — |
| Maven Central (`repo.maven.apache.org`) | Respondió **429 (límite de peticiones)** en repetidas pruebas | Riesgo para descargar JUnit, Kotlin, etc. Hay que volver a comprobarlo en la Fase 2. |
| Hardware | 4 CPU, 15 GB RAM | Suficiente para compilar; sin aceleración para emulador. |

**Conclusión:** en este entorno en la nube **no es posible compilar ni probar la app Android**. Opciones para la Fase 2 (decisión pendiente D5):

1. Añadir `dl.google.com` a los dominios permitidos del entorno (Configuración del entorno → *Network access* → *Allowed domains*; ver https://code.claude.com/docs/en/cloud-environments#network-access) para instalar el SDK aquí.
2. Compilar y ejecutar en tu equipo con Android Studio (necesario de todos modos para probar en emulador o dispositivo real).
3. Compilar y ejecutar pruebas en GitHub Actions (los *runners* de Ubuntu traen el Android SDK).

Lo recomendado es **2 + 3**: Android Studio para desarrollo visual y pruebas manuales, y GitHub Actions para tener una verificación automática y reproducible de cada cambio. La opción 1 es complementaria.

---

## 2. Tecnologías recomendadas

| Área | Recomendación | Motivo | Alternativa |
|---|---|---|---|
| Lenguaje de la app | **Kotlin** | Lenguaje oficial preferente de Android, menos código repetitivo, corrutinas para trabajo asíncrono. | Java (ver decisión D1). |
| Interfaz | **Jetpack Compose + Material 3** | Estándar actual, adaptable a tamaños de pantalla, temas (por edad) y accesibilidad más sencillos. | Vistas XML (solo si se elige Java). |
| Arquitectura | **MVVM + capas (UI / dominio / datos)**, una sola *Activity* | Separación clara, fácil de probar, recomendada por Google. | — |
| Navegación | **Navigation Compose** (rutas tipadas) | Oficial, soporta *back stack* y *deep links*. | — |
| Persistencia del progreso | **Room (SQLite)** | Datos relacionales (progreso por ejercicio, intentos, logros), consultas, migraciones. | — |
| Preferencias | **DataStore (Preferences)** | Ajustes de perfil, accesibilidad, tema. Reemplazo moderno de SharedPreferences. | — |
| Contenido educativo | **Archivos JSON versionados en `assets/`** | Funciona sin conexión, revisable en Git, independiente del lenguaje enseñado. | Descarga remota en el futuro. |
| Inyección de dependencias | **Manual (`AppContainer`)** al inicio | Evita dependencias y complejidad innecesarias en un proyecto de aprendizaje. | Hilt cuando el proyecto crezca. |
| Serialización | **kotlinx.serialization** | Ligera, sin reflexión, oficial de Kotlin. | Moshi / Gson. |
| Resaltado de código | Implementación propia sencilla (tokenizador de palabras clave Java → `AnnotatedString`) | Evita una dependencia pesada; solo hace falta resaltar. | Biblioteca de terceros si se requiere un editor avanzado. |
| Pruebas | JUnit (dominio), pruebas de Room, pruebas de UI de Compose, Robolectric opcional | Las reglas de XP, desbloqueo y validación deben estar cubiertas por pruebas. | — |
| Compatibilidad | `minSdk 26` (Android 8.0), `targetSdk`/`compileSdk` = el más reciente estable en la Fase 2 | Cubre la gran mayoría de dispositivos activos. | `minSdk 24` si se necesita más alcance. |
| Servicios de pago | **Ninguno** en el MVP | Requisito del proyecto. | — |

Las versiones exactas (AGP, Kotlin, Compose BOM) se fijarán y verificarán al crear el proyecto en la Fase 2, usando un *version catalog* (`gradle/libs.versions.toml`).

---

## 3. Arquitectura

```
┌──────────────────────── :app (Android) ─────────────────────────┐
│  UI (Compose)  ──►  ViewModels  ──►  Casos de uso (dominio)     │
│                                          │                      │
│                       Repositorios (interfaces del dominio)     │
│                                          │                      │
│        Implementaciones: Room · DataStore · ContentLoader(JSON) │
└─────────────────────────────────────────────────────────────────┘
            ▲ depende de
┌──────────── :core:domain (Kotlin/JVM puro, sin Android) ────────┐
│ Modelos · Reglas de XP y niveles · Reglas de desbloqueo ·       │
│ Validadores de ejercicios · Interfaces de repositorio           │
└─────────────────────────────────────────────────────────────────┘
```

Decisiones clave:

- **El dominio es un módulo JVM puro.** Las reglas que más importan (validar respuestas, otorgar XP, desbloquear contenido) se pueden probar con JUnit sin Android, incluso en este contenedor.
- **El contenido es independiente del lenguaje enseñado.** Cada curso tiene `languageId` (`"java"`), y los ejercicios usan tipos genéricos. Añadir Python más adelante significa añadir contenido JSON (y, si existe, un ejecutor), no reescribir la app.
- **La adaptación por edad es una capa de presentación del contenido.** Una lección tiene un núcleo técnico común y variantes de texto/ejemplos por perfil (`kids`, `teens`, `adults`). Los conceptos y los criterios de evaluación son los mismos en todas las variantes, así la adaptación no puede ocultar contenido obligatorio.
- **Fuente única de verdad:** el progreso se guarda en Room y la UI lo observa mediante `Flow`.

Se empieza con solo dos módulos (`:app`, `:core:domain`). Dividir en más módulos por funcionalidad solo si el tamaño lo justifica.

---

## 4. Estructura de carpetas propuesta

```
Academia/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
├── docs/
│   ├── PLAN_TECNICO_FASE1.md        ← este documento
│   ├── ESTADO_PROYECTO.md           ← registro de calidad
│   └── contenido/GUIA_AUTORES.md    ← formato de lecciones (Fase 3)
├── core/domain/
│   └── src/main/kotlin/com/funcode/academy/domain/
│       ├── model/        (Course, Module, Lesson, Exercise, Profile, Progress, Achievement…)
│       ├── content/      (interfaz ContentRepository)
│       ├── exercise/     (ExerciseValidator y validadores por tipo)
│       ├── progress/     (XpPolicy, LevelCurve, UnlockPolicy, MasteryPolicy)
│       └── usecase/
│   └── src/test/kotlin/…  (pruebas unitarias JVM)
└── app/
    └── src/main/
        ├── assets/content/java/
        │   ├── course.json
        │   └── modules/01-intro/lesson-01.json …
        ├── java/com/funcode/academy/
        │   ├── FuncodeApp.kt, MainActivity.kt, AppContainer.kt
        │   ├── data/
        │   │   ├── local/   (Room: entidades, DAOs, AppDatabase, migraciones)
        │   │   ├── prefs/   (DataStore)
        │   │   └── content/ (lectura de JSON desde assets)
        │   └── ui/
        │       ├── theme/   (tema por edad, tipografía escalable, alto contraste)
        │       ├── navigation/
        │       ├── components/ (CodeBlock, XpBar, HintPanel…)
        │       └── screens/ (onboarding, home, map, lesson, exercise, result,
        │                     profile, achievements, settings)
        └── res/
```

---

## 5. Navegación y pantallas principales

```
Onboarding (primera vez) ──► Inicio
                               │
       ┌───────────────┬───────┴────────┬──────────────┐
       ▼               ▼                ▼              ▼
  Mapa de ruta     Misiones          Perfil        Ajustes
       │          (Fase 6)         (avatar, XP,   (edad, tamaño
       ▼                            logros)        de texto, contraste)
  Módulo ──► Lección (teoría + ejemplos comentados)
                 │
                 ▼
            Ejercicios (uno por pantalla, pistas progresivas)
                 │
                 ▼
            Resultado / retroalimentación ──► Evaluación del módulo ──► Desbloqueo
```

| Pantalla | Propósito | Fase |
|---|---|---|
| Onboarding | Apodo, franja de edad (no fecha de nacimiento), avatar básico, nivel previo | 2–4 |
| Inicio | Continuar donde se quedó, resumen de XP y nivel | 2 |
| Mapa de ruta | Los 15 módulos de Java con su estado (bloqueado / disponible / completado / dominado) | 2–3 |
| Lección | Objetivos, explicación, ejemplos comentados línea por línea | 3 |
| Ejercicio | Tipos validados (ver §8), pistas, retroalimentación explicada | 3 |
| Evaluación | Prueba práctica del módulo; desbloquea el siguiente | 4 |
| Perfil y logros | Avatar, nivel, XP, insignias | 5 |
| Misiones/retos | Retos y proyectos por etapa | 6 |
| Ajustes | Público, tamaño de texto, alto contraste, reducir animaciones, borrar datos | 2 + 7 |

Navegación inferior con 4 destinos (Inicio, Mapa, Perfil, Ajustes) en teléfonos; *navigation rail* en tabletas y pantallas grandes (*window size classes*).

---

## 6. Modelo de datos

### 6.1 Contenido (JSON de solo lectura, en `assets`)

```
Course      { id, languageId, title, version, modules[] }
Module      { id, order, title, objectives[], lessons[], assessmentId, project? }
Lesson      { id, order, objectives[], sections[], exercises[],
              variants: { kids?, teens?, adults } }   // texto adaptado; mismo núcleo
Section     { type: text | code | callout, content, codeLines?[{code, comment}] }
Exercise    { id, type, difficulty(1-5), prompt, payload(según tipo),
              hints[] (progresivas), feedback{correct, incorrect, perOption?}, xp }
Assessment  { id, exerciseIds[], passThreshold (p. ej. 0.8) }
Achievement { id, title, description, condition }
```

Ejemplo de ejercicio (tipo "predecir la salida"):

```json
{
  "id": "java.m02.l01.e03",
  "type": "PREDICT_OUTPUT",
  "difficulty": 1,
  "prompt": "¿Qué imprime este programa?",
  "payload": {
    "code": "System.out.println(\"Hola\");\nSystem.out.println(2 + 3);",
    "expected": "Hola\n5"
  },
  "hints": ["println imprime y salta de línea.", "2 + 3 son números, no texto."],
  "feedback": {
    "correct": "Correcto: la segunda línea suma dos enteros antes de imprimir.",
    "incorrect": "Revisa: \"2 + 3\" sin comillas es una operación aritmética."
  },
  "xp": 10
}
```

### 6.2 Progreso del usuario (Room)

| Tabla | Campos principales |
|---|---|
| `profile` | id, nickname, ageBand (`KIDS`/`TEENS`/`ADULTS`), avatarId, createdAt |
| `exercise_attempt` | id, exerciseId, timestamp, correct, hintsUsed, answer (texto) |
| `exercise_progress` | exerciseId (PK), firstSolvedAt, bestScore, xpAwarded |
| `lesson_progress` | lessonId (PK), status, completedAt |
| `module_progress` | moduleId (PK), status (LOCKED/AVAILABLE/COMPLETED/MASTERED), assessmentBestScore |
| `xp_ledger` | id, sourceType, sourceId, amount, timestamp — **UNIQUE(sourceType, sourceId)** |
| `achievement_unlocked` | achievementId (PK), unlockedAt |

El XP total y el nivel **se calculan** a partir de `xp_ledger`; no se guardan como un número editable. La restricción `UNIQUE` impide a nivel de base de datos que una misma actividad otorgue XP dos veces.

### 6.3 Reglas anti-abuso (sistema RPG)

- El XP se otorga solo la **primera vez** que se resuelve correctamente un ejercicio, una lección o una evaluación.
- Usar pistas reduce el XP de ese ejercicio (p. ej. −25 % por pista, con un mínimo).
- Repetir un ejercicio ya resuelto sirve para practicar, pero da 0 XP.
- Un módulo se desbloquea al **aprobar la evaluación** del anterior (≥ 80 %), no por tiempo ni por número de pulsaciones.
- Los logros se basan en hitos de aprendizaje (p. ej. "primer bucle correcto sin pistas"), no en la actividad bruta.

---

## 7. Persistencia y funcionamiento sin conexión

- Todo el MVP funciona **100 % sin conexión**: contenido en `assets` y progreso en Room.
- Migraciones de Room versionadas desde el primer esquema (con `exportSchema = true` y esquemas en Git).
- El contenido tiene `version`; si un ejercicio cambia de id, se mantiene una tabla de equivalencias para no perder progreso.
- Copia de seguridad: Android Auto Backup activado solo para la base de datos de progreso (sin datos sensibles).
- La sincronización en la nube **no** forma parte del MVP (requeriría cuentas y un backend).

---

## 8. Ejecución de código Java: análisis de opciones

Android no incluye `javac` y su máquina virtual (ART) ejecuta *bytecode* DEX, no `.class`. Ejecutar Java de verdad no es trivial.

### Opción A — Ejercicios validados sin ejecución (recomendada para el MVP)

Tipos: opción múltiple, completar huecos en código, ordenar líneas, predecir la salida, encontrar el error, verdadero/falso, emparejar conceptos. Para la escritura libre de código, una **validación estructural** (comparación normalizada de tokens o reglas como "debe declarar un `int` llamado `edad`").

- ✅ Sin conexión, sin riesgos de seguridad, sin costes, retroalimentación precisa y explicada.
- ❌ El estudiante no ve su propio programa ejecutarse; la escritura libre se valida de forma limitada.
- La interfaz nunca dirá "ejecutado": dirá "respuesta comprobada".

### Opción B — Servicio de ejecución aislado (remoto)

Un servidor recibe el código, lo compila y ejecuta en un contenedor efímero y devuelve stdout/stderr.

- Variantes: servidor propio con contenedores endurecidos (sin red, límites de CPU/memoria/tiempo/procesos, sistema de archivos de solo lectura, gVisor o nsjail); o **Judge0** (código abierto, autoalojable).
- ✅ Ejecución real con un JDK completo; permite casos de prueba ocultos para corregir programas.
- ❌ Requiere conexión, un servidor (coste de alojamiento, aunque sea bajo), mantenimiento y seguridad operativa; el código del estudiante sale del dispositivo (hay que informarlo en la política de privacidad, más sensible con menores).
- Las API públicas gratuitas de terceros no son fiables para producción y no deberían usarse sin evaluar sus condiciones.

### Opción C — Compilación y ejecución en el dispositivo

Empaquetar un compilador Java (p. ej. ECJ de Eclipse) y D8 para convertir a DEX, y cargarlo con `DexClassLoader` dentro de un **servicio en un proceso aislado** (`android:isolatedProcess="true"`) con límite de tiempo.

- ✅ Sin conexión y con ejecución real.
- ❌ Alta complejidad técnica; aumenta mucho el tamaño del APK; hay que proporcionar las clases de la biblioteca estándar que el compilador necesita; Android 14+ exige que el código cargado dinámicamente sea de solo lectura; hay que simular `System.in`; el aislamiento depende del proceso aislado y de un *watchdog* para bucles infinitos. Viabilidad no comprobada.

### Recomendación

1. **MVP: opción A** (Fases 3–4).
2. **Fase 8: prueba técnica acotada** de la opción C *y/o* de la opción B, con criterios medibles (tamaño del APK, tiempo de compilación, aislamiento demostrado ante bucles infinitos y acceso a archivos/red). Decidir según los resultados.
3. La interfaz `CodeRunner` del dominio se definirá desde el principio para que la opción elegida pueda enchufarse sin cambiar las pantallas.

---

## 9. Seguridad y privacidad

- Sin cuentas, sin correo y sin fecha de nacimiento en el MVP: solo apodo y franja de edad.
- Sin publicidad ni SDK de analítica de terceros en el MVP.
- Datos solo en el dispositivo; opción en Ajustes para "borrar todo mi progreso".
- Si la app se dirige a niños, en Google Play hay que cumplir la *Families Policy* (y COPPA/GDPR-K según el país). Esto condiciona futuras decisiones (anuncios, analítica, inicio de sesión).
- `android:allowBackup` configurado para incluir solo la base de datos de progreso.
- Si llega la opción B: HTTPS, sin identificadores personales en las peticiones y límites de uso.

---

## 10. Accesibilidad y adaptación

- Tipografía escalable (respeta el tamaño de fuente del sistema), contraste AA, modo de alto contraste, "reducir animaciones".
- `contentDescription` en todos los elementos interactivos; objetivos táctiles ≥ 48 dp.
- Bloques de código con fuente monoespaciada, desplazamiento horizontal y lectura compatible con TalkBack.
- Temas visuales por público (más ilustrado para niños, más sobrio para adultos) con los mismos componentes.

---

## 11. Plan por fases y criterios de aceptación

Cada fase termina con pruebas ejecutadas y documentadas en `docs/ESTADO_PROYECTO.md`, y espera tu autorización.

| Fase | Objetivo | Criterios de aceptación |
|---|---|---|
| **1. Planificación** (actual) | Este plan | Plan entregado; decisiones pendientes respondidas por ti. |
| **2. Esqueleto del proyecto** | Proyecto Gradle (`:app`, `:core:domain`), tema, navegación entre pantallas vacías (Inicio, Mapa, Perfil, Ajustes), Room y DataStore configurados, CI en GitHub Actions | `./gradlew assembleDebug` y `./gradlew test` pasan en CI; la app abre en emulador/dispositivo y navega entre las 4 pantallas; rotar la pantalla no rompe nada. |
| **3. Motor de lecciones y ejercicios** | Cargar contenido JSON; pantalla de lección; 4 tipos de ejercicio validados; pistas progresivas; retroalimentación; guardar intentos | Pruebas unitarias de cada validador (respuestas correctas, incorrectas y casos límite); el progreso persiste tras cerrar la app; JSON inválido muestra un error controlado, no un cierre. |
| **4. Contenido de los módulos 1–3 + evaluación** | Lecciones de introducción, primer programa y variables (variantes por edad); evaluaciones; desbloqueo por dominio | El módulo 2 permanece bloqueado hasta aprobar la evaluación 1 con ≥ 80 % (probado); revisión técnica del contenido; las variantes de edad cubren los mismos objetivos. |
| **5. Sistema RPG básico** | XP, niveles, perfil con avatar, logros, mapa de progreso visual | Pruebas: repetir un ejercicio no da XP; las pistas reducen el XP; el nivel se calcula correctamente; los logros se otorgan una sola vez. |
| **6. Misiones, retos y proyectos** | Retos por módulo y proyecto integrador por etapa | Cada reto está ligado a objetivos de aprendizaje y completarlo exige respuestas validadas. |
| **7. Personalización y accesibilidad** | Ajustes por edad, tamaño de texto, contraste, animaciones; diseño para tabletas | Verificación con TalkBack y escalado de fuente al 200 %; capturas en teléfono y tableta. |
| **8. Prueba técnica de ejecución de Java** | Prototipo de la opción C y/o B | Informe con mediciones reales; decisión documentada; nada se publica como "ejecución" sin pruebas de aislamiento. |
| **9+. Contenido de los módulos 4–15** | En bloques de 2–3 módulos | Mismos criterios que la Fase 4. |
| **Posterior** | Otros lenguajes, sincronización en la nube, publicación en Google Play | Se planificarán por separado. |

---

## 12. Riesgos y dependencias

| Riesgo | Probabilidad / impacto | Mitigación |
|---|---|---|
| No se puede compilar Android en este entorno | Seguro / alto | CI en GitHub Actions + Android Studio local; o permitir `dl.google.com`. |
| Maven Central limitado (429) desde este entorno | Media / medio | Volver a comprobarlo; caché de Gradle; CI como fuente fiable de compilación. |
| Volumen de contenido (15 módulos × lecciones × variantes por edad) | Alta / alto | Formato JSON con guía de autoría; producir por bloques; validar los JSON con pruebas automáticas. |
| Expectativa de "ejecutar código real" | Alta / medio | Ser transparentes en la UI; prototipo en la Fase 8. |
| Seguridad de la ejecución de código | Media / alto | Nada de ejecución sin aislamiento demostrado. |
| Cumplimiento normativo con menores | Media / alto | Sin datos personales, sin anuncios ni analítica; revisar la Families Policy antes de publicar. |
| Gamificación que distraiga del aprendizaje | Media / medio | XP ligado a resultados validados; reglas probadas con pruebas unitarias. |
| Cambios en el contenido que rompan el progreso guardado | Media / medio | IDs estables, versionado y tabla de equivalencias. |

Dependencias externas previstas (todas gratuitas y de código abierto): AndroidX (Compose, Navigation, Room, DataStore, Lifecycle), kotlinx.serialization, kotlinx.coroutines, JUnit. Ninguna de pago.

---

## 13. Decisiones pendientes (necesito tu respuesta)

| # | Decisión | Mi recomendación |
|---|---|---|
| **D1** | Lenguaje de la app: **Kotlin + Compose** o **Java + Vistas XML** | Kotlin + Compose. Si tu prioridad es practicar Java tú mismo mientras construyes la app, Java + XML es viable, aunque con más código y menos soporte moderno. |
| **D2** | Estrategia de ejecución de Java | Opción A en el MVP; prototipo de C/B en la Fase 8. |
| **D3** | Públicos del MVP | Empezar con **adolescentes/adultos** (una variante) y añadir la variante infantil en la Fase 7, por el esfuerzo de contenido y el cumplimiento normativo. |
| **D4** | Idioma de la interfaz y del contenido | Español primero, con los textos de la UI en `strings.xml` para poder traducir. |
| **D5** | Dónde compilar y probar | GitHub Actions + tu Android Studio (y, si lo deseas, permitir `dl.google.com` en el entorno). |
| **D6** | `minSdk` | 26 (Android 8.0). |
| **D7** | ¿Quién redacta y revisa el contenido educativo? | Yo redacto borradores en JSON y tú los revisas antes de integrarlos. |
| **D8** | Nombre del paquete | `com.funcode.academy` (cámbialo si tienes un dominio propio). |

---

## 14. Prueba técnica mínima

No se ha construido ninguna prueba técnica en esta fase. La única que valdría la pena aquí (compilar un módulo de dominio JVM con un validador y pruebas JUnit) depende de que se resuelva D1 y de que Maven Central responda; además, la parte de mayor riesgo (Android y la ejecución de Java) no puede probarse en este contenedor. Se propone hacerla al inicio de la Fase 2.

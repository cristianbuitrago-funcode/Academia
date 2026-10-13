# Funcode Academy — Preparación de Android Studio y GitHub Actions

Requisitos y pasos que el responsable del proyecto debe hacer **antes o durante la Fase 2**. Todo es gratuito. La sección 4 explica cómo abrir y probar la Fase 2.

---

## 1. Android Studio (en tu equipo)

### Requisitos
- Windows 10/11 de 64 bits, macOS 12 o posterior, o Linux de 64 bits.
- **16 GB de RAM** recomendados (8 GB como mínimo, con el emulador muy lento) y unos 20 GB de disco libres.
- Virtualización por hardware activada (Intel VT-x / AMD-V en la BIOS/UEFI) para el emulador. En Windows, la *Windows Hypervisor Platform* activada.

### Pasos
1. Descarga la **última versión estable** de Android Studio desde https://developer.android.com/studio (no las versiones *Canary* ni *Beta*). Incluye su propio JDK; no necesitas instalar Java aparte.
2. En el asistente inicial, elige la instalación **Standard**: instala el Android SDK, las *platform-tools* y un emulador.
3. Abre **Settings → Languages & Frameworks → Android SDK**:
   - En *SDK Platforms*, instala la plataforma que indique `compileSdk` en la Fase 2. Android Studio la ofrecerá automáticamente al sincronizar si falta.
   - En *SDK Tools*, comprueba que estén *Android SDK Build-Tools*, *Platform-Tools* y *Android Emulator*.
4. Crea dos dispositivos virtuales en **Device Manager**:
   - Uno con una versión reciente de Android (para el uso normal).
   - Uno con **API 26 (Android 8.0)** para comprobar la versión mínima.
5. (Opcional) Dispositivo físico: activa *Opciones de desarrollador* (toca 7 veces "Número de compilación") y la *Depuración por USB*.
6. Clona el repositorio: ver la sección 4.
7. Espera a que termine **Gradle Sync**. Si aparece un error, copia el mensaje completo para analizarlo; no aceptes actualizaciones automáticas de versiones (AGP, Kotlin) sin consultarlo (D6).
8. Ejecuta la app con el botón ▶ sobre el emulador y anota el resultado de los criterios 17–18 de la Fase 2.

---

## 2. GitHub Actions (en GitHub)

El repositorio es **público**, así que los minutos de los *runners* estándar de GitHub son gratuitos. Los *runners* `ubuntu-latest` ya incluyen el Android SDK; el *workflow* instala JDK 21.

### Pasos
1. **Activar Actions:** en el repositorio, **Settings → Actions → General → Actions permissions** → "Allow all actions and reusable workflows" (o, más restrictivo, permitir solo las acciones de GitHub y de creadores verificados: el *workflow* usará `actions/checkout`, `actions/setup-java`, `gradle/actions/setup-gradle` y `actions/upload-artifact`).
2. En la misma página, **Workflow permissions** → "Read repository contents permission" es suficiente (el CI no necesita escribir en el repositorio).
3. **Rama principal (pendiente P1):** si apruebas crear `main`, después ve a **Settings → General → Default branch** y elígela como rama por defecto.
4. **Protección de rama (recomendado tras el primer CI en verde):** **Settings → Branches → Add branch ruleset** para `main` → exigir *pull request* y que pase el *status check* del CI antes de fusionar.
5. **Permiso para subir *workflows* (P4):** si al subir `.github/workflows/ci.yml` GitHub rechaza el *push* por falta de permiso *workflows*, te entregaré el contenido y lo crearás en **Add file → Create new file** desde la web.
6. **Ver resultados:** pestaña **Actions** → ejecución → registros de cada paso; el APK de depuración aparecerá en *Artifacts* al final de la ejecución.

### Qué **no** hace falta
- Secretos, claves de firma ni cuentas de pago: el CI compilará solo versiones de depuración.
- Servicios externos de compilación.

---

## 3. Qué se puede comprobar desde cada lugar

| Comprobación | Contenedor de Claude | GitHub Actions | Android Studio |
|---|---|---|---|
| Pruebas de `:core:domain` | Sí, en modo solo dominio (si Maven Central responde) | Sí | Sí |
| Compilar el APK | **No** (sin Android SDK) | Sí | Sí |
| Pruebas JVM de `:app` (incl. Robolectric si se aprueba) | No | Sí | Sí |
| Ejecutar la app y revisar la interfaz | No | No (salvo emulador en CI, P3) | **Sí** |
| Accesibilidad con TalkBack | No | No | Sí (en dispositivo o emulador) |

---

## 4. Abrir y probar la Fase 2 en Android Studio

### Requisitos adicionales
- Android Studio estable que admita **Android Gradle Plugin 9.4** (cualquier versión estable publicada desde septiembre de 2026). Si tu versión es anterior, Android Studio avisará al sincronizar: actualízalo en lugar de bajar la versión del plugin.
- **JDK 17 o superior** para Gradle (el JDK que trae Android Studio sirve). Las pruebas con Robolectric necesitan **JDK 21**: el de Android Studio (JetBrains Runtime 21) es suficiente.
- Plataforma Android **API 37** (Android Studio la descargará al sincronizar si falta).

### Pasos
1. *File → New → Project from Version Control* → `https://github.com/cristianbuitrago-funcode/Academia.git`.
2. Cambia a la rama de la fase: menú *Git → Branches* → `origin/ccr-db27c4de-czvzgb` → *Checkout* (hasta que se fusione en `main`).
3. Espera a que termine **Gradle Sync**. Si aparece un error, copia el mensaje completo.
4. Comprueba el JDK de Gradle: *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK* = el JDK 21 de Android Studio (*jbr-21*).
5. **Pruebas automáticas:** en la terminal de Android Studio ejecuta `./gradlew :core:domain:test :app:testDebugUnitTest` (en Windows, `gradlew.bat`). Debe terminar en `BUILD SUCCESSFUL`.
6. **Ejecutar la app:** elige un emulador en la barra superior y pulsa ▶ (*Run 'app'*).

### Comprobaciones manuales (criterios 17–18 y parte de 5 y 9)

| # | Qué hacer | Resultado esperado |
|---|---|---|
| 1 | Abrir la app en el emulador con Android reciente | Se ve "Te damos la bienvenida a Funcode Academy" y "XP total: 0". |
| 2 | Tocar Mapa, Perfil, Ajustes e Inicio | Cada pantalla muestra su título; Mapa y Perfil dicen "Disponible en una próxima versión". |
| 3 | Desde Perfil, pulsar Atrás | Vuelve a Inicio; otro Atrás cierra la app. |
| 4 | En Ajustes, elegir "Persona adulta" | Queda marcada. |
| 5 | Girar el emulador (botón de rotación) | Sigue en la misma pantalla, con la opción marcada. |
| 6 | Cerrar la app (deslizarla en Recientes) y volver a abrirla → Ajustes | "Persona adulta" sigue marcada. |
| 7 | Activar el modo oscuro del sistema | La app cambia a colores oscuros legibles. |
| 8 | Ajustes del sistema → tamaño de fuente al máximo | Los textos crecen y nada queda cortado sin poder desplazarse. |
| 9 | (Opcional) Activar TalkBack y recorrer la barra inferior | Lee "Inicio", "Mapa", "Perfil", "Ajustes" y las opciones como botones de opción. |
| 10 | Repetir 1–6 en el emulador con **API 26** | Igual que arriba, sin cierres. |
| 11 | *app/src/main/AndroidManifest.xml* → pestaña *Merged Manifest* | No aparece `android.permission.INTERNET`. |

Anota cualquier fallo con el número de la comprobación, el dispositivo y, si es posible, una captura.

### Instalar el APK sin compilar
En la ejecución de CI (pestaña *Actions* → ejecución en verde → *Artifacts*) descarga `funcode-academy-debug-apk`, descomprímelo y arrastra el `.apk` al emulador. Es un APK de **depuración**: no sirve para publicar.

---

## 5. Probar la Fase 3 (motor de lecciones) en Android Studio

1. Actualiza la rama: **Git → Pull** (rama `ccr-db27c4de-czvzgb`).
2. Pruebas automáticas: `./gradlew :core:domain:test :app:testDebugUnitTest` (en Windows, `.\gradlew.bat …`).
3. Ejecuta la app (▶) y comprueba:

| # | Qué hacer | Resultado esperado |
|---|---|---|
| 1 | Inicio | Aparece «Lecciones disponibles» con «Tu primer programa: mostrar texto en pantalla», «Ejercicios: 7» y el aviso de borrador. |
| 2 | **Abrir lección** | Título, aviso de borrador, objetivo, explicación, ejemplo de código con números de línea y comentarios por línea. |
| 3 | **Empezar ejercicios** → «Comprobar respuesta» sin elegir nada | «Elige una respuesta antes de comprobar.» |
| 4 | Ejercicio 1: **Ver una pista**, elegir *Falso* y comprobar | Se muestra la pista; después «Todavía no es correcto.» y los botones «Intentar de nuevo» / «Continuar sin resolver». |
| 5 | *Intentar de nuevo* → *Verdadero* → comprobar | «¡Correcto! Respuesta comprobada.» con la explicación. |
| 6 | Resolver los ejercicios 2–7 (selección, relacionar con desplegables, huecos, ordenar con flechas, salida, línea con error) | Cada tipo responde y da retroalimentación. Ninguna pantalla dice que el código se haya ejecutado. |
| 7 | Tras el último, **Ver resumen** | Totales (al primer intento, tras varios intentos, sin resolver, pistas) y el estado de cada ejercicio. |
| 8 | Girar la pantalla en mitad de un ejercicio | Se conservan el ejercicio y la respuesta escrita. |
| 9 | TalkBack (opcional) en un ejercicio de ordenar | Los botones de flecha se leen como «Subir la línea N» / «Bajar la línea N». |

Anota cualquier fallo con su número.

---

## 6. Probar la Fase 4 (módulo 1 y evaluación) en Android Studio

1. Actualiza la rama: **Git → Pull** (rama `ccr-db27c4de-czvzgb`).
2. Pruebas automáticas: `./gradlew :core:domain:test :app:testDebugUnitTest` (en Windows, `.\gradlew.bat …`). `JavaContentCompilationTest` compila los ejemplos con el `javac` del JDK de Gradle (JDK 17 o superior).
3. Si tenías instalada la versión de la Fase 3, puedes conservarla: no hay cambio de base de datos. Los intentos de la lección antigua `java.m02.l01` no cuentan para el módulo nuevo.
4. Ejecuta la app (▶) y comprueba:

| # | Qué hacer | Resultado esperado |
|---|---|---|
| 1 | Inicio | Tarjeta «Continuar aprendiendo»: título del módulo 1, «Lecciones completadas: 0 de 7», «Siguiente lección: ¿Qué es programar?», botones «Continuar» y «Ver el módulo». |
| 2 | Pestaña **Mapa** | «Mapa de aprendizaje» con el módulo 1, aviso de borrador, «Evaluación: aún no realizada» y «Abrir módulo». |
| 3 | **Abrir módulo** | Lecciones numeradas del 1 al 7, nota sobre el orden recomendado, «Ejercicios resueltos: 0 de N» y la tarjeta «Evaluación del módulo». |
| 4 | Abrir la lección 1 y resolver todos sus ejercicios | Al volver al módulo, la lección 1 dice «Completada»; en Inicio, «Siguiente lección» pasa a la 2. |
| 5 | Cerrar la app (deslizarla en Recientes) y volver a abrirla | El progreso del paso 4 se conserva. |
| 6 | **Hacer la evaluación** | La introducción dice «Una sola respuesta por pregunta y sin pistas.»; botón «Empezar la evaluación». |
| 7 | Responder mal la pregunta 1 y comprobar | Se muestra «Todavía no es correcto.» con la explicación; **no** aparece «Intentar de nuevo» ni «Ver una pista», y la respuesta ya no se puede cambiar; solo «Siguiente pregunta». |
| 8 | Pulsar «Siguiente pregunta» sin responder varias preguntas hasta el final → **Ver resultado** | «Resultado: X de 10 respuestas correctas (Y %)»; las preguntas sin responder cuentan como fallos. **No** aparece «aprobado» (el umbral P6 no está aprobado). |
| 9 | Volver al módulo | «Mejor resultado de la evaluación: Y %» y «Repetir la evaluación». |
| 10 | Repetir la evaluación con peor resultado | El mejor resultado no baja. Con uno mejor, sube. |
| 11 | Cerrar y reabrir la app | El mejor resultado se conserva. |
| 12 | Girar la pantalla en mitad de la evaluación | Se conservan la pregunta y la respuesta. |
| 13 | Repetir 1–3 y 6–8 en el emulador con **API 26** | Igual, sin cierres. |

Ninguna pantalla debe decir que se ejecuta código Java: solo se comprueban las respuestas. Anota cualquier fallo con su número.


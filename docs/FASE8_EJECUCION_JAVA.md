# Fase 8 — Ejecución real de Java: estudio de viabilidad y propuesta

_Estado: **prototipo de verificación implementado y probado en emuladores de CI** (aprobado como prototipo con las condiciones F8-D1 a F8-D8). **No está integrado en las lecciones ni declarado seguro.** Ver §9 (resultados) y §10 (decisiones pendientes)._
_Fecha: 2026-10-15._

Cada afirmación lleva su nivel de evidencia:

- **[Medido]:** comprobado en esta sesión, con el comando o la prueba indicados.
- **[Doc]:** según la documentación citada.
- **[Supuesto]:** razonado, pero sin verificar. Lo comprobaría el prototipo.

---

## 1. Inspección inicial (estado real del repositorio)

| Punto | Resultado |
|---|---|
| Interfaz `CodeRunner` | **No existe en el código.** Solo está diseñada en `PLAN_TECNICO_FASE1.md` §8.2. El plan preveía añadirla como contrato en la Fase 3, pero no se hizo. Habría que crearla. |
| ¿Qué valida respuestas? | `AnswerValidator` (dominio), con 7 tipos de ejercicio de respuesta predefinida. Ninguno acepta código libre. |
| ¿Qué compila Java? | Solo `JavaContentCompilationTest`, en la JVM de las **pruebas**, con `javac --release 17`. Comprueba que el contenido es correcto. **La app no compila nada.** |
| ¿Qué ejecuta programas? | **Nada en la app.** La misma prueba ejecuta los programas del contenido en la JVM de CI para comparar la salida de los ejercicios «predecir la salida». |
| Persistencia de intentos | `ExerciseAttempt(exerciseId, correct, hintsUsed)`. No guarda respuestas ni código. |
| Protección contra XP repetido | El XP sale del primer acierto (`firstCorrectAttempts`), y el registro `xp_ledger` tiene una clave única por fuente. Un ejercicio que ejecuta código y registra el intento como los demás no puede dar XP dos veces. |
| Permisos | El manifiesto no declara ninguno, tampoco `INTERNET`. |
| Pruebas en Android real | Desde el 2026-10-15 hay un job de CI con emulador (API 26 y 34). Es imprescindible para esta fase. |

### Defectos y condiciones previas (informar antes de continuar)

1. **Rama sin fusionar.** La corrección del cierre al abrir y la fase visual están solo en la rama de trabajo, no en `main`. Aún no las has confirmado en tu teléfono ni las has aprobado. La Fase 8 se construiría encima.
2. **La Fase 7 no está aprobada formalmente**, aunque se fusionó con el *pull request* #1.
3. **P7 (versión de Java) sigue pendiente.** El ejecutor necesita un nivel de lenguaje. Propuesta: Java 17, igual que las pruebas del contenido.
4. **Falta `CodeRunner`** (ver la tabla).

Ninguno de estos puntos impide investigar. Los puntos 1 y 3 deberían resolverse antes de implementar.

### Limitaciones reales del entorno

- **Android no trae JVM ni `javac`.** ART ejecuta código DEX, no archivos `.class` [Doc: arquitectura de Android].
- **Contenedor de Claude:**
  - No tiene Android SDK.
  - `dl.google.com` está bloqueado, así que no puedo descargar R8/D8 aquí. [Medido: `curl` devuelve 000.]
  - Maven Central sí está accesible [Medido].
- **CI (GitHub Actions):** tiene SDK, acceso a Google Maven y emuladores x86_64.
- **Sin medir todavía:** un teléfono de gama baja real. Los emuladores de CI no representan el rendimiento real.

---

## 2. Alternativas evaluadas

### A. Compilador ECJ + D8 en el dispositivo, ejecutado en un proceso aislado (`isolatedProcess`)

Flujo:

1. El código del estudiante se compila con ECJ (Eclipse Compiler for Java) contra una **biblioteca reducida de clases permitidas** (*stubs* propios).
2. D8 convierte el resultado a DEX.
3. El DEX se carga con `InMemoryDexClassLoader` dentro de un **servicio en proceso aislado**.
4. La salida sale por un flujo con tamaño máximo.

| Criterio | Evaluación |
|---|---|
| Viabilidad técnica | **Probable, pero sin demostrar en Android.**<br>[Medido] ECJ 3.46.100 pesa 3 384 534 bytes; sus clases son bytecode Java 17 (versión 61), que D8 convierte durante la compilación de la app.<br>[Medido] ECJ hace referencia a paquetes que Android no tiene (`javax.tools`, `javax.lang.model`, `javax.annotation.processing`). Se usarían solo las clases del compilador por lotes (`org.eclipse.jdt.internal.compiler.Compiler` con un `INameEnvironment` propio). Que no se carguen esas clases en ART es un [Supuesto] que debe comprobar el prototipo.<br>[Supuesto] D8 puede ejecutarse en el dispositivo; hay IDE para Android de código abierto que lo hacen, pero no lo he verificado. |
| Arquitectura actual | Encaja con el contrato `CodeRunner` del plan (§8.2), detrás del dominio. La UI solo ve `RunResult`. |
| Seguridad (código no confiable) | Defensa en capas (ver §4):<br>1. Compilación contra una lista de clases permitidas. [Medido en la JVM de escritorio: `java.io.File`, `Runtime`, `Class` y `getClass()` dan error de compilación.]<br>2. Cargador de clases que solo deja resolver nombres permitidos. [Supuesto]<br>3. Proceso aislado, sin permisos y con otro UID [Doc: `<service android:isolatedProcess>`; GeckoView, aislamiento de procesos].<br>4. Límites de tiempo y salida, y proceso desechable tras cada ejecución.<br>**No es un *sandbox* demostrado** hasta superar las pruebas de §6 en el emulador. |
| Sin Internet | Sí: todo va dentro del APK. [Supuesto razonable: no hay ninguna llamada de red y la app no tiene el permiso `INTERNET`.] |
| Recursos | [Sin medir] Tiempo de compilación en frío en el teléfono (cargar ECJ + D8). Memoria: límite de *heap* por proceso de ART; el aislado no afecta al de la interfaz. |
| Tamaño de la app | ECJ: +3,4 MB sin comprimir [Medido]. D8/R8: [Sin medir; es un JAR grande, del orden de decenas de MB sin reducir]. Hay que medirlo en CI. Puede bajar activando R8 en la versión *release*, que hoy está desactivado. |
| Mantenimiento | Medio-alto: hay que mantener las versiones de ECJ y D8 y la biblioteca de clases permitidas. |
| Licencias | ECJ: **EPL-2.0** [Medido: POM en Maven Central]. Exige aviso y acceso al código fuente de ECJ; no obliga a abrir el código de la app.<br>R8/D8: BSD-3-Clause según un README de terceros (Chromium); falta confirmarlo en la fuente oficial al fijar la versión. Incluye dependencias Apache-2.0 y BSD que se listarán. |
| Costes | Ninguno obligatorio. |
| Límites educativos | Java real (lenguaje 17), pero solo con la API permitida: `System.out`, `String`, `StringBuilder`, `Math`, tipos envoltorio; colecciones más adelante. Sin hilos, archivos, red ni reflexión. Hay que explicarlo al estudiante. |
| Riesgo de políticas | Google Play desaconseja cargar código dinámicamente, sobre todo si es remoto [Doc: developer.android.com, «Dynamic code loading»]. Aquí el código lo genera localmente el propio estudiante. **No he podido leer el texto exacto de la política** (`support.google.com` no resuelve desde el contenedor). Queda pendiente antes de publicar. |

### B. Intérprete educativo propio de un subconjunto de Java

ECJ se usa solo para analizar y mostrar los errores de compilación. Un intérprete propio, que recorre el árbol del programa, lo ejecuta.

| Criterio | Evaluación |
|---|---|
| Viabilidad | Alta para un subconjunto pequeño (variables, aritmética, `if`, bucles, métodos estáticos, `String`). |
| Seguridad | Muy controlable: no se carga código, cada paso se cuenta (límite de tiempo exacto) y la memoria se contabiliza. Sigue siendo recomendable el proceso aislado. |
| Sin Internet / tamaño | Sí, sin conexión. Solo añade ECJ (+3,4 MB), sin D8. |
| Fidelidad | **Riesgo principal.** Hay que reproducir exactamente la semántica de Java: desbordamiento de `int`, división entera, formato de `double`, orden de evaluación… Un error enseñaría un Java incorrecto. Se mitigaría con pruebas diferenciales contra la JVM real en CI, que ya existe para el contenido. |
| Honestidad | La interfaz diría «ejecutado por el intérprete de Funcode (subconjunto de Java)», no «JVM». |
| Mantenimiento | Alto: cada construcción nueva de Java hay que implementarla. |

### C. Servicio externo de compilación aislado (Judge0, Piston u otro, autoalojado)

Solo como alternativa futura y separada.

- **Requisitos:** necesita el permiso `INTERNET`, un servidor y su coste, y enviar el código fuera del dispositivo (privacidad, en especial de menores).
- **Sin Internet no funciona,** lo que rompe D1/D4 (*offline* y privacidad).
- **Ventaja:** es la mejor opción para cubrir todo Java y la más sencilla de aislar con contenedores en el servidor.
- **No se propone para el MVP.** Requeriría una decisión explícita sobre costes y privacidad.

### D. JDK completo en el dispositivo (binarios nativos de OpenJDK, estilo Termux)

**Descartada.**

- Ocupa más de 100 MB por arquitectura [Supuesto, por orden de magnitud].
- Licencia GPLv2 con *Classpath Exception*.
- Android no permite ejecutar binarios desde carpetas escribibles en apps con `targetSdk ≥ 29`; el truco de empaquetarlos como `lib*.so` es frágil.
- El mantenimiento es muy alto.

### E. JVM en un WebView (DoppioJVM, CheerpJ)

**Descartada.** DoppioJVM está sin mantenimiento. CheerpJ tiene licencia comercial y su entorno se descarga desde Internet [Supuesto, por su documentación pública; no verificado en esta sesión].

---

## 3. Recomendación para el MVP

**Alternativa A**, con un **prototipo de verificación previo** con criterios de «sigue / no sigue».

**Plan B:** si el prototipo demuestra que ECJ o D8 no funcionan de forma fiable en API 26–34, o que el tamaño o el tiempo son inaceptables, se pasa a la **alternativa B** con el mismo contrato `CodeRunner`. La interfaz no cambiaría.

### Criterios del prototipo (medidos en el emulador de CI con API 26 y 34)

1. ECJ compila «Hola» y aritmética en el dispositivo y devuelve errores con línea y mensaje.
2. D8 genera el DEX en el dispositivo y el programa se ejecuta con salida real.
3. Las pruebas de seguridad de §6 pasan dentro del proceso aislado.
4. Medir y registrar: aumento del APK, tiempo en frío y en caliente, y memoria. Umbrales propuestos, que necesitan tu aprobación:
   - APK *release* +25 MB como máximo;
   - ejecución completa en menos de 5 s en el emulador.

### Qué soportaría realmente el MVP

- Una clase con `main`, en el paquete por defecto, como único archivo.
- Tipos primitivos, `String`, operadores, `if`/`switch`, bucles, métodos estáticos, arrays.
- `System.out.print`/`println`.
- Entrada por `Scanner` con texto predefinido del ejercicio: solo en una segunda iteración y si se aprueba.

### Restricciones del código permitido

- Fuente de 10 000 caracteres como máximo.
- Salida de 10 000 caracteres como máximo.
- Ejecución de 3 s como máximo (configurable) y compilación de 10 s como máximo.
- Solo la API de la lista permitida.
- Sin `package` ni más de una clase pública.

### Componentes que se reutilizan

- `LessonParser` y `Exercise`: con un tipo de ejercicio nuevo.
- `LessonSession` y `recordAttempt`, `RewardEngine` (XP del primer acierto, sin duplicados).
- `CodeBlock` y el tema.
- La prueba diferencial con `javac` del contenido.
- El job de emulador del CI.

### Archivos y módulos que se modificarían (requiere aprobación)

| Cambio | Dónde |
|---|---|
| Contrato `CodeRunner`, `RunRequest`, `RunResult` (con `RuntimeError`, `TimedOut`, `OutputLimitExceeded`, `Unavailable`) | `core/domain/.../runner/` (nuevo paquete; Kotlin puro) |
| Tipo de ejercicio `WRITE_PROGRAM`: plantilla inicial, salida esperada (lista de aceptadas, normalizando espacios finales) y nota de qué se comprueba | `domain/lesson/Exercise.kt`, `LessonParser` y su JSON, `AnswerValidator` (la comparación de salida). El contenido actual no cambia. |
| Compilación (ECJ + biblioteca permitida) y conversión a DEX (D8): Java/Kotlin puro, probado en la JVM de CI | **Módulo nuevo `:core:javarunner`** |
| Biblioteca de clases permitidas: código propio, compilado al construir y empaquetado en `assets` | `core/javarunner/stubs/` |
| Servicio aislado (`isolatedProcess="true"`, `exported="false"`) y cliente `AndroidCodeRunner` | `app/.../runner/`, `AndroidManifest.xml` (**sin permisos nuevos**) |
| Editor (monoespaciado, números de línea), botones Compilar y ejecutar / Cancelar, estados y panel de salida o errores | `app/.../ui/screens/lesson/` |
| Dependencias nuevas: `org.eclipse.jdt:ecj` y `com.android.tools:r8`, con versión fija | `gradle/libs.versions.toml` + `docs/LICENCIAS_TERCEROS.md` |
| Room | **Sin cambios.** El borrador de código vive solo en memoria (`SavedStateHandle`) y no se guarda en disco. |

### Riesgos que permanecen (aunque el prototipo salga bien)

- Fallos del propio ECJ o D8 con entradas maliciosas: se ejecutan dentro del proceso aislado.
- Diferencias entre fabricantes en el límite de memoria por proceso.
- Política de Google Play (§2.A).
- Tamaño del APK.
- La biblioteca permitida debe revisarse cada vez que se amplíe: un método que devuelva `Class` o `Thread` abriría puertas.
- Un *sandbox* hecho con mecanismos de Android no es una garantía formal. Se documentará qué se ha demostrado.

---

## 4. Diseño de seguridad propuesto (a demostrar)

| Riesgo | Medida | Cómo se demuestra |
|---|---|---|
| Acceso a archivos, red, datos personales, reflexión, procesos, hilos | (1) Solo se compila contra la API permitida.<br>(2) Cargador de clases padre que rechaza cualquier otro nombre en tiempo de ejecución.<br>(3) Proceso aislado: otro UID, sin permisos y sin acceso a la base de datos ni a DataStore. | Prueba instrumentada: (a) código que usa `java.io.File`, `Socket`, `Runtime`, `Class.forName` o `Thread` → error de compilación. (b) DEX fabricado a mano que hace referencia a `java.io.File` → rechazado al ejecutarse. (c) Desde el proceso aislado no se puede leer `funcode.db`. |
| Bucle infinito / CPU | Temporizador en el proceso aislado (hilo vigía que termina el proceso) y otro en la app, que cancela, desvincula el servicio y marca «tiempo agotado». | Prueba: `while(true){}` → `TimedOut` en ≤ límite + margen, y la interfaz sigue respondiendo. |
| Memoria | Proceso aparte: si se queda sin memoria (`OutOfMemoryError`) o muere, la app lo detecta (`DeadObjectException` / `binderDied`) → «límite de memoria o fallo de ejecución». | Prueba: reservar memoria sin fin → resultado controlado y la app sigue viva. |
| Salida enorme | Flujo de salida con límite de tamaño. | Prueba: imprimir sin fin → `OutputLimitExceeded`. |
| Fuente vacía o enorme | Validación antes de compilar. | Pruebas unitarias. |
| Estado entre ejecuciones | Un proceso nuevo por ejecución: tras cada resultado se cierra el proceso. | Prueba: una variable estática no se conserva entre ejecuciones. |
| Bloqueo de la interfaz | Todo se hace con Binder fuera del hilo principal; el estado se muestra con *flows*. | Prueba de UI + el job de emulador. |
| Recuperación | Si el proceso muere, se crea uno nuevo en la siguiente ejecución. | Prueba: ejecución que falla y otra correcta después. |

**Filtrar palabras del código fuente no se usa como medida de seguridad.** La restricción se aplica en la compilación, en la carga de clases y en el sistema operativo.

---

## 5. Evaluación y progreso

- Pulsar «Compilar y ejecutar» **no da XP**.
- Solo cuenta como acierto si la ejecución termina con normalidad **y** la salida coincide con una de las aceptadas. Entonces se registra un intento correcto.
- El XP lo concede el motor actual, una sola vez.
- Errores de compilación o de ejecución y tiempo agotado → intento no correcto o sin registrar. **Propuesta:** solo se registra el intento cuando el programa termina. Así no se penaliza a quien está corrigiendo errores de compilación; decisión pendiente.
- La interfaz dice, según el caso:
  - «Compilado y ejecutado en tu dispositivo»;
  - «No compila: …»;
  - «Error durante la ejecución: …»;
  - «Tiempo agotado (3 s)».
- Nunca dice «ejecutado» si no hubo ejecución.
- Los ejercicios de respuesta predefinida siguen diciendo «Respuesta comprobada».
- El código del estudiante **no se guarda** ni sale del dispositivo.

---

## 6. Pruebas previstas

| Tipo | Dónde |
|---|---|
| Programa válido, error de sintaxis, error de ejecución (`ArithmeticException`), salida esperada, código vacío o enorme, salida enorme, API prohibida | JVM de CI (`:core:javarunner`) y emulador |
| Bucle infinito, memoria agotada, cancelación, recuperación tras un fallo, aislamiento (archivos y base de datos), DEX que se salta la lista permitida, interfaz sin bloquearse | Emulador de CI (instrumentadas) |
| `WRITE_PROGRAM` en el parser, el validador y la sesión; sin XP duplicado al repetir | Dominio + Robolectric |
| Medidas de tamaño y tiempo | Job de emulador (registradas en `ESTADO_PROYECTO.md`) |

---

## 7. Decisiones que necesito de ti

| # | Decisión | Propuesta |
|---|---|---|
| F8-D1 | Alternativa para el MVP | **A** (ECJ + D8 en un proceso aislado), con prototipo previo y **B** como plan alternativo |
| F8-D2 | Nuevas dependencias y licencias: ECJ (EPL-2.0), R8/D8 (BSD-3, por confirmar) | Aceptar, con `docs/LICENCIAS_TERCEROS.md` |
| F8-D3 | Módulo nuevo `:core:javarunner` y servicio aislado en el manifiesto (sin permisos nuevos) | Aceptar |
| F8-D4 | Umbrales del prototipo: APK *release* +25 MB, 5 s por ejecución en el emulador, 3 s de límite de ejecución | Aceptar o ajustar |
| F8-D5 | P7: nivel de lenguaje Java 17 | Aceptar |
| F8-D6 | Registro de intentos: solo cuando el programa termina | Aceptar |
| F8-D7 | Entrada estándar (`Scanner`) | Más adelante, tras validar el prototipo |
| F8-D8 | Alternativa C (servicio externo) | Solo documentada; no se implementa |

---

## 8. Prototipo: qué se implementó

Solo como prototipo de verificación. El ejecutor **no** aparece en las lecciones. Un «Laboratorio de Java» solo existe en compilaciones de depuración: Ajustes → «Laboratorio de Java (prototipo)».

| Pieza | Archivo | Qué hace |
|---|---|---|
| Contrato | `core/domain/.../runner/CodeRunner.kt` | `CodeRunner`, `RunRequest`, `RunLimits` (3 s, 10 000 caracteres de código y de salida), `RunResult` (Rechazado, Error de compilación, Completado, Error de ejecución, Límite TIME/OUTPUT/MEMORY, Cancelado, No disponible). |
| Evaluación | `core/domain/.../runner/ProgramEvaluation.kt` | Compara la salida real con las aceptadas (misma normalización que «predecir la salida»). Registra un intento **solo si la ejecución terminó**. Errores de compilación, límites y cancelación no registran intento (F8-D6). |
| Compilador | `core/javarunner/.../JavaSourceCompiler.kt` | ECJ 3.28.0, nivel Java 17, contra la biblioteca permitida. Rechaza `package` y exige la clase `Main`. Concatena con `StringBuilder` (sin `invokedynamic`). |
| Biblioteca permitida | `core/javarunner/src/stubs/java/**` (37 clases) | `Object`, `String`, `StringBuilder`, `Math`, tipos envoltorio, `System.out` (solo `out`), `PrintStream`, excepciones comunes. **Sin** `Class`, reflexión, hilos, archivos, red, procesos, `System.exit`/`getenv`/`getProperty`/`err`/`in`, colecciones ni `Scanner`. |
| Ejecución | `core/javarunner/.../ProgramExecutor.kt` | Hilo propio con límite de tiempo; salida con límite (por caracteres, saltos de línea normalizados); `OutOfMemoryError` → límite de memoria; línea del error del programa. `AllowlistClassLoader`: en ejecución solo se cargan las clases permitidas y los errores que lanza la VM. |
| DEX | `app/.../runner/D8Dexer.kt` | D8 9.4.28 en memoria (minApi 26, modo depuración para conservar líneas). |
| Proceso aislado | `app/.../runner/JavaRunnerService.kt` + manifiesto | `isolatedProcess="true"`, `process=":javarunner"`, `exported="false"`, **sin permisos nuevos**. Un proceso por ejecución: responde y se termina (`killProcess`). Vigilante de tiempo propio. |
| Cliente | `app/.../runner/IsolatedJavaRunner.kt` | Vincula el servicio, envía el código, espera con límite, detecta la muerte del proceso (memoria o fallo), cancela (mensaje + fin del proceso) y reintenta si el proceso anterior aún no ha terminado. |
| Laboratorio (depuración) | `app/.../ui/screens/lab/` | Editor monoespaciado, «Compilar y ejecutar» / «Detener», estados honestos («No compila: el programa no se ha ejecutado», «Tiempo agotado», etc.), salida real, comparación con la salida esperada. **No registra intentos ni da XP.** El código solo vive en memoria. |

## 9. Resultados (2026-10-15)

### 9.1 Compilación real

- `:core:javarunner` compila en el contenedor y en CI (Linux y Windows).
- La app (con ECJ y R8) compila en CI. Lint: 0 errores; se desactivó `InvalidPackage` con justificación, porque ECJ referencia `javax.tools`, pero esas clases no se cargan (comprobado en el emulador).

### 9.2 Ejecución real en emuladores de CI

**Dispositivos:**
- Android 14 (API 34): `sdk_gphone64_x86_64`, 2 núcleos, 2,5 GB, `heapgrowthlimit` 192 MB.
- Android 8.0 (API 26): «Android SDK built for x86_64».

Compilación de **depuración** (sin optimizar). Los emuladores de CI comparten máquina y sus tiempos varían.

| Prueba (emulador API 34, CI 38686325724 y anteriores) | Resultado |
|---|---|
| Programa válido «Hola, Funcode / 7 + 5 = 12», 3 veces | **Salida real correcta** y coincide con la esperada |
| Aritmética, `double`, cadenas, `Math`, `Integer.parseInt` | Salida real `2 / 2.5 / JAVA4 / 14` correcta |
| Error de sintaxis | `CompilationFailed`, línea 3 |
| Error de ejecución (división entre cero) | `RuntimeError` `ArithmeticException`, línea 4 |
| API peligrosas (`File`, `Runtime.exec`, `Class.forName`, `Thread`, `Socket`, `System.exit`) | Las 6 dan error de compilación |
| Bucle infinito (límite 3 s) | `LimitExceeded(TIME)` en 4,1–5,5 s desde la app (incluye compilar); latencia del hilo principal 0–1 ms; ejecución correcta inmediatamente después |
| Memoria agotada | `LimitExceeded(MEMORY)`; ejecución correcta después (recuperación) |
| Salida infinita | `LimitExceeded(OUTPUT)` con exactamente 10 000 caracteres |
| Código vacío o de más de 10 000 caracteres | `Rejected`, sin abrir proceso |
| Estado estático entre ejecuciones | No se conserva (`1` y `1`) |
| Cancelar un bucle infinito | Detenido en 16–24 ms; la siguiente ejecución funciona |
| Biblioteca permitida frente a las clases reales de Android | Todos los miembros existen (API 26 y API 34) |

### 9.3 Restricciones de seguridad verificadas (y cómo)

Cada capa se probó **por separado** en API 34. Las pruebas de las capas 2 y 3 usan una opción que **solo existe en compilaciones de depuración**: el servicio la ignora si la app no es *debuggable*.

| Capa | Prueba | Resultado observado |
|---|---|---|
| 1. Compilación contra la lista permitida | API peligrosas | Error de compilación (JVM y emulador) |
| 2. Cargador con lista permitida | Bytecode que usa `java.io.File` (compilado con una declaración falsa para esquivar la capa 1) | `NoClassDefFoundError: Failed resolution of: Ljava/io/File;`; el archivo no se toca |
| 3. Proceso aislado (capas 1 y 2 **desactivadas** a propósito) | Leer un archivo privado de la app y su base de datos | `ENOENT`: los archivos de la app no son visibles desde el proceso aislado |
| | Abrir una conexión de red | `SocketException: EACCES (Permission denied)` |
| | Ejecutar un proceso (`exec("id")`) | `IOException: error=13, Permission denied` |
| Recuperación | Bucle infinito, memoria agotada, cancelación | La app sigue respondiendo y la siguiente ejecución funciona |

**Lo que NO está demostrado (no se declara un sandbox seguro):**
- No se ha probado en teléfonos reales ni con fabricantes que modifiquen Android.
- No hay auditoría de seguridad externa ni pruebas de fallos de ECJ/D8 con entradas maliciosas (*fuzzing*). Esos fallos ocurrirían dentro del proceso aislado.
- Las capas 2 y 3 dependen de ART y del sistema. La lista permitida debe revisarse cada vez que se amplíe.
- El consumo de CPU de un programa malicioso solo se limita por tiempo: el proceso se termina a los 3 s más el margen.

### 9.4 Medidas frente a los umbrales de F8-D4

| Umbral | Medida | ¿Se cumple? |
|---|---|---|
| APK *release* +25 MB como máximo | Base `d867bd5`: 9 892 282 bytes → con el prototipo: 18 754 706 bytes. **+8,45 MB** (sin firmar, sin *minify*) | **Sí** |
| Menos de 5 s por ejecución normal en el emulador | Desde la app: la mayoría entre 0,9 y 2,6 s. En el proceso: 0,5–2,6 s (compilar 0,3–1,8 s, DEX 0,2–1,0 s, ejecutar 0–45 ms). **Excepción:** la primera ejecución en frío de una ejecución de CI tardó **5,9 s** | **No siempre**: superado una vez (primera ejecución en frío, emulador cargado) |
| Límite de 3 s del programa | Aplicado: bucle infinito detenido; respuesta total 4,1–5,5 s (compilar + 3 s + margen) | Sí |
| 10 000 caracteres de código y de salida | Aplicado y probado | Sí |

### 9.5 Fallos encontrados durante el prototipo (y estado)

1. **ECJ 3.46.100 no funciona en Android** (`Runtime.version()`, `NoClassDefFoundError`). Se cambió a 3.28.0. Un análisis de referencias muestra la llamada desde ECJ 3.40.
2. **ECJ 3.28.0 no funciona en Android 8–12 (API 26–32)**: usa `InputStream.readAllBytes()` (API 33) al cargar sus tablas.
   - La biblioteca oficial de *desugaring* no incluye ese método (tabla oficial «nio»: solo `transferTo`).
   - Ninguna versión de ECJ con Java 17 lo evita: 3.25–3.26 no lo usan, pero no soportan Java 17.
   - **Sin resolver; requiere tu decisión (§10).** En API 26 una prueba registra que el ejecutor responde «no disponible» sin cerrar la app.
3. ART resolvía `OutOfMemoryError`/`NoClassDefFoundError` a través del cargador del programa y la lista permitida los bloqueaba. **Corregido:** se permiten en ejecución los errores que lanza la VM (no se pueden usar al compilar).
4. En Windows, `println` escribe `\r\n`. **Corregido:** la salida se normaliza a `\n`.
5. El motor de pruebas de AGP informa `assumeTrue` como fallo. **Corregido** en el recuento del CI: se listan como omitidas.

### 9.6 Política de Google Play

- La política «Device and Network Abuse» prohíbe **descargar** código ejecutable de fuentes distintas de Google Play. Hay una excepción para código que se ejecuta en una máquina virtual o un intérprete con acceso **indirecto** a las API de Android.
- **No pude abrir la página oficial**: `support.google.com` y `play.google.com` no resuelven desde el contenedor. El texto se conoce por resultados de búsqueda.
- Aquí el código lo genera el estudiante en el propio teléfono; no se descarga. Aun así, cargar DEX generado en ejecución es un caso límite.
- **Riesgo PENDIENTE**: debe verificarse en la página oficial antes de publicar.

## 10. Decisiones pendientes tras el prototipo

| # | Decisión | Opciones |
|---|---|---|
| F8-P1 | **Android 8–12 (API 26–32)**: el compilador no arranca | (a) Ejecutor solo en Android 13+; en versiones anteriores, «no disponible» de forma honesta. (b) Modificar el binario de ECJ al compilar la app (sustituir `readAllBytes` por una función propia; la EPL lo permite publicando la modificación). (c) Bajar a ECJ 3.26 con **nivel Java 16** (cambia F8-D5). (d) Plan alternativo B (intérprete propio). |
| F8-P2 | Licencias de R8 (LGPL-2.1, CDDL/GPL-CE declaradas en su `LICENSE`) | Revisión antes de cualquier distribución |
| F8-P3 | Tiempo de la primera ejecución (5,9 s una vez) | Aceptar, o medir en un teléfono real / compilación *release* antes de decidir |
| F8-P4 | Política de Google Play | Verificar en la página oficial |

## Fuentes consultadas

- `<service>` en el manifiesto de Android (`android:isolatedProcess`): https://developer.android.com/guide/topics/manifest/service-element
- Riesgos de la carga dinámica de código (Android): https://developer.android.com/privacy-and-security/risks/dynamic-code-loading
- Procesos aislados (documentación de GeckoView, Mozilla): https://firefox-source-docs.mozilla.org/mobile/android/geckoview/project/isolated-process/overview.html
- Política SELinux `isolated_app` (AOSP): https://android.googlesource.com/platform/system/sepolicy/+/cd109d4/isolated_app.te
- Restricción de solo lectura para código cargado dinámicamente en Android 14: https://blog.codia.ai/?p=51
- ECJ en Maven Central (POM con la licencia EPL-2.0): https://repo1.maven.org/maven2/org/eclipse/jdt/ecj/3.46.100/
- R8 (código fuente y README): https://r8.googlesource.com/r8/+/refs/heads/8.0 · licencia en un README de terceros: https://cnb.cool/aosp/monorepo/-/tree/main/external/angle/third_party/r8
- Tabla oficial de API disponibles con *desugaring* (especificación nio): https://developer.android.com/studio/write/java11-nio-support-table
- Política «Device and Network Abuse» (Google Play), conocida por resultados de búsqueda; no se pudo abrir: https://support.google.com/googleplay/android-developer/answer/9888379

## Anexo: experimento realizado [Medido]

Hecho en la carpeta temporal de la sesión, sin cambiar el repositorio. JDK 21 de escritorio, no Android.

1. Se creó una biblioteca de 22 clases permitidas (`Object` sin `getClass`, `String`, `System.out`, `PrintStream`, `Math`, `Integer`, `StringBuilder`, excepciones básicas…).
2. Se compiló con `ecj-3.46.100.jar -8 -bootclasspath empty`.
   - Error encontrado: falta `java.lang.annotation.Annotation`. Hay que añadirlo a la biblioteca.
   - Además, `-bootclasspath` no se admite con nivel de lenguaje ≥ 9, así que el nivel 17 necesitará la API programática de ECJ (`INameEnvironment`).
3. `Main.java` (`"Hola " + 7*6`) compiló contra esa biblioteca, y el `.class` generado se ejecutó en la JVM real con la salida `Hola 42`.
4. Un programa que usa `java.io.File`, `Runtime.getRuntime().exec`, `Class.forName` y `getClass()` dio **4 errores de compilación**: los cuatro usos quedaron bloqueados.

**Esto no demuestra nada sobre ART ni sobre el proceso aislado.** Es solo la base del prototipo.

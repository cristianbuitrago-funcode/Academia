# Guía de autoría de lecciones (formato JSON, versión 1)

El contenido vive en `app/src/main/assets/content/<idioma>/<lenguaje>/`:

```
content/es/java/
├── index.json                  {"schemaVersion": 1, "modules": ["modules/java.m01.json"], "lessons": ["lessons/java.m01.l01.json", …]}
├── modules/java.m01.json       un módulo por archivo
├── missions/java.m01.mi01.json una misión por archivo
└── lessons/java.m01.l01.json   una lección (o evaluación) por archivo
```

Una lección o un módulo que no cumpla las reglas **no se muestra**: la app enseña un aviso en lugar de cerrarse. Las pruebas `ModuleContentTest` y `JavaContentCompilationTest` (en `:core:domain`) fallan con la lista completa de problemas. Para comprobar el contenido basta con ejecutar `./gradlew :core:domain:test`.

## Campos del módulo

| Campo | Obligatorio | Reglas |
|---|---|---|
| `schemaVersion` | Sí | `1` |
| `id` | Sí | Estable (p. ej. `java.m01`). |
| `order` | Sí | Entero ≥ 1; orden en el mapa. |
| `reviewStatus`, `title`, `description` | Sí | Como en la lección. |
| `lessons` | Sí | Ids de las lecciones **en el orden recomendado**. Deben existir, tener `moduleId` igual al id del módulo y `kind` = `lesson`. |
| `assessment` | No | Id de la evaluación del módulo (`kind` = `assessment`). |
| `challenge` | No | Id del reto guiado del módulo (`kind` = `challenge`). Se abre al completar todas las lecciones y da 50 XP al resolver todos sus pasos. |

## Campos de la lección

| Campo | Obligatorio | Reglas |
|---|---|---|
| `schemaVersion` | Sí | `1` |
| `id`, `moduleId` | Sí | Identificador estable: minúsculas, números, `.` y `-` (p. ej. `java.m02.l01`). **No cambiarlo nunca**: el progreso se guarda con él. |
| `languageId`, `locale` | Sí | `java`, `es` |
| `version` | Sí | Entero ≥ 1. Súbelo cada vez que cambie el contenido. |
| `reviewStatus` | Sí | `DRAFT` (siempre al crear) o `APPROVED` (solo el responsable). |
| `title`, `description` | Sí | Texto no vacío. |
| `objectives` | Sí | Lista de `{id, text}`. Cada objetivo debe practicarse en al menos un ejercicio. |
| `presentations` | Sí | Mapa de variante → secciones. `default` es obligatoria; `kids`, `teens` y `adults` son opcionales y solo cambian la presentación, nunca los objetivos ni los ejercicios. |
| `exercises` | Sí | Lista ordenada (de menor a mayor dificultad). |
| `kind` | No | `lesson` (por defecto), `assessment` (evaluación del módulo) o `challenge` (proyecto guiado: pasos con pistas y reintentos, sin bloque `assessment`). |
| `project` | Solo en `challenge` | `{situation, priorConcepts[], recap[]}`: el problema que se plantea, los conceptos previos y el resumen de lo aprendido (se muestra al resolver todos los pasos). |
| `assessment` | Solo en evaluaciones | `{exerciseIds, passThreshold?}`. `exerciseIds` debe incluir **todos** los ejercicios. `passThreshold` (fracción en (0, 1] de respuestas correctas al primer intento) es opcional; el valor aprobado (P6) es **0.8**. Sin él, la evaluación no se puede aprobar ni da XP. |

**Evaluaciones:** una sola respuesta por pregunta y sin pistas (el parser rechaza `hints` en una evaluación). Una pregunta sin responder cuenta como fallo.

Secciones: `{"type":"text","text":…}`, `{"type":"callout","text":…}` y `{"type":"code","code":…,"lineComments":[{"line":1,"comment":…}]}` (líneas numeradas desde 1).

## Campos de una misión

| Campo | Obligatorio | Reglas |
|---|---|---|
| `schemaVersion`, `id`, `moduleId`, `order`, `version`, `reviewStatus` | Sí | Como en la lección. `id` no debe cambiar: identifica también su bonificación de XP. |
| `type` | Sí | `learning`, `practice`, `assessment` o `project`. |
| `title`, `description`, `objective` | Sí | `objective` es el objetivo educativo. |
| `prerequisites` | No | Ids de otras misiones que hay que completar antes (sin ciclos). |
| `tasks` | Sí | Al menos una: `{"kind":"completeLesson","lessonId"}`, `{"kind":"solveExercises","exerciseIds":[…],"description"}` (ejercicios de lecciones o del proyecto, no de evaluaciones), `{"kind":"passAssessment","assessmentId"}` o `{"kind":"completeChallenge","challengeId"}`. |

Completar una misión da 10 XP de bonificación una sola vez. Todo lo que referencia debe existir en el mismo módulo; si no, la misión se descarta (y las que dependen de ella).

## Campos comunes de cada ejercicio

`id` (estable), `type`, `objectiveIds`, `difficulty` (1–5; decide el XP: 10 + 5 × (dificultad − 1)), `prompt`, `hints` (opcionales, progresivas), `explanation` (por qué la respuesta es correcta) e `incorrectFeedback` (orientación general si falla).

**Pistas:** deben ayudar a razonar. El validador rechaza una pista que contenga literalmente la salida esperada o una respuesta de un hueco.

## Tipos de ejercicio

| `type` | Campos propios | Cómo se comprueba |
|---|---|---|
| `multipleChoice` | `options` [`{text, feedback?}`], `correctIndices` | Conjunto exacto de opciones. Con varias correctas, hay que marcarlas todas. `feedback` se muestra si se elige esa opción. |
| `trueFalse` | `statement`, `correct` | Igualdad. |
| `fillInBlanks` | `template` con `{{0}}`, `{{1}}`…; `blanks` (respuestas aceptadas por hueco) | Por *tokens* de Java: se ignoran los espacios entre símbolos (`x+1` = `x + 1`), pero no dentro de textos entre comillas. Distingue mayúsculas. |
| `orderLines` | `lines` (en el orden correcto), `alternativeOrders` (opcional) | Se compara el texto resultante, así dos líneas idénticas (p. ej. `}`) son intercambiables. |
| `predictOutput` | `code`, `acceptedOutputs` | Se normalizan los saltos de línea y los espacios al final de cada línea; mayúsculas y espacios internos cuentan. **El código no se ejecuta.** |
| `findError` | `code`, `errorLines` (desde 1) | Conjunto exacto de líneas. |
| `matchPairs` | `left`, `right`, `pairs` [[iLeft, iRight], …] | Correspondencia uno a uno completa. Desordena `right` al escribirlo. |

## Antes de enviar un bloque a revisión (D7)

1. Todo el código Java debe compilar (o fallar exactamente donde dice `findError`) y producir la salida declarada. `JavaContentCompilationTest` lo comprueba automáticamente con `javac --release 17` (versión provisional, P7) durante las pruebas del dominio, en el contenedor y en CI. Esto ocurre en la JVM de las pruebas, **no en la app**. Solo se compila el código que contiene `class X`; el pseudocódigo (p. ej. `mostrar "Hola"`) se ignora.
2. `reviewStatus` = `DRAFT`.
3. `./gradlew :core:domain:test` en verde.

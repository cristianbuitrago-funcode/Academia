# Guía de autoría de lecciones (formato JSON, versión 1)

El contenido vive en `app/src/main/assets/content/<idioma>/<lenguaje>/`:

```
content/es/java/
├── index.json                    {"schemaVersion": 1, "lessons": ["lessons/java.m02.l01.json"]}
└── lessons/java.m02.l01.json     una lección por archivo
```

Una lección que no cumpla las reglas **no se muestra**: la app enseña un aviso en lugar de cerrarse, y la prueba `DemoLessonContentTest` (en `:core:domain`) falla con la lista completa de problemas. Para comprobar un archivo basta con ejecutar `./gradlew :core:domain:test`.

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
| `assessment` | No | `{exerciseIds, passThreshold}`: fracción (0–1] de esos ejercicios que hay que resolver al primer intento. |

Secciones: `{"type":"text","text":…}`, `{"type":"callout","text":…}` y `{"type":"code","code":…,"lineComments":[{"line":1,"comment":…}]}` (líneas numeradas desde 1).

## Campos comunes de cada ejercicio

`id` (estable), `type`, `objectiveIds`, `difficulty` (1–5), `prompt`, `hints` (opcionales, progresivas), `explanation` (por qué la respuesta es correcta) e `incorrectFeedback` (orientación general si falla).

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

1. Todo el código Java debe compilar (o fallar exactamente donde dice `findError`) y producir la salida declarada. Mientras la comprobación automática (P8) no exista, verifícalo con `javac`/`java` y anótalo en `docs/ESTADO_PROYECTO.md`.
2. `reviewStatus` = `DRAFT`.
3. `./gradlew :core:domain:test` en verde.

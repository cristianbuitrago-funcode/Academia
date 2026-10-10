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
| 1. Planificación y arquitectura | **Aprobado** (decisiones D1–D8, 2026-10-10) | Quedan las cuestiones técnicas P1–P10 (§14 del plan), que se resolverán en su fase. |
| 2. Esqueleto y CI | Planificado — **sin autorizar** | Criterios de aceptación en §13.1 del plan. |
| 3–9 | Planificado — sin autorizar | — |

## Decisiones

| Decisión | Estado |
|---|---|
| D1 Kotlin + Compose + Material 3; módulos `:app` y `:core:domain` | Aprobado |
| D2 Ejercicios comprobados sin ejecución; interfaz `CodeRunner` solo diseñada; Fase 8 para la ejecución real | Aprobado |
| D3 MVP para adolescentes y adultos; modelo con variantes `kids`/`teens`/`adults` | Aprobado |
| D4 Español; `strings.xml` y contenido por idioma | Aprobado |
| D5 GitHub Actions + Android Studio, sin servicios de pago | Aprobado |
| D6 `minSdk 26`; versiones fijadas en el catálogo de Gradle en la Fase 2 | Aprobado |
| D7 Contenido como borrador hasta la revisión del responsable | Aprobado |
| D8 `com.funcode.academy` | Aprobado |
| P1–P10 (ver §14 del plan) | **Pendiente** |

## Componentes

| Componente | Planificado | Implementado | Probado | Aprobado |
|---|---|---|---|---|
| Arquitectura y estructura de carpetas | ✔ | — | — | ✔ (plan) |
| Modelo de contenido con variantes por público | ✔ | — | — | ✔ (plan) |
| Esquema de Room con `UNIQUE` en `xp_ledger` | ✔ | — | — | ✔ (plan) |
| 7 tipos de ejercicio y sus reglas de validación | ✔ | — | — | ✔ (plan) |
| Interfaz `CodeRunner` | ✔ (solo diseño) | — | — | ✔ (plan) |
| CI en GitHub Actions | ✔ | — | — | — |
| Contenido educativo | — | — | — | — |

**No existe código de la aplicación.** Nada está implementado ni probado todavía.

## Contenido educativo

| Bloque | Resumen presentado | Borrador | Revisado y aprobado |
|---|---|---|---|
| (ninguno) | — | — | — |

## Pruebas ejecutadas

| Fecha | Prueba | Resultado |
|---|---|---|
| 2026-10-10 | Inspección del entorno | JDK 21 y Gradle 8.14.3 disponibles; Android SDK ausente; `dl.google.com` bloqueado (403); Maven Central responde 429; `maven.google.com` y `plugins.gradle.org` accesibles. |
| 2026-10-10 | Visibilidad del repositorio (consulta a GitHub) | Público; rama por defecto `ccr-db27c4de-czvzgb`. |

No se ha compilado ni ejecutado ninguna aplicación.

## Errores conocidos

Ninguno (no hay código).

## Limitaciones conocidas

- El contenedor en la nube no puede compilar Android (sin SDK y con `dl.google.com` bloqueado). Aceptado en D5: la compilación se verificará en GitHub Actions y Android Studio.
- Maven Central respondió 429 desde el contenedor; si persiste, tampoco se podrán ejecutar aquí las pruebas de `:core:domain` y la evidencia vendrá de CI.

## Acciones pendientes del responsable

- Responder P1 (rama `main`) y P3 (Robolectric o emulador) antes de la Fase 2.
- Seguir `docs/CONFIGURACION_ENTORNO.md` (Android Studio y activación de GitHub Actions).

## Próximos pasos autorizados

Ninguno. La Fase 2 espera autorización explícita.

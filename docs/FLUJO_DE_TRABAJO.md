# Funcode Academy: flujo de trabajo con ramas y pull requests

## Ramas

| Rama | Para qué sirve | Quién la modifica |
|---|---|---|
| `main` | Rama **estable**. Solo contiene trabajo revisado y aprobado. | Nadie sube cambios directamente: entran por *pull request*. |
| `ccr-…` / `fase-N-…` | Ramas de desarrollo, una por fase o tarea. | Claude (o tú) sube los cambios aquí. |

Estado actual (2026-10-10):
- `main` se creó a partir de la rama `ccr-db27c4de-czvzgb`, con la documentación y el historial de la Fase 1 (commit `c8c2fe6`).
- En GitHub ya existía una rama llamada **`Main`** (con mayúscula) que apunta al mismo commit. **Dos ramas que solo se diferencian en las mayúsculas dan problemas en Windows y macOS**, cuyos sistemas de archivos no distinguen mayúsculas. Te recomiendo borrar `Main` (pasos más abajo).
- La rama por defecto del repositorio todavía es `ccr-db27c4de-czvzgb`. Cambiarla requiere permisos de administración del repositorio, que solo tienes tú.

## Ciclo de una fase

1. Se crea o reutiliza una rama de desarrollo a partir de `main`.
2. Se suben los cambios; GitHub Actions ejecuta las pruebas en cada *push*.
3. Cuando la fase está lista y el CI está en verde, se abre un **pull request** hacia `main` con el resumen de la fase.
4. Tú revisas los cambios (pestaña *Files changed*) y la verificación manual en Android Studio.
5. Si apruebas, fusionas el *pull request* (**Merge**). Si no, comentas y se corrige en la misma rama.
6. Solo entonces se autoriza la fase siguiente.

## Pasos que debes hacer en GitHub (una sola vez)

1. **Borrar la rama `Main` duplicada:** pestaña *Code* → selector de ramas → *View all branches* → icono de papelera junto a `Main`. Antes, comprueba que apunta al commit `c8c2fe6` (el mismo que `main`), para no perder nada.
2. **Rama por defecto:** *Settings → General → Default branch* → icono de flechas → `main` → *Update*.
3. **Proteger `main`** (cuando el CI esté en verde): *Settings → Rules → Rulesets → New branch ruleset*:
   - *Target branches*: `main`.
   - Activa *Restrict deletions*, *Block force pushes* y *Require a pull request before merging*.
   - Activa *Require status checks to pass* y añade la comprobación **"Pruebas y compilación"** (nombre del trabajo del CI).
4. La rama `ccr-db27c4de-czvzgb` **no se borra** hasta que su contenido esté fusionado en `main`.

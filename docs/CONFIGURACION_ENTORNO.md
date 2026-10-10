# Funcode Academy — Preparación de Android Studio y GitHub Actions

Requisitos y pasos que el responsable del proyecto debe hacer **antes o durante la Fase 2**. Todo es gratuito.

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
6. Clona el repositorio cuando exista el proyecto de la Fase 2: **File → New → Project from Version Control** → `https://github.com/cristianbuitrago-funcode/Academia.git`, y selecciona la rama que se indique.
7. Espera a que termine **Gradle Sync**. Si aparece un error, copia el mensaje completo para analizarlo; no aceptes actualizaciones automáticas de versiones (AGP, Kotlin) sin consultarlo (D6).
8. Ejecuta la app con el botón ▶ sobre el emulador y anota el resultado de los criterios 17–18 de la Fase 2.

---

## 2. GitHub Actions (en GitHub)

El repositorio es **público**, así que los minutos de los *runners* estándar de GitHub son gratuitos. Los *runners* `ubuntu-latest` ya incluyen el Android SDK y JDK, así que no hay que instalar nada.

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
| Pruebas de `:core:domain` | Sí (si Maven Central responde) | Sí | Sí |
| Compilar el APK | **No** (sin Android SDK) | Sí | Sí |
| Pruebas JVM de `:app` (incl. Robolectric si se aprueba) | No | Sí | Sí |
| Ejecutar la app y revisar la interfaz | No | No (salvo emulador en CI, P3) | **Sí** |
| Accesibilidad con TalkBack | No | No | Sí (en dispositivo o emulador) |

# Licencias de terceros: prototipo de la Fase 8

_Estado: **en evaluación.** Estas dependencias solo existen para el prototipo de verificación. Antes de integrarlas definitivamente o publicar la app, las licencias marcadas como «pendiente» deben revisarse._

Las versiones, licencias y sumas de comprobación se obtienen con el workflow reproducible `.github/workflows/inspeccion-dependencias.yml`. Se lanza a mano (*workflow_dispatch*) o al cambiar ese archivo.

## ECJ (Eclipse Compiler for Java)

| Dato | Valor | Evidencia |
|---|---|---|
| Coordenadas | `org.eclipse.jdt:ecj:3.28.0` (Eclipse 2021-12) | `gradle/libs.versions.toml` |
| Licencia | **EPL-2.0** | POM en Maven Central y `about.html` dentro del JAR [verificado] |
| Tamaño | 3 180 419 bytes | descarga desde Maven Central [verificado] |
| SHA-1 | `414f81a653aab5654b6ed55578f3d3a416532ba3` | calculado sobre la descarga [verificado] |
| Código de bytes | Java 11 (versión de clase 55) | análisis del JAR [verificado] |

**Obligaciones de la EPL-2.0 al distribuir el código objeto:**
- conservar los avisos;
- indicar dónde se obtiene el código fuente (https://www.eclipse.org; el JAR incluye `about.html`, que la app conserva);
- si se modifica ECJ, poner disponibles las modificaciones.

La EPL-2.0 **no** obliga a abrir el código de la app. **Pendiente:** mostrar los avisos en la app (pantalla «Licencias») si se integra.

**Versión descartada:** ECJ 3.46.100 (la más reciente) **no funciona en Android**. Su analizador usa `java.lang.Runtime.version()`, que no existe en Android; comprobado en el emulador API 34.

## R8 / D8

| Dato | Valor | Evidencia |
|---|---|---|
| Coordenadas | `com.android.tools:r8:9.4.28` (última 9.4 estable; misma serie que AGP 9.4) | metadatos de Google Maven [verificado en CI] |
| Tamaño | 20 597 762 bytes | CI [verificado] |
| SHA-256 | `51af0a9d41cc541d619064224aec628923bd779cbcc5a62cb914ea2d669c4063` | CI [verificado] |
| Licencia del código de R8 | **BSD-3-Clause** («All non-library code has the following 3-Clause BSD license», archivo `LICENSE` del JAR) | [verificado] |
| Bibliotecas que declara incluidas | Apache-2.0 (Guava, Gson, fastutil, Kotlin stdlib, Apache Commons…), BSD (protobuf, kxml2), MIT (Bouncy Castle), EDL-1.0 (jakarta.xml.bind…), **LGPL-2.1 (trove4j)** y **CDDL-1.1 / GPL-2.0 con excepción de *classpath* (jaxb-runtime, txw2, javax.activation)** | archivo `LICENSE` del JAR [verificado] |

**PENDIENTE (riesgo de licencia, sin resolver):**
- El `LICENSE` de R8 declara componentes LGPL-2.1 y CDDL/GPL-CE.
- Las 7 622 clases del JAR están reubicadas bajo `com/android/tools`. Por el nombre **no se puede saber** si el código de esos componentes está dentro: la búsqueda de `trove`, `jaxb`, `javax/activation`, `com/sun/istack` y `org/bouncycastle` da 0 entradas.
- Antes de distribuir la app con R8 dentro, hace falta una revisión de licencias, idealmente legal, o una confirmación del proyecto R8.

## Biblioteca de clases permitidas

`core/javarunner/src/stubs/java/**` es **código propio** (solo declaraciones), no copiado de OpenJDK ni de Android. Se publica con la licencia del proyecto.

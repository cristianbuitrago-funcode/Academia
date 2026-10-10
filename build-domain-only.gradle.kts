// Archivo raíz alternativo para el modo "solo dominio" (-Pfuncode.domainOnly=true),
// usado en máquinas sin acceso al Android SDK ni al repositorio de Google.
// No declara los plugins de Android, así que no necesita descargarlos.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

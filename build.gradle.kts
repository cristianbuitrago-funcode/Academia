// Archivo de compilación raíz. Declara (sin aplicar) los plugins de Kotlin para
// que todos los módulos usen la misma versión. Los plugins de Android, KSP y Room
// se declaran solo en :app, para poder compilar :core:domain sin Android SDK.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

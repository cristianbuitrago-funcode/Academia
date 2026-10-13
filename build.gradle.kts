// Archivo de compilación raíz. Declara (sin aplicar) todos los plugins para que
// se carguen una sola vez y todos los módulos usen la misma versión.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

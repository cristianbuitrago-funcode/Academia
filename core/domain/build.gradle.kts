import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Módulo de dominio: Kotlin puro, sin dependencias de Android.
// Contiene los modelos y las reglas, y se prueba con JUnit en cualquier JDK.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // Flow: permite que la UI observe cambios (XP, ajustes) de forma reactiva.
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

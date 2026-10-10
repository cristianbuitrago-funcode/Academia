pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Todos los repositorios se declaran aquí; los módulos no pueden añadir otros.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FuncodeAcademy"

include(":core:domain")

// En máquinas sin Android SDK se puede compilar y probar solo el dominio:
//   ./gradlew -Pfuncode.domainOnly=true :core:domain:test
// Por defecto (Android Studio y CI) se incluye también la app.
if (providers.gradleProperty("funcode.domainOnly").orNull == "true") {
    rootProject.buildFileName = "build-domain-only.gradle.kts"
} else {
    include(":app")
}

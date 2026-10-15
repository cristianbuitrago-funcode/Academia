import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Fase 8 (PROTOTIPO de verificación): compilación de programas de estudiantes con ECJ
// contra una biblioteca de clases PERMITIDAS, y ejecución con límites.
// Kotlin/Java puro: se prueba en la JVM. En Android se usa dentro de un proceso aislado.
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

val stubCompiler: Configuration by configurations.creating

dependencies {
    api(project(":core:domain"))
    // Eclipse Compiler for Java (EPL-2.0). Ver docs/LICENCIAS_TERCEROS.md.
    implementation(libs.eclipse.ecj)
    stubCompiler(libs.eclipse.ecj)

    testImplementation(libs.junit)
}

// --- Biblioteca de clases permitidas -----------------------------------------------
// src/stubs/java contiene solo declaraciones (código propio). Se compilan con ECJ sin
// biblioteca del sistema y se empaquetan como recursos `funcode-stubs/**.stub` más un
// índice. La extensión .stub evita que las herramientas de Android las traten como código.
val stubSources = layout.projectDirectory.dir("src/stubs/java")
val stubClasses = layout.buildDirectory.dir("stubs/classes")
val emptyBootClasspath = layout.buildDirectory.dir("stubs/empty-bootclasspath")
val stubResources = layout.buildDirectory.dir("generated/stub-resources")

val compileStubs by tasks.registering(JavaExec::class) {
    description = "Compila la biblioteca de clases permitidas con ECJ."
    classpath = stubCompiler
    mainClass.set("org.eclipse.jdt.internal.compiler.batch.Main")
    inputs.dir(stubSources).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(stubClasses)
    doFirst {
        delete(stubClasses)
        emptyBootClasspath.get().asFile.mkdirs()
    }
    argumentProviders.add(
        CommandLineArgumentProvider {
            listOf(
                "-8", "-nowarn",
                "-bootclasspath", emptyBootClasspath.get().asFile.absolutePath,
                "-d", stubClasses.get().asFile.absolutePath,
                stubSources.asFile.absolutePath,
            )
        },
    )
}

val packageStubs by tasks.registering {
    description = "Empaqueta la biblioteca de clases permitidas como recursos."
    dependsOn(compileStubs)
    inputs.dir(stubClasses)
    outputs.dir(stubResources)
    doLast {
        val src = stubClasses.get().asFile
        val out = stubResources.get().asFile.resolve("funcode-stubs")
        out.deleteRecursively()
        out.mkdirs()
        val names = src.walkTopDown().filter { it.isFile && it.extension == "class" }.map { f ->
            val name = f.relativeTo(src).invariantSeparatorsPath.removeSuffix(".class")
            f.copyTo(out.resolve("$name.stub"))
            name
        }.sorted().toList()
        out.resolve("index.txt").writeText(names.joinToString("\n", postfix = "\n"))
    }
}

sourceSets["main"].resources.srcDir(stubResources)
tasks.named("processResources") { dependsOn(packageStubs) }
tasks.withType<Jar>().configureEach { dependsOn(packageStubs) }

tasks.withType<Test>().configureEach {
    // Las pruebas de límites crean hilos que no terminan (bucles infinitos): un proceso
    // de prueba nuevo por clase evita que afecten a las demás.
    forkEvery = 1
    testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

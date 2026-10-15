package com.funcode.academy.javarunner

/**
 * Biblioteca de clases permitidas (declaraciones compiladas en `src/stubs/java`).
 *
 * - El compilador solo «ve» estas clases: cualquier otra (archivos, red, hilos,
 *   reflexión, procesos…) da error de compilación.
 * - En ejecución, [AllowlistClassLoader] solo deja resolver estos nombres.
 */
class StubLibrary private constructor(
    /** Nombre interno (`java/lang/String`) → bytes del archivo de clase. */
    private val classes: Map<String, ByteArray>,
) {
    /** Nombres internos de todas las clases permitidas, p. ej. `java/lang/String`. */
    val internalNames: Set<String> get() = classes.keys

    /** Nombres binarios (`java.lang.String`) que se pueden cargar en ejecución. */
    val allowedClassNames: Set<String> = classes.keys.mapTo(HashSet()) { it.replace('/', '.') }

    /** Paquetes con al menos una clase permitida (`java`, `java/lang`, …). */
    val packages: Set<String> = buildSet {
        classes.keys.forEach { name ->
            var pkg = name.substringBeforeLast('/', "")
            while (pkg.isNotEmpty()) {
                add(pkg)
                pkg = pkg.substringBeforeLast('/', "")
            }
        }
    }

    fun classBytes(internalName: String): ByteArray? = classes[internalName]

    companion object {
        private const val PREFIX = "funcode-stubs/"

        /** Lee la biblioteca empaquetada como recursos de este módulo. */
        fun load(loader: ClassLoader = StubLibrary::class.java.classLoader): StubLibrary {
            val index = loader.getResourceAsStream("${PREFIX}index.txt")?.use { it.readBytes().decodeToString() }
                ?: error("No se encontró la biblioteca de clases permitidas (${PREFIX}index.txt)")
            val classes = index.lines().filter { it.isNotBlank() }.associateWith { name ->
                loader.getResourceAsStream("$PREFIX$name.stub")?.use { it.readBytes() }
                    ?: error("Falta $PREFIX$name.stub")
            }
            return StubLibrary(classes)
        }
    }
}

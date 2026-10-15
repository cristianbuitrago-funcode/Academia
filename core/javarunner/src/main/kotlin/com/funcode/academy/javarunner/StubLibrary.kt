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

    /**
     * Nombres binarios (`java.lang.String`) que se pueden cargar en ejecución: la biblioteca
     * permitida más [VM_THROWABLES]. ART resuelve por nombre, a través del cargador del
     * programa, los errores que lanza la propia máquina virtual (comprobado en el emulador:
     * sin ellos, un OutOfMemoryError aparecía como ClassNotFoundException).
     */
    val allowedClassNames: Set<String> = classes.keys.mapTo(HashSet()) { it.replace('/', '.') } + VM_THROWABLES

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

        /**
         * Errores y excepciones que la máquina virtual puede lanzar por sí misma. Solo se
         * permiten en ejecución (no en la compilación): cargar estas clases no da acceso a nada.
         */
        val VM_THROWABLES: Set<String> = setOf(
            "java.lang.OutOfMemoryError", "java.lang.StackOverflowError", "java.lang.VirtualMachineError",
            "java.lang.InternalError", "java.lang.LinkageError", "java.lang.NoClassDefFoundError",
            "java.lang.ClassNotFoundException", "java.lang.ReflectiveOperationException",
            "java.lang.ExceptionInInitializerError", "java.lang.IncompatibleClassChangeError",
            "java.lang.NoSuchMethodError", "java.lang.NoSuchFieldError", "java.lang.AbstractMethodError",
            "java.lang.IllegalAccessError", "java.lang.VerifyError", "java.lang.ArrayStoreException",
            "java.lang.IllegalMonitorStateException",
        )

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

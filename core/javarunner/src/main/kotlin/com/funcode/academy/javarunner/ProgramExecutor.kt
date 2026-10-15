package com.funcode.academy.javarunner

import com.funcode.academy.domain.runner.Limit
import com.funcode.academy.domain.runner.RunResult
import com.funcode.academy.domain.runner.RunTimings
import java.io.OutputStream
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicReference

/**
 * Ejecuta `Main.main` de un programa ya cargado, con límite de tiempo y de salida.
 *
 * **No aísla nada por sí mismo.** En Android se usa dentro de un proceso aislado que se
 * termina tras cada ejecución (un hilo con un bucle infinito no se puede detener de forma
 * segura en Java). En la JVM solo se usa en pruebas.
 */
object ProgramExecutor {

    fun execute(loader: ClassLoader, studentClasses: Set<String>, timeoutMs: Long, maxOutputChars: Int): RunResult {
        val output = LimitedOutput(maxOutputChars)
        val stream = PrintStream(output, true, "UTF-8")
        val previous = System.out
        val started = System.nanoTime()
        fun elapsed() = (System.nanoTime() - started) / 1_000_000
        fun timings() = RunTimings(executeMs = elapsed())

        val main = try {
            Class.forName(JavaSourceCompiler.MAIN_CLASS, false, loader).getMethod("main", Array<String>::class.java)
                .takeIf { Modifier.isStatic(it.modifiers) && it.returnType == Void.TYPE }
        } catch (e: ReflectiveOperationException) {
            null
        } ?: return RunResult.Unavailable("Falta «public static void main(String[] args)» en la clase Main.")

        val failure = AtomicReference<Throwable?>(null)
        val thread = Thread(null, {
            try {
                main.invoke(null, arrayOf<String>())
            } catch (e: InvocationTargetException) {
                failure.set(e.targetException ?: e)
            } catch (t: Throwable) {
                failure.set(t)
            }
        }, "funcode-programa", STACK_BYTES)
        thread.isDaemon = true
        System.setOut(stream)
        try {
            thread.start()
            thread.join(timeoutMs)
        } finally {
            stream.flush()
            System.setOut(previous)
        }
        val text = output.text()
        // La salida excesiva se informa aunque el programa capture el error y siga en marcha.
        if (output.exceeded) return RunResult.LimitExceeded(Limit.OUTPUT, text, timings())
        if (thread.isAlive) return RunResult.LimitExceeded(Limit.TIME, text, timings())
        val error = failure.get() ?: return RunResult.Completed(text, timings())
        if (error is OutOfMemoryError) return RunResult.LimitExceeded(Limit.MEMORY, text, timings())
        val line = error.stackTrace.firstOrNull { it.className in studentClasses }?.lineNumber?.takeIf { it > 0 }
        return RunResult.RuntimeError(error.javaClass.simpleName, error.message, line, text, timings())
    }

    /** Pila amplia para que la recursión normal funcione; la infinita acaba en StackOverflowError. */
    private const val STACK_BYTES = 8L * 1024 * 1024

    /** Error interno al superar la salida máxima. El programa no puede nombrarlo (no está permitido). */
    class OutputLimitReached : Error("Límite de salida alcanzado")

    /**
     * Salida UTF-8 con límite de caracteres (cuenta puntos de código). Al superarlo deja de
     * guardar y lanza [OutputLimitReached] en el hilo del programa.
     */
    private class LimitedOutput(private val maxChars: Int) : OutputStream() {
        private val buffer = java.io.ByteArrayOutputStream()
        private var chars = 0

        @Volatile var exceeded = false
            private set

        @Synchronized
        override fun write(b: Int) {
            if (exceeded) throw OutputLimitReached()
            // Los bytes de continuación UTF-8 (10xxxxxx) no inician un carácter nuevo.
            if (b and 0xC0 != 0x80) {
                if (chars == maxChars) {
                    exceeded = true
                    throw OutputLimitReached()
                }
                chars++
            }
            buffer.write(b)
        }

        /** Saltos de línea normalizados a `\n`: `println` usa `\r\n` en Windows y `\n` en Android. */
        @Synchronized
        fun text(): String = buffer.toByteArray().decodeToString().replace("\r\n", "\n")
    }
}

/**
 * Cargador padre que solo deja resolver las clases de la biblioteca permitida. Cualquier
 * otro nombre (p. ej. `java.io.File`) da [ClassNotFoundException] aunque el código de
 * bytes lo mencione. Segunda capa de defensa tras la compilación.
 */
class AllowlistClassLoader(private val allowed: Set<String>) : ClassLoader(BOOT) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name !in allowed) throw ClassNotFoundException("Clase no permitida: $name")
        return Class.forName(name, false, BOOT)
    }

    private companion object {
        /** Cargador del sistema base: `null` en la JVM, `BootClassLoader` en Android. */
        val BOOT: ClassLoader? = Any::class.java.classLoader
    }
}

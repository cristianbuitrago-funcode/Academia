package com.funcode.academy.javarunner

import com.funcode.academy.domain.runner.Diagnostic
import com.funcode.academy.domain.runner.ProgramEvaluation
import com.funcode.academy.domain.runner.RunRequest
import com.funcode.academy.domain.runner.RunResult

/**
 * Ejecutor SOLO PARA PRUEBAS en la JVM: misma compilación, mismo cargador con lista
 * permitida y mismo [ProgramExecutor] que en Android, pero sin proceso aislado ni D8.
 */
object JvmTestRunner {
    val stubs: StubLibrary by lazy { StubLibrary.load() }
    private val compiler by lazy { JavaSourceCompiler(stubs) }

    fun run(source: String, timeoutMs: Long = 3_000, maxOutputChars: Int = 10_000): RunResult {
        val request = RunRequest(source)
        ProgramEvaluation.precheck(request)?.let { return it }
        return when (val compiled = compiler.compile(source)) {
            is JavaSourceCompiler.Outcome.Failure -> RunResult.CompilationFailed(compiled.diagnostics)
            is JavaSourceCompiler.Outcome.Success -> runClasses(compiled.classes, timeoutMs, maxOutputChars)
        }
    }

    fun compileErrors(source: String): List<Diagnostic> =
        (compiler.compile(source) as? JavaSourceCompiler.Outcome.Failure)?.diagnostics.orEmpty()

    /** Carga archivos de clase con el cargador de la lista permitida como padre. */
    fun runClasses(classes: Map<String, ByteArray>, timeoutMs: Long = 3_000, maxOutputChars: Int = 10_000): RunResult {
        val loader = object : ClassLoader(AllowlistClassLoader(stubs.allowedClassNames)) {
            override fun findClass(name: String): Class<*> {
                val bytes = classes[name] ?: throw ClassNotFoundException(name)
                return defineClass(name, bytes, 0, bytes.size)
            }
        }
        return ProgramExecutor.execute(loader, classes.keys, timeoutMs, maxOutputChars)
    }
}

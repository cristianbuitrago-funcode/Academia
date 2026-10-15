package com.funcode.academy.javarunner

import com.funcode.academy.domain.runner.Diagnostic
import org.eclipse.jdt.internal.compiler.ClassFile
import org.eclipse.jdt.internal.compiler.CompilationResult
import org.eclipse.jdt.internal.compiler.Compiler
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies
import org.eclipse.jdt.internal.compiler.ICompilerRequestor
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader
import org.eclipse.jdt.internal.compiler.env.INameEnvironment
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory
import java.util.Locale

/**
 * Compila el programa de un estudiante con ECJ (compilador real de Java, nivel 17)
 * contra la [StubLibrary]. No ejecuta nada.
 *
 * Restricciones del prototipo: un único archivo `Main.java`, sin `package`, con una
 * clase `Main`. Solo se usan las clases del compilador por lotes de ECJ (no `javax.tools`,
 * que Android no tiene).
 */
class JavaSourceCompiler(private val stubs: StubLibrary) {

    sealed interface Outcome {
        /** Nombre binario (`Main`, `Main$Punto`…) → archivo de clase. */
        data class Success(val classes: Map<String, ByteArray>) : Outcome
        data class Failure(val diagnostics: List<Diagnostic>) : Outcome
    }

    /**
     * @param extraLibrarySources SOLO PARA PRUEBAS DE AISLAMIENTO: declaraciones de clases
     *   no permitidas (p. ej. `java.io.File`) para fabricar código que esquive esta capa y
     *   comprobar que las siguientes lo detienen. No se incluyen en el resultado.
     */
    fun compile(source: String, extraLibrarySources: List<String> = emptyList()): Outcome {
        val unit = CompilationUnit(source.toCharArray(), FILE_NAME, "UTF-8")
        val extraUnits = extraLibrarySources.mapIndexed { i, text -> CompilationUnit(text.toCharArray(), "Extra$i.java", "UTF-8") }
        val errors = mutableListOf<Diagnostic>()
        val classes = linkedMapOf<String, ByteArray>()
        val packaged = mutableListOf<String>()
        val requestor = ICompilerRequestor { result: CompilationResult ->
            if (String(result.getFileName()) != FILE_NAME) return@ICompilerRequestor
            result.problems.orEmpty().filter { it.isError }.forEach {
                errors += Diagnostic(it.sourceLineNumber.takeIf { line -> line > 0 }, it.message)
            }
            result.classFiles.orEmpty().forEach { file: ClassFile ->
                val compound = file.compoundName
                if (compound.size > 1) packaged += compound.joinToString(".") { String(it) }
                classes[compound.joinToString(".") { String(it) }] = file.bytes
            }
        }
        Compiler(
            Environment(),
            DefaultErrorHandlingPolicies.proceedWithAllProblems(),
            CompilerOptions(OPTIONS),
            requestor,
            DefaultProblemFactory(Locale.ENGLISH),
        ).compile(arrayOf(unit, *extraUnits.toTypedArray()))

        if (errors.isNotEmpty()) return Outcome.Failure(errors.sortedBy { it.line ?: Int.MAX_VALUE })
        if (packaged.isNotEmpty()) return Outcome.Failure(listOf(Diagnostic(null, NO_PACKAGES)))
        if (MAIN_CLASS !in classes) return Outcome.Failure(listOf(Diagnostic(null, NO_MAIN)))
        return Outcome.Success(classes)
    }

    /** Solo resuelve las clases permitidas; el resto «no existe» para el compilador. */
    private inner class Environment : INameEnvironment {
        override fun findType(compoundTypeName: Array<CharArray>): NameEnvironmentAnswer? =
            answer(compoundTypeName.joinToString("/") { String(it) })

        override fun findType(typeName: CharArray, packageName: Array<CharArray>): NameEnvironmentAnswer? =
            answer((packageName.map { String(it) } + String(typeName)).joinToString("/"))

        override fun isPackage(parentPackageName: Array<CharArray>?, packageName: CharArray): Boolean {
            val parent = parentPackageName.orEmpty().joinToString("/") { String(it) }
            val name = if (parent.isEmpty()) String(packageName) else "$parent/${String(packageName)}"
            return name in stubs.packages
        }

        override fun cleanup() = Unit

        private fun answer(internalName: String): NameEnvironmentAnswer? {
            val bytes = stubs.classBytes(internalName) ?: return null
            return NameEnvironmentAnswer(ClassFileReader(bytes, "$internalName.class".toCharArray()), null)
        }
    }

    companion object {
        const val FILE_NAME = "Main.java"
        const val MAIN_CLASS = "Main"
        const val NO_PACKAGES = "No se permiten declaraciones package: el programa va en el paquete por defecto."
        const val NO_MAIN = "El programa debe declarar una clase Main con el método main."

        private val OPTIONS = mapOf(
            CompilerOptions.OPTION_Compliance to "17",
            CompilerOptions.OPTION_Source to "17",
            CompilerOptions.OPTION_TargetPlatform to "17",
            // Concatenación con StringBuilder (clase permitida), no con invokedynamic.
            CompilerOptions.OPTION_UseStringConcatFactory to CompilerOptions.DISABLED,
            CompilerOptions.OPTION_Process_Annotations to CompilerOptions.DISABLED,
            CompilerOptions.OPTION_LineNumberAttribute to CompilerOptions.GENERATE,
            CompilerOptions.OPTION_SourceFileAttribute to CompilerOptions.GENERATE,
            CompilerOptions.OPTION_LocalVariableAttribute to CompilerOptions.DO_NOT_GENERATE,
            CompilerOptions.OPTION_ReportDeprecation to CompilerOptions.IGNORE,
        )
    }
}

package com.funcode.academy.domain.lesson

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Files
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider

/**
 * Comprueba el Java del contenido con el compilador real (javac), **en la máquina de
 * pruebas** (CI o el equipo de desarrollo), nunca en el teléfono:
 * - los ejemplos y programas completos compilan con `--release 17` (P7, provisional);
 * - en "predecir la salida", la salida real coincide con la respuesta aceptada;
 * - en "encontrar errores", javac falla exactamente en las líneas marcadas;
 * - "completar huecos" y "ordenar líneas" forman programas que compilan.
 * Solo se comprueban textos que declaran una clase Java (el pseudocódigo se omite).
 */
class JavaContentCompilationTest {

    private val compiler = ToolProvider.getSystemJavaCompiler()

    private class Compilation(val errorLines: Set<Long>, val messages: List<String>, val classesDir: File, val className: String)

    private fun isJavaProgram(code: String) = CLASS_NAME.containsMatchIn(code)

    private fun compile(code: String): Compilation {
        assertNotNull("Estas pruebas necesitan un JDK con javac (no basta un JRE)", compiler)
        val className = CLASS_NAME.find(code)!!.groupValues[1]
        val dir = Files.createTempDirectory("funcode-java").toFile()
        val source = File(dir, "$className.java").apply { writeText(code + "\n", Charsets.UTF_8) }
        val out = File(dir, "classes").apply { mkdirs() }
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        compiler.getStandardFileManager(diagnostics, null, Charsets.UTF_8).use { fm ->
            compiler.getTask(
                null, fm, diagnostics, listOf("--release", "17", "-encoding", "UTF-8", "-d", out.path),
                null, fm.getJavaFileObjects(source),
            ).call()
        }
        val errors = diagnostics.diagnostics.filter { it.kind == Diagnostic.Kind.ERROR }
        return Compilation(errors.map { it.lineNumber }.toSet(), errors.map { "${it.lineNumber}: ${it.getMessage(null)}" }, out, className)
    }

    /** Ejecuta `main` capturando la salida estándar en UTF-8. */
    private fun run(c: Compilation): String {
        val buffer = ByteArrayOutputStream()
        val original = System.out
        URLClassLoader(arrayOf(c.classesDir.toURI().toURL()), null).use { loader ->
            val main = loader.loadClass(c.className).getMethod("main", Array<String>::class.java)
            System.setOut(PrintStream(buffer, true, "UTF-8"))
            try {
                main.invoke(null, arrayOf<String>())
            } finally {
                System.setOut(original)
            }
        }
        return buffer.toString("UTF-8")
    }

    private fun assertCompiles(id: String, code: String) {
        val c = compile(code)
        assertTrue("$id no compila: ${c.messages}", c.errorLines.isEmpty())
    }

    private val lessons = ContentFiles.lessons

    @Test fun `los ejemplos de codigo de la teoria compilan`() {
        var checked = 0
        lessons.forEach { lesson ->
            lesson.presentations.values.flatten().filterIsInstance<Section.Code>().filter { isJavaProgram(it.code) }.forEach {
                assertCompiles(lesson.id, it.code)
                checked++
            }
        }
        assertTrue("No se comprobó ningún ejemplo", checked > 0)
    }

    @Test fun `predecir la salida coincide con la ejecucion real del programa`() {
        var checked = 0
        lessons.flatMap { it.exercises }.forEach { e ->
            val content = e.content as? ExerciseContent.PredictOutput ?: return@forEach
            if (!isJavaProgram(content.code)) return@forEach
            val c = compile(content.code)
            assertTrue("${e.id} no compila: ${c.messages}", c.errorLines.isEmpty())
            assertEquals(e.id, CodeText.normalizeOutput(content.acceptedOutputs.first()), CodeText.normalizeOutput(run(c)))
            checked++
        }
        assertTrue(checked > 0)
    }

    @Test fun `encontrar errores falla exactamente en las lineas marcadas`() {
        var checked = 0
        lessons.flatMap { it.exercises }.forEach { e ->
            val content = e.content as? ExerciseContent.FindError ?: return@forEach
            if (!isJavaProgram(content.code)) return@forEach
            val c = compile(content.code)
            assertEquals("${e.id}: ${c.messages}", content.errorLines.map { it.toLong() }.toSet(), c.errorLines)
            checked++
        }
        assertTrue(checked > 0)
    }

    @Test fun `completar huecos y ordenar lineas forman programas que compilan`() {
        lessons.flatMap { it.exercises }.forEach { e ->
            when (val content = e.content) {
                is ExerciseContent.FillInBlanks -> content.acceptedAnswers.indices.fold(content.template) { code, i ->
                    code.replace("{{$i}}", content.acceptedAnswers[i].first())
                }.takeIf(::isJavaProgram)?.let { assertCompiles(e.id, it) }
                is ExerciseContent.OrderLines -> content.lines.joinToString("\n")
                    .takeIf(::isJavaProgram)?.let { assertCompiles(e.id, it) }
                else -> Unit
            }
        }
    }

    private companion object {
        val CLASS_NAME = Regex("""\bclass\s+([A-Za-z_]\w*)""")
    }
}

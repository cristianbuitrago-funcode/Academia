package com.funcode.academy.domain.lesson

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Android usa el motor de expresiones regulares de ICU, más estricto que el de la JVM:
 * una llave `{` o `}` sin escapar que no forme un cuantificador (`{2}`, `{1,3}`) lanza
 * `PatternSyntaxException` en el teléfono, aunque las pruebas JVM y Robolectric pasen.
 * Este fallo cerraba la app al abrirla (las plantillas usan el marcador `{{n}}`).
 *
 * La prueba revisa todas las expresiones regulares escritas en el código de producción.
 */
class RegexPortabilityTest {

    private val sourceRoots = listOf(File("src/main"), File("../../app/src/main"))

    @Test
    fun `el marcador de hueco reconoce {{n}}`() {
        val found = LessonParser.BLANK_MARKER.findAll("a {{0}} b {{12}} c {0}").map { it.groupValues[1] }.toList()
        assertEquals(listOf("0", "12"), found)
    }

    @Test
    fun `el comprobador detecta llaves sin escapar`() {
        assertTrue(unescapedBraces("""\{\{(\d+)}}""").isNotEmpty())
        assertTrue(unescapedBraces("a{b").isNotEmpty())
        assertTrue(unescapedBraces("""[{]""").isNotEmpty())
        assertTrue(unescapedBraces("""\{\{(\d+)\}\}""").isEmpty())
        assertTrue(unescapedBraces("""a{2}b{1,}c{1,3}""").isEmpty())
    }

    @Test
    fun `ninguna expresion regular del codigo usa llaves que Android rechaza`() {
        val sources = sourceRoots.flatMap { root -> root.walkTopDown().filter { it.extension == "kt" }.toList() }
        assertTrue("No se encontró código fuente que revisar", sources.size > 10)
        val problems = sources.flatMap { file ->
            regexLiterals(file.readText()).mapNotNull { pattern ->
                unescapedBraces(pattern).takeIf { it.isNotEmpty() }?.let { "${file.path}: «$pattern» (posiciones $it)" }
            }
        }
        assertTrue("Expresiones no válidas en Android:\n${problems.joinToString("\n")}", problems.isEmpty())
    }

    /** Patrones escritos en una línea como `Regex("""…""")`, `Regex("…")` o `"…".toRegex()`. */
    private fun regexLiterals(code: String): List<String> = code.lines().flatMap(::regexLiteralsInLine)

    private fun regexLiteralsInLine(code: String): List<String> {
        val raw = Regex("Regex\\(\"\"\"(.*?)\"\"\"").findAll(code).map { it.groupValues[1] }
        val raw2 = Regex("\"\"\"(.*?)\"\"\"\\.toRegex").findAll(code).map { it.groupValues[1] }
        val escaped = (Regex("Regex\\(\"((?:\\\\.|[^\"\\\\])*+)\"").findAll(code) +
            Regex("\"((?:\\\\.|[^\"\\\\])*+)\"\\.toRegex").findAll(code))
            .map { it.groupValues[1].replace("\\\\", "\\") }
        return (raw + raw2 + escaped).toList()
    }

    /** Posiciones de llaves sin escapar que no forman un cuantificador válido fuera de `[...]`. */
    private fun unescapedBraces(pattern: String): List<Int> {
        val bad = mutableListOf<Int>()
        var inClass = false
        var i = 0
        while (i < pattern.length) {
            when (val c = pattern[i]) {
                '\\' -> i++
                '[' -> inClass = true
                ']' -> inClass = false
                '{', '}' -> {
                    val quantifier = if (c == '{' && !inClass) QUANTIFIER.matchAt(pattern, i) else null
                    if (quantifier != null) i += quantifier.value.length - 1 else bad += i
                }
            }
            i++
        }
        return bad
    }

    private companion object {
        val QUANTIFIER = Regex("""\{\d+(,\d*)?\}""")
    }
}

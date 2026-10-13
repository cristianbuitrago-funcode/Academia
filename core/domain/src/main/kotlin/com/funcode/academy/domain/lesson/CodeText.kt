package com.funcode.academy.domain.lesson

/**
 * Normalización de texto para comparar respuestas sin depender de detalles irrelevantes.
 */
internal object CodeText {

    /**
     * Divide código Java en tokens: identificadores/números, literales de texto y de
     * carácter (se conservan completos, con sus espacios) y signos de puntuación.
     * Los espacios entre tokens se ignoran, así `x+1` y `x + 1` son equivalentes.
     */
    fun tokenize(source: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < source.length) {
            val c = source[i]
            when {
                c.isWhitespace() -> i++
                c == '"' || c == '\'' -> {
                    val start = i
                    i++
                    while (i < source.length && source[i] != c) {
                        if (source[i] == '\\' && i + 1 < source.length) i++
                        i++
                    }
                    i = minOf(i + 1, source.length)
                    tokens += source.substring(start, i)
                }
                c.isLetterOrDigit() || c == '_' || c == '$' -> {
                    val start = i
                    while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_' || source[i] == '$' || source[i] == '.' && source.getOrNull(i + 1)?.isDigit() == true && source[start].isDigit())) i++
                    tokens += source.substring(start, i)
                }
                else -> {
                    tokens += c.toString()
                    i++
                }
            }
        }
        return tokens
    }

    /**
     * Normaliza la salida de un programa: unifica saltos de línea, quita espacios al
     * final de cada línea y líneas vacías al final. Mayúsculas y espacios internos
     * se conservan, porque forman parte de la salida real.
     */
    fun normalizeOutput(text: String): String =
        text.replace("\r\n", "\n").replace('\r', '\n')
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .trimEnd('\n')

    /** Normaliza una línea de código para compararla: tokens unidos por un espacio. */
    fun normalizeLine(line: String): String = tokenize(line).joinToString(" ")
}

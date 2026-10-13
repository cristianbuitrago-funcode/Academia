package com.funcode.academy.domain.lesson

import org.junit.Assert.assertEquals
import org.junit.Test

class CodeTextTest {

    @Test fun `tokeniza identificadores, signos y textos`() =
        assertEquals(
            listOf("System", ".", "out", ".", "println", "(", "\"a b\"", ")", ";"),
            CodeText.tokenize("System.out.println( \"a b\" ) ;"),
        )

    @Test fun `conserva los numeros decimales como un solo token`() =
        assertEquals(listOf("x", "=", "3.5", ";"), CodeText.tokenize("x=3.5;"))

    @Test fun `respeta comillas escapadas dentro del texto`() =
        assertEquals(listOf("\"a\\\"b\""), CodeText.tokenize("\"a\\\"b\""))

    @Test fun `un texto sin cerrar no rompe el analisis`() =
        assertEquals(listOf("\"abc"), CodeText.tokenize("\"abc"))

    @Test fun `normaliza la salida`() = assertEquals("a\n  b", CodeText.normalizeOutput("a  \r\n  b\r\n\n"))
}

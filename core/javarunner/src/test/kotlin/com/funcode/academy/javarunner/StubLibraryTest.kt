package com.funcode.academy.javarunner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

class StubLibraryTest {
    private val stubs = JvmTestRunner.stubs

    @Test
    fun `la biblioteca contiene lo basico y nada peligroso`() {
        assertTrue("java/lang/String" in stubs.internalNames)
        assertTrue("java/io/PrintStream" in stubs.internalNames)
        val forbidden = listOf(
            "java/lang/Class", "java/lang/ClassLoader", "java/lang/Thread", "java/lang/Runtime", "java/lang/ProcessBuilder",
            "java/io/File", "java/io/FileInputStream", "java/net/Socket", "java/lang/reflect/Method", "java/util/Scanner",
        )
        forbidden.forEach { assertFalse("$it no debe estar permitida", it in stubs.internalNames) }
        assertFalse(stubs.packages.any { it.startsWith("java/lang/reflect") || it.startsWith("java/net") || it.startsWith("java/util") })
    }

    /** Cada miembro público declarado existe en la JVM real (en Android lo comprueba la prueba instrumentada). */
    @Test
    fun `cada metodo y campo declarado existe en la JVM real`() {
        val missing = mutableListOf<String>()
        StubConformance.check(stubs) { name -> Class.forName(name, false, null) }.forEach { missing += it }
        assertEquals("Miembros que no existen en la JVM: $missing", emptyList<String>(), missing)
    }

    @Test
    fun `System solo expone out`() {
        val fields = StubConformance.publicMembers(stubs, "java/lang/System")
        assertEquals(listOf("field out Ljava/io/PrintStream;"), fields)
        assertTrue(Modifier.isFinal(System::class.java.modifiers))
    }
}

package com.funcode.academy.javarunner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JavaSourceCompilerTest {

    private fun program(body: String) = "public class Main {\n    public static void main(String[] args) {\n        $body\n    }\n}\n"

    private fun assertRejected(body: String, mentions: String, line: Int? = 3) {
        val errors = JvmTestRunner.compileErrors(program(body))
        assertTrue("Debía fallar: $body", errors.isNotEmpty())
        assertTrue("Errores sin «$mentions»: $errors", errors.any { mentions in it.message })
        if (line != null) assertEquals("El error debe estar en la línea $line: $errors", line, errors.first().line)
    }

    @Test
    fun `un error de sintaxis da diagnostico con linea`() {
        val errors = JvmTestRunner.compileErrors(program("System.out.println(\"Hola\")"))
        assertEquals(3, errors.single().line)
        assertTrue(errors.single().message, errors.single().message.contains(";"))
    }

    @Test
    fun `un tipo incorrecto es un error de compilacion`() {
        assertRejected("int x = \"texto\";", "String")
    }

    @Test
    fun `las clases peligrosas no existen para el compilador`() {
        assertRejected("new java.io.File(\"/data\").delete();", "java.io")
        assertRejected("java.io.FileInputStream f = null;", "java.io.FileInputStream")
        assertRejected("Runtime.getRuntime().exec(\"sh\");", "Runtime")
        assertRejected("new ProcessBuilder(\"sh\").start();", "ProcessBuilder")
        assertRejected("new Thread(null).start();", "Thread")
        assertRejected("Class.forName(\"java.io.File\");", "Class")
        assertRejected("Object o = new Object(); o.getClass();", "getClass")
        assertRejected("\"x\".getClass().getClassLoader();", "getClass")
        assertRejected("java.lang.reflect.Method m = null;", "java.lang.reflect")
        assertRejected("new java.net.Socket(\"example.com\", 80);", "java.net")
        assertRejected("java.util.Scanner s = new java.util.Scanner(System.in);", "java.util")
        assertRejected("System.exit(0);", "exit")
        assertRejected("System.getenv(\"HOME\");", "getenv")
        assertRejected("System.getProperty(\"user.home\");", "getProperty")
        assertRejected("System.err.println(1);", "err")
        // ECJ lo informa como referencia indirecta (línea 1), pero lo rechaza igualmente.
        assertRejected("Object o = Main.class;", "java.lang.Class", line = null)
    }

    @Test
    fun `no se permiten paquetes ni falta de Main`() {
        val withPackage = JvmTestRunner.compileErrors("package a;\npublic class Main { public static void main(String[] a) {} }")
        assertTrue(withPackage.toString(), withPackage.isNotEmpty())
        val noMain = JvmTestRunner.compileErrors("class Otro { }")
        assertEquals(JavaSourceCompiler.NO_MAIN, noMain.single().message)
    }

    @Test
    fun `construcciones de Java 17 compatibles`() {
        val source = """
            public class Main {
                static int doble(int n) { return n * 2; }
                public static void main(String[] args) {
                    var texto = ""${'"'}
                        Hola""${'"'};
                    int dia = 3;
                    String nombre = switch (dia) { case 1 -> "lunes"; case 3 -> "miércoles"; default -> "otro"; };
                    Object o = 5;
                    if (o instanceof Integer i && i > 2) System.out.println(texto.trim() + " " + nombre + " " + doble(i));
                }
            }
        """.trimIndent()
        assertEquals(emptyList<Any>(), JvmTestRunner.compileErrors(source))
    }
}

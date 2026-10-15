package com.funcode.academy.javarunner

import com.funcode.academy.domain.runner.Limit
import com.funcode.academy.domain.runner.RejectReason
import com.funcode.academy.domain.runner.RunResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramExecutionTest {

    private fun program(body: String) = "public class Main {\n    public static void main(String[] args) {\n        $body\n    }\n}\n"

    @Test
    fun `programa valido con salida real`() {
        val r = JvmTestRunner.run(program("int x = 7 * 6; System.out.println(\"Hola \" + x); System.out.println(10 / 4.0);"))
        assertEquals(RunResult.Completed("Hola 42\n2.5\n", r.timings), r)
    }

    @Test
    fun `metodos, bucles, arrays y StringBuilder`() {
        val source = """
            public class Main {
                static int suma(int[] v) { int s = 0; for (int x : v) s += x; return s; }
                public static void main(String[] args) {
                    int[] v = {1, 2, 3, 4};
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < v.length; i++) sb.append(v[i]).append(i < v.length - 1 ? "," : "");
                    System.out.println(sb + " -> " + suma(v));
                    String s = "Java";
                    switch (s) { case "Java": System.out.println(s.toUpperCase().length()); break; default: break; }
                    Integer boxed = 41; boxed++;
                    System.out.println(boxed + Math.max(1, 2));
                }
            }
        """.trimIndent()
        assertEquals("1,2,3,4 -> 10\n4\n44\n", (JvmTestRunner.run(source) as RunResult.Completed).stdout)
    }

    @Test
    fun `error de ejecucion con su linea`() {
        val r = JvmTestRunner.run(program("System.out.println(\"antes\");\n        int cero = 0;\n        System.out.println(1 / cero);"))
        r as RunResult.RuntimeError
        assertEquals("ArithmeticException", r.exception)
        assertEquals(5, r.line)
        assertEquals("antes\n", r.stdout)
    }

    @Test
    fun `indice fuera de rango y recursion infinita`() {
        assertEquals("ArrayIndexOutOfBoundsException", (JvmTestRunner.run(program("int[] a = new int[2]; a[5] = 1;")) as RunResult.RuntimeError).exception)
        val recursion = "public class Main { static int f(int n) { return f(n + 1) + 1; } public static void main(String[] a) { f(0); } }"
        assertEquals("StackOverflowError", (JvmTestRunner.run(recursion) as RunResult.RuntimeError).exception)
    }

    @Test
    fun `codigo vacio o demasiado grande no se compila`() {
        assertEquals(RejectReason.EMPTY_SOURCE, (JvmTestRunner.run("   ") as RunResult.Rejected).reason)
        val big = program("int x = 0;" + " ".repeat(10_000))
        assertEquals(RejectReason.SOURCE_TOO_LARGE, (JvmTestRunner.run(big) as RunResult.Rejected).reason)
    }

    @Test
    fun `memoria agotada se informa como limite`() {
        val r = JvmTestRunner.run(program("long[] enorme = new long[Integer.MAX_VALUE - 8]; System.out.println(enorme.length);"))
        assertEquals(Limit.MEMORY, (r as RunResult.LimitExceeded).limit)
    }

    @Test
    fun `una clase fuera de la lista permitida falla al ejecutarse aunque el bytecode la mencione`() {
        // Bytecode generado con el javac real (sin la lista permitida) que usa java.io.File:
        // simula un programa que hubiera esquivado la compilación restringida.
        val classes = RealJavac.compile(
            "Main",
            "public class Main { public static void main(String[] a) throws Exception { System.out.println(new java.io.File(\"/\").exists()); } }",
        )
        val r = JvmTestRunner.runClasses(classes)
        r as RunResult.RuntimeError
        assertTrue(r.exception, r.exception == "NoClassDefFoundError")
        assertTrue(r.message.orEmpty(), r.message.orEmpty().contains("java/io/File") || r.message.orEmpty().contains("java.io.File"))
    }
}

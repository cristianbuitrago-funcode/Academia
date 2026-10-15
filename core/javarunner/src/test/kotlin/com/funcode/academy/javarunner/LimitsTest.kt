package com.funcode.academy.javarunner

import com.funcode.academy.domain.runner.Limit
import com.funcode.academy.domain.runner.RunResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas con hilos que no terminan: Gradle usa un proceso nuevo por clase (forkEvery = 1). */
class LimitsTest {

    @Test
    fun `bucle infinito se detiene por tiempo`() {
        val started = System.nanoTime()
        val r = JvmTestRunner.run("public class Main { public static void main(String[] a) { while (true) { } } }", timeoutMs = 1_000)
        val ms = (System.nanoTime() - started) / 1_000_000
        assertEquals(Limit.TIME, (r as RunResult.LimitExceeded).limit)
        assertTrue("Tardó $ms ms", ms < 5_000)
    }

    @Test
    fun `salida excesiva se corta en el limite`() {
        val r = JvmTestRunner.run(
            "public class Main { public static void main(String[] a) { while (true) { System.out.print(\"ab\"); } } }",
            maxOutputChars = 1_000,
        )
        r as RunResult.LimitExceeded
        assertEquals(Limit.OUTPUT, r.limit)
        assertEquals(1_000, r.stdout.length)
    }

    @Test
    fun `salida excesiva aunque el programa capture el error`() {
        val r = JvmTestRunner.run(
            "public class Main { public static void main(String[] a) { for (int i = 0; i < 100000; i++) { try { System.out.print(\"ñ\"); } catch (Throwable t) { } } } }",
            maxOutputChars = 100,
        )
        r as RunResult.LimitExceeded
        assertEquals(Limit.OUTPUT, r.limit)
        assertEquals("ñ".repeat(100), r.stdout)
    }
}

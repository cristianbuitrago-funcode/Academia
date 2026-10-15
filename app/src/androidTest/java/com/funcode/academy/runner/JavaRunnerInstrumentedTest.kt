package com.funcode.academy.runner

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.funcode.academy.domain.runner.Limit
import com.funcode.academy.domain.runner.ProgramEvaluation
import com.funcode.academy.domain.runner.RunLimits
import com.funcode.academy.domain.runner.RunRequest
import com.funcode.academy.domain.runner.RunResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * PROTOTIPO Fase 8: pruebas REALES en el emulador (ECJ + D8 + proceso aislado).
 * Las medidas se escriben en logcat con la etiqueta «FuncodeMedidas» y el CI las muestra.
 */
@RunWith(AndroidJUnit4::class)
class JavaRunnerInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val runner = IsolatedJavaRunner(context)

    /**
     * LIMITACIÓN CONOCIDA (no oculta): ECJ 3.28 usa InputStream.readAllBytes (Android 13,
     * API 33). En API 26–32 el compilador no arranca; lo comprueba
     * [JavaRunnerApiLimitTest]. Estas pruebas solo tienen sentido donde el compilador funciona.
     */
    @Before
    fun requiereApi33() = assumeTrue("ECJ 3.28 necesita API 33 o superior", Build.VERSION.SDK_INT >= 33)

    private fun program(body: String) = "public class Main {\n    public static void main(String[] args) {\n        $body\n    }\n}\n"

    private fun run(source: String, limits: RunLimits = RunLimits(), diagnostics: Bundle? = null): RunResult = runBlocking {
        val started = SystemClock.elapsedRealtime()
        val r = runner.runWithDiagnostics(RunRequest(source, limits), diagnostics)
        val wall = SystemClock.elapsedRealtime() - started
        Log.i(TAG, "API ${Build.VERSION.SDK_INT} ${Build.MODEL} | ${r.javaClass.simpleName} | total(app)=${wall}ms " +
            "compilar=${r.timings.compileMs} dex=${r.timings.dexMs} ejecutar=${r.timings.executeMs} total(proceso)=${r.timings.totalMs}")
        r
    }

    private fun assertCompleted(r: RunResult): RunResult.Completed {
        assertTrue("Se esperaba Completed: $r", r is RunResult.Completed)
        return r as RunResult.Completed
    }

    @Test
    fun programaValidoConSalidaRealYMedidas() {
        val source = program("int a = 7, b = 5;\n        System.out.println(\"Hola, Funcode\");\n        System.out.println(a + \" + \" + b + \" = \" + (a + b));")
        repeat(3) {
            val r = assertCompleted(run(source))
            assertEquals("Hola, Funcode\n7 + 5 = 12\n", r.stdout)
            assertEquals(ProgramEvaluation.Outcome.OutputMatches, ProgramEvaluation.evaluate(r, listOf("Hola, Funcode\n7 + 5 = 12")))
        }
    }

    @Test
    fun aritmeticaDoublesYCadenas() {
        val r = assertCompleted(run(program("System.out.println(10 / 4);\n        System.out.println(10 / 4.0);\n        System.out.println(\"Java\".toUpperCase() + \"Java\".length());\n        System.out.println(Math.max(3, 9) + Integer.parseInt(\"5\"));")))
        assertEquals("2\n2.5\nJAVA4\n14\n", r.stdout)
    }

    @Test
    fun errorDeSintaxisConLinea() {
        val r = run(program("System.out.println(\"Hola\")"))
        assertTrue("$r", r is RunResult.CompilationFailed)
        assertEquals(3, (r as RunResult.CompilationFailed).diagnostics.first().line)
    }

    @Test
    fun errorDeEjecucionConLinea() {
        val r = run(program("int cero = 0;\n        System.out.println(5 / cero);"))
        assertTrue("$r", r is RunResult.RuntimeError)
        r as RunResult.RuntimeError
        assertEquals("ArithmeticException", r.exception)
        assertEquals(4, r.line)
    }

    @Test
    fun apisPeligrosasNoCompilan() {
        listOf(
            "new java.io.File(\"/data\").delete();",
            "Runtime.getRuntime().exec(\"id\");",
            "Class.forName(\"android.app.ActivityThread\");",
            "new Thread(null).start();",
            "new java.net.Socket(\"10.0.2.2\", 80);",
            "System.exit(0);",
        ).forEach { body -> assertTrue(body, run(program(body)) is RunResult.CompilationFailed) }
    }

    @Test
    fun bucleInfinitoSeDetieneYLaInterfazSigueRespondiendo() = runBlocking {
        val limits = RunLimits(timeoutMs = 3_000)
        val started = SystemClock.elapsedRealtime()
        val pending = async(Dispatchers.IO) { runner.run(RunRequest(program("while (true) { }"), limits)) }
        delay(1_000)
        // Mientras el programa ocupa la CPU en otro proceso, el hilo principal responde enseguida.
        val mainStart = SystemClock.elapsedRealtime()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { }
        val mainLatency = SystemClock.elapsedRealtime() - mainStart
        val r = pending.await()
        val elapsed = SystemClock.elapsedRealtime() - started
        Log.i(TAG, "bucle infinito: ${r.javaClass.simpleName} en ${elapsed}ms; latencia del hilo principal ${mainLatency}ms")
        assertEquals(Limit.TIME, (r as RunResult.LimitExceeded).limit)
        assertTrue("Latencia del hilo principal $mainLatency ms", mainLatency < 1_000)
        // Tras el bucle infinito, la app se recupera: otra ejecución funciona.
        assertEquals("ok\n", assertCompleted(run(program("System.out.println(\"ok\");"))).stdout)
    }

    @Test
    fun memoriaAgotadaSeInformaYLaAppSeRecupera() {
        val r = run(program("long[][] guardar = new long[100000][];\n        for (int i = 0; i < guardar.length; i++) guardar[i] = new long[1000000];"))
        Log.i(TAG, "memoria: $r")
        // OutOfMemoryError capturado en el proceso aislado, o el proceso terminado por el sistema: ambos son MEMORY.
        assertTrue("$r", r is RunResult.LimitExceeded && r.limit == Limit.MEMORY)
        assertEquals("ok\n", assertCompleted(run(program("System.out.println(\"ok\");"))).stdout)
    }

    @Test
    fun salidaExcesivaSeCorta() {
        val r = run(program("while (true) { System.out.print(\"0123456789\"); }"))
        r as RunResult.LimitExceeded
        assertEquals(Limit.OUTPUT, r.limit)
        assertEquals(10_000, r.stdout.length)
    }

    @Test
    fun codigoVacioODemasiadoGrandeNoSeEnvia() {
        assertTrue(run("  ") is RunResult.Rejected)
        assertTrue(run(program("int x = 0;" + " ".repeat(10_000))) is RunResult.Rejected)
    }

    @Test
    fun elEstadoNoSeConservaEntreEjecuciones() {
        val source = "public class Main { static int veces; public static void main(String[] a) { veces++; System.out.println(veces); } }"
        assertEquals("1\n", assertCompleted(run(source)).stdout)
        assertEquals("1\n", assertCompleted(run(source)).stdout)
    }

    @Test
    fun cancelarDetieneLaEjecucion() = runBlocking {
        val pending = async(Dispatchers.IO) { runner.run(RunRequest(program("while (true) { }"))) }
        delay(1_500)
        val started = SystemClock.elapsedRealtime()
        pending.cancel()
        runCatching { pending.await() }
        val stopMs = SystemClock.elapsedRealtime() - started
        Log.i(TAG, "cancelar: detenido en ${stopMs}ms")
        assertTrue("Cancelar tardó $stopMs ms", stopMs < 3_000)
        assertEquals("ok\n", assertCompleted(run(program("System.out.println(\"ok\");"))).stdout)
    }

    /** Capa 2: aunque el compilador «conociera» java.io.File, la ejecución no puede cargarla. */
    @Test
    fun capa2ListaPermitidaEnEjecucion() {
        val diagnostics = Bundle().apply {
            putStringArrayList(RunnerProtocol.DIAG_EXTRA_LIBRARY, arrayListOf(
                "package java.io; public class File { public File(String p) {} public boolean exists() { return false; } }",
            ))
        }
        val r = run(program("System.out.println(new java.io.File(\"/\").exists());"), diagnostics = diagnostics)
        Log.i(TAG, "capa 2: $r")
        assertTrue("$r", r is RunResult.RuntimeError)
        r as RunResult.RuntimeError
        assertEquals("NoClassDefFoundError", r.exception)
        assertTrue(r.message.orEmpty(), r.message.orEmpty().contains("java/io/File") || r.message.orEmpty().contains("java.io.File"))
    }

    /**
     * Capa 3 (sistema operativo), con las capas 1 y 2 DESACTIVADAS a propósito (solo en
     * compilaciones de depuración): el proceso aislado no puede leer los archivos de la app
     * ni abrir conexiones de red. El resultado de ejecutar un proceso se registra.
     */
    @Test
    fun capa3ProcesoAisladoSinArchivosNiRed() {
        val secret = File(context.filesDir, "secreto.txt").apply { writeText("dato privado") }
        val database = context.getDatabasePath("funcode.db")
        val diagnostics = Bundle().apply {
            putBoolean(RunnerProtocol.DIAG_UNRESTRICTED_LOADER, true)
            putStringArrayList(RunnerProtocol.DIAG_EXTRA_LIBRARY, arrayListOf(
                "package java.io; public class FileInputStream { public FileInputStream(String p) {} public int read() { return 0; } }",
                "package java.net; public class Socket { public Socket(String h, int p) {} }",
                "package java.lang; public class Runtime { public static Runtime getRuntime() { return null; } public Process exec(String c) { return null; } }",
                "package java.lang; public abstract class Process { public abstract int waitFor(); }",
            ))
        }
        val source = """
            public class Main {
                static void leer(String etiqueta, String ruta) {
                    try { new java.io.FileInputStream(ruta).read(); System.out.println(etiqueta + ":LEIDO"); }
                    catch (Throwable t) { System.out.println(etiqueta + ":BLOQUEADO " + t); }
                }
                public static void main(String[] args) {
                    leer("ARCHIVO", "${secret.absolutePath}");
                    leer("BASEDATOS", "${database.absolutePath}");
                    try { new java.net.Socket("10.0.2.2", 80); System.out.println("RED:CONECTADO"); }
                    catch (Throwable t) { System.out.println("RED:BLOQUEADO " + t); }
                    try { int codigo = Runtime.getRuntime().exec("id").waitFor(); System.out.println("PROCESO:EJECUTADO codigo=" + codigo); }
                    catch (Throwable t) { System.out.println("PROCESO:BLOQUEADO " + t); }
                }
            }
        """.trimIndent()
        val r = run(source, diagnostics = diagnostics)
        Log.i(TAG, "capa 3: $r")
        val out = assertCompleted(r).stdout
        out.lines().filter { it.isNotBlank() }.forEach { Log.i(TAG, "capa 3 > $it") }
        assertTrue(out, out.contains("ARCHIVO:BLOQUEADO"))
        assertTrue(out, out.contains("BASEDATOS:BLOQUEADO"))
        assertTrue(out, out.contains("RED:BLOQUEADO"))
        assertFalse(out, out.contains("RED:CONECTADO"))
    }

    private companion object {
        const val TAG = "FuncodeMedidas"
    }
}

package com.funcode.academy.runner

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.funcode.academy.domain.runner.RunRequest
import com.funcode.academy.domain.runner.RunResult
import com.funcode.academy.javarunner.StubConformance
import com.funcode.academy.javarunner.StubLibrary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Pruebas que se ejecutan en TODAS las versiones de Android del CI (API 26 y 34). */
@RunWith(AndroidJUnit4::class)
class JavaRunnerApiLimitTest {

    /** Cada miembro de la biblioteca permitida existe en esta versión de Android. */
    @Test
    fun bibliotecaPermitidaExisteEnAndroid() {
        val missing = StubConformance.check(StubLibrary.load()) { Class.forName(it, false, null) }
        assertEquals("Miembros que faltan en API ${Build.VERSION.SDK_INT}: $missing", emptyList<String>(), missing)
    }

    /**
     * LIMITACIÓN CONOCIDA, registrada como prueba para que el CI avise si cambia:
     * en API < 33, ECJ 3.28 falla al cargar sus tablas (InputStream.readAllBytes no existe)
     * y el ejecutor responde «no disponible» sin cerrar la app ni inventar una salida.
     */
    @Test
    fun enApiMenorQue33ElCompiladorNoArrancaYSeInformaComoNoDisponible() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT < 33)
        val runner = IsolatedJavaRunner(ApplicationProvider.getApplicationContext<Context>())
        val r = runner.run(RunRequest("public class Main { public static void main(String[] a) { System.out.println(1); } }"))
        Log.i("FuncodeMedidas", "API ${Build.VERSION.SDK_INT}: limitación conocida -> $r")
        assertTrue("$r", r is RunResult.Unavailable)
        assertTrue((r as RunResult.Unavailable).reason, r.reason.contains("readAllBytes"))
    }
}

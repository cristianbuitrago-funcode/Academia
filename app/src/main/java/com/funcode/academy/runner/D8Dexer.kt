package com.funcode.academy.runner

import com.android.tools.r8.ByteDataView
import com.android.tools.r8.ClassFileResourceProvider
import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.DexIndexedConsumer
import com.android.tools.r8.DiagnosticsHandler
import com.android.tools.r8.ProgramResource
import com.android.tools.r8.origin.Origin
import com.funcode.academy.javarunner.StubLibrary

/**
 * Convierte los archivos de clase del estudiante a DEX con D8 (R8), en memoria.
 * La biblioteca permitida se pasa como «library» (no se incluye en el resultado).
 * Se ejecuta solo dentro del proceso aislado.
 */
internal object D8Dexer {

    fun dex(classes: Map<String, ByteArray>, stubs: StubLibrary): ByteArray {
        var output: ByteArray? = null
        val consumer = object : DexIndexedConsumer {
            override fun accept(fileIndex: Int, data: ByteDataView, descriptors: Set<String>?, handler: DiagnosticsHandler?) {
                check(fileIndex == 0) { "El programa genera más de un archivo DEX" }
                output = data.copyByteData()
            }

            override fun finished(handler: DiagnosticsHandler?) = Unit
        }
        val library = object : ClassFileResourceProvider {
            private val descriptors = stubs.internalNames.associateBy { "L$it;" }

            override fun getClassDescriptors(): Set<String> = descriptors.keys

            override fun getProgramResource(descriptor: String): ProgramResource? {
                val internal = descriptors[descriptor] ?: return null
                return ProgramResource.fromBytes(Origin.unknown(), ProgramResource.Kind.CF, stubs.classBytes(internal), setOf(descriptor))
            }
        }
        val builder = D8Command.builder()
            .setMinApiLevel(MIN_API)
            .setMode(CompilationMode.DEBUG) // conserva los números de línea para los errores
            .addLibraryResourceProvider(library)
            .setProgramConsumer(consumer)
        classes.values.forEach { builder.addClassProgramData(it, Origin.unknown()) }
        D8.run(builder.build())
        return output ?: error("D8 no generó código")
    }

    /** Igual que el minSdk de la app. */
    private const val MIN_API = 26
}

package com.funcode.academy.javarunner

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URI
import javax.tools.FileObject
import javax.tools.ForwardingJavaFileManager
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider

/** javac real de la JVM de pruebas, sin restricciones: solo para fabricar bytecode «malicioso». */
object RealJavac {
    fun compile(className: String, source: String): Map<String, ByteArray> {
        val javac = ToolProvider.getSystemJavaCompiler() ?: error("Las pruebas necesitan un JDK")
        val outputs = mutableMapOf<String, ByteArrayOutputStream>()
        val manager = object : ForwardingJavaFileManager<JavaFileManager>(javac.getStandardFileManager(null, null, null)) {
            override fun getJavaFileForOutput(location: JavaFileManager.Location, name: String, kind: JavaFileObject.Kind, sibling: FileObject?): JavaFileObject =
                object : SimpleJavaFileObject(URI.create("mem:///$name.class"), kind) {
                    override fun openOutputStream(): OutputStream = ByteArrayOutputStream().also { outputs[name] = it }
                }
        }
        val unit = object : SimpleJavaFileObject(URI.create("string:///$className.java"), JavaFileObject.Kind.SOURCE) {
            override fun getCharContent(ignoreEncodingErrors: Boolean) = source
        }
        check(javac.getTask(null, manager, null, listOf("--release", "17"), null, listOf(unit)).call()) { "javac falló" }
        return outputs.mapValues { it.value.toByteArray() }
    }
}

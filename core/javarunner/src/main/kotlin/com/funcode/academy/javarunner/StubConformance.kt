package com.funcode.academy.javarunner

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader
import java.lang.reflect.Modifier

/**
 * Comprueba que cada miembro público o protegido de la biblioteca permitida existe en
 * las clases reales del entorno de ejecución (JVM en pruebas, ART en el emulador).
 * Si un método no existiera en Android, el programa compilaría pero fallaría al ejecutarse.
 */
object StubConformance {

    /** Devuelve la lista de miembros que faltan (vacía si todo existe). */
    fun check(stubs: StubLibrary, lookup: (String) -> Class<*>): List<String> {
        val missing = mutableListOf<String>()
        stubs.internalNames.sorted().forEach { internal ->
            val real = try {
                lookup(internal.replace('/', '.'))
            } catch (e: ClassNotFoundException) {
                missing += "clase $internal"
                return@forEach
            }
            val reader = reader(stubs, internal)
            reader.methods.orEmpty().filter { visible(it.modifiers) }.forEach { m ->
                val name = String(m.selector)
                val descriptor = String(m.methodDescriptor)
                if (name == "<clinit>") return@forEach
                val params = parameterTypes(descriptor, real.classLoader)
                val found = try {
                    if (name == "<init>") real.getDeclaredConstructor(*params.toTypedArray()) != null
                    else findMethod(real, name, params) != null
                } catch (e: ReflectiveOperationException) {
                    false
                } catch (e: LinkageError) {
                    false
                }
                if (!found) missing += "$internal.$name$descriptor"
            }
            reader.fields.orEmpty().filter { visible(it.modifiers) }.forEach { f ->
                val name = String(f.name)
                val exists = try {
                    real.getField(name) != null
                } catch (e: NoSuchFieldException) {
                    false
                }
                if (!exists) missing += "$internal.$name"
            }
        }
        return missing
    }

    /** Miembros públicos de una clase permitida, para pruebas («field out Ljava/io/PrintStream;»). */
    fun publicMembers(stubs: StubLibrary, internal: String): List<String> {
        val reader = reader(stubs, internal)
        val methods = reader.methods.orEmpty().filter { Modifier.isPublic(it.modifiers) }.map { "method ${String(it.selector)}${String(it.methodDescriptor)}" }
        val fields = reader.fields.orEmpty().filter { Modifier.isPublic(it.modifiers) }.map { "field ${String(it.name)} ${String(it.typeName)}" }
        return fields + methods
    }

    private fun reader(stubs: StubLibrary, internal: String) =
        ClassFileReader(stubs.classBytes(internal)!!, "$internal.class".toCharArray())

    private fun visible(modifiers: Int) = Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)

    private fun findMethod(type: Class<*>, name: String, params: List<Class<*>>): Any? {
        var c: Class<*>? = type
        while (c != null) {
            try {
                return c.getDeclaredMethod(name, *params.toTypedArray())
            } catch (e: NoSuchMethodException) {
                c = c.superclass
            }
        }
        // Métodos de interfaz (p. ej. CharSequence) o heredados públicos.
        return type.getMethod(name, *params.toTypedArray())
    }

    private fun parameterTypes(descriptor: String, loader: ClassLoader?): List<Class<*>> {
        val params = descriptor.substring(1, descriptor.indexOf(')'))
        val result = mutableListOf<Class<*>>()
        var i = 0
        while (i < params.length) {
            var dims = 0
            while (params[i] == '[') { dims++; i++ }
            var type: Class<*> = when (params[i]) {
                'Z' -> java.lang.Boolean.TYPE
                'B' -> java.lang.Byte.TYPE
                'C' -> Character.TYPE
                'S' -> java.lang.Short.TYPE
                'I' -> Integer.TYPE
                'J' -> java.lang.Long.TYPE
                'F' -> java.lang.Float.TYPE
                'D' -> java.lang.Double.TYPE
                'L' -> {
                    val end = params.indexOf(';', i)
                    val cls = Class.forName(params.substring(i + 1, end).replace('/', '.'), false, loader)
                    i = end
                    cls
                }
                else -> error("Descriptor no válido: $descriptor")
            }
            repeat(dims) { type = java.lang.reflect.Array.newInstance(type, 0).javaClass }
            result += type
            i++
        }
        return result
    }
}

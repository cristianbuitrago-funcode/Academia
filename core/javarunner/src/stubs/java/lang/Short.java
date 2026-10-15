// Biblioteca de clases PERMITIDAS para los programas de los estudiantes (Fase 8).
// Solo son declaraciones: el compilador las usa para saber qué existe. En ejecución
// se usan las clases reales de Android/Java. Cada método debe existir en Android 8.0
// (API 26) y en Java 17; lo comprueba StubConformanceTest.
// Ampliar esta lista es una decisión de seguridad: no añadir nada que dé acceso a
// clases (Class, ClassLoader), reflexión, hilos, archivos, red, procesos ni System.exit.
package java.lang;

public final class Short extends Number implements Comparable<Short> { public Short(short value) {} public static Short valueOf(short s) { return null; } public short shortValue() { return 0; } public int intValue() { return 0; } public long longValue() { return 0; } public float floatValue() { return 0; } public double doubleValue() { return 0; } public int compareTo(Short other) { return 0; } }

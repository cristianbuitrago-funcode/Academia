// Biblioteca de clases PERMITIDAS para los programas de los estudiantes (Fase 8).
// Solo son declaraciones: el compilador las usa para saber qué existe. En ejecución
// se usan las clases reales de Android/Java. Cada método debe existir en Android 8.0
// (API 26) y en Java 17; lo comprueba StubConformanceTest.
// Ampliar esta lista es una decisión de seguridad: no añadir nada que dé acceso a
// clases (Class, ClassLoader), reflexión, hilos, archivos, red, procesos ni System.exit.
package java.lang;

public final class Long extends Number implements Comparable<Long> {
  public static final long MAX_VALUE = 0x7fffffffffffffffL;
  public static final long MIN_VALUE = 0x8000000000000000L;
  public Long(long value) {}
  public static Long valueOf(long l) { return null; }
  public static long parseLong(String s) { return 0; }
  public static String toString(long l) { return null; }
  public int intValue() { return 0; }
  public long longValue() { return 0; }
  public float floatValue() { return 0; }
  public double doubleValue() { return 0; }
  public int compareTo(Long other) { return 0; }
  public boolean equals(Object o) { return false; }
  public int hashCode() { return 0; }
  public String toString() { return null; }
}

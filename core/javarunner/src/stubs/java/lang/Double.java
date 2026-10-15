// Biblioteca de clases PERMITIDAS para los programas de los estudiantes (Fase 8).
// Solo son declaraciones: el compilador las usa para saber qué existe. En ejecución
// se usan las clases reales de Android/Java. Cada método debe existir en Android 8.0
// (API 26) y en Java 17; lo comprueba StubConformanceTest.
// Ampliar esta lista es una decisión de seguridad: no añadir nada que dé acceso a
// clases (Class, ClassLoader), reflexión, hilos, archivos, red, procesos ni System.exit.
package java.lang;

public final class Double extends Number implements Comparable<Double> {
  public Double(double value) {}
  public static Double valueOf(double d) { return null; }
  public static double parseDouble(String s) { return 0; }
  public static String toString(double d) { return null; }
  public static boolean isNaN(double v) { return false; }
  public int intValue() { return 0; }
  public long longValue() { return 0; }
  public float floatValue() { return 0; }
  public double doubleValue() { return 0; }
  public int compareTo(Double other) { return 0; }
  public boolean equals(Object o) { return false; }
  public int hashCode() { return 0; }
  public String toString() { return null; }
}

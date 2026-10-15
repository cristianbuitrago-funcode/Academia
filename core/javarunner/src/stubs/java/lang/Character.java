// Biblioteca de clases PERMITIDAS para los programas de los estudiantes (Fase 8).
// Solo son declaraciones: el compilador las usa para saber qué existe. En ejecución
// se usan las clases reales de Android/Java. Cada método debe existir en Android 8.0
// (API 26) y en Java 17; lo comprueba StubConformanceTest.
// Ampliar esta lista es una decisión de seguridad: no añadir nada que dé acceso a
// clases (Class, ClassLoader), reflexión, hilos, archivos, red, procesos ni System.exit.
package java.lang;

public final class Character implements java.io.Serializable, Comparable<Character> {
  public Character(char value) {}
  public static Character valueOf(char c) { return null; }
  public char charValue() { return 0; }
  public static boolean isDigit(char ch) { return false; }
  public static boolean isLetter(char ch) { return false; }
  public static boolean isLetterOrDigit(char ch) { return false; }
  public static boolean isUpperCase(char ch) { return false; }
  public static boolean isLowerCase(char ch) { return false; }
  public static boolean isWhitespace(char ch) { return false; }
  public static char toUpperCase(char ch) { return 0; }
  public static char toLowerCase(char ch) { return 0; }
  public static int getNumericValue(char ch) { return 0; }
  public int compareTo(Character other) { return 0; }
  public boolean equals(Object o) { return false; }
  public int hashCode() { return 0; }
  public String toString() { return null; }
}

// Biblioteca de clases PERMITIDAS para los programas de los estudiantes (Fase 8).
// Solo son declaraciones: el compilador las usa para saber qué existe. En ejecución
// se usan las clases reales de Android/Java. Cada método debe existir en Android 8.0
// (API 26) y en Java 17; lo comprueba StubConformanceTest.
// Ampliar esta lista es una decisión de seguridad: no añadir nada que dé acceso a
// clases (Class, ClassLoader), reflexión, hilos, archivos, red, procesos ni System.exit.
package java.lang;

public final class String implements java.io.Serializable, Comparable<String>, CharSequence {
  public String() {}
  public String(char[] value) {}
  public int length() { return 0; }
  public boolean isEmpty() { return false; }
  public char charAt(int index) { return 0; }
  public CharSequence subSequence(int start, int end) { return null; }
  public String substring(int beginIndex) { return null; }
  public String substring(int beginIndex, int endIndex) { return null; }
  public int indexOf(String str) { return 0; }
  public int indexOf(int ch) { return 0; }
  public int lastIndexOf(String str) { return 0; }
  public boolean contains(CharSequence s) { return false; }
  public boolean startsWith(String prefix) { return false; }
  public boolean endsWith(String suffix) { return false; }
  public boolean equals(Object o) { return false; }
  public boolean equalsIgnoreCase(String other) { return false; }
  public int hashCode() { return 0; }
  public int compareTo(String other) { return 0; }
  public String toUpperCase() { return null; }
  public String toLowerCase() { return null; }
  public String trim() { return null; }
  public String concat(String str) { return null; }
  public String replace(char oldChar, char newChar) { return null; }
  public String replace(CharSequence target, CharSequence replacement) { return null; }
  public char[] toCharArray() { return null; }
  public String toString() { return null; }
  public static String valueOf(Object obj) { return null; }
  public static String valueOf(int i) { return null; }
  public static String valueOf(long l) { return null; }
  public static String valueOf(double d) { return null; }
  public static String valueOf(char c) { return null; }
  public static String valueOf(boolean b) { return null; }
  public static String valueOf(char[] data) { return null; }
}

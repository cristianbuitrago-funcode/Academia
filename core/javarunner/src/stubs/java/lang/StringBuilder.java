// Biblioteca de clases PERMITIDAS para los programas de los estudiantes (Fase 8).
// Solo son declaraciones: el compilador las usa para saber qué existe. En ejecución
// se usan las clases reales de Android/Java. Cada método debe existir en Android 8.0
// (API 26) y en Java 17; lo comprueba StubConformanceTest.
// Ampliar esta lista es una decisión de seguridad: no añadir nada que dé acceso a
// clases (Class, ClassLoader), reflexión, hilos, archivos, red, procesos ni System.exit.
package java.lang;

public final class StringBuilder implements java.io.Serializable, CharSequence {
  public StringBuilder() {}
  public StringBuilder(String str) {}
  public StringBuilder append(String s) { return this; }
  public StringBuilder append(Object o) { return this; }
  public StringBuilder append(CharSequence s) { return this; }
  public StringBuilder append(char[] s) { return this; }
  public StringBuilder append(char c) { return this; }
  public StringBuilder append(int i) { return this; }
  public StringBuilder append(long l) { return this; }
  public StringBuilder append(float f) { return this; }
  public StringBuilder append(double d) { return this; }
  public StringBuilder append(boolean b) { return this; }
  public StringBuilder insert(int offset, String s) { return this; }
  public StringBuilder reverse() { return this; }
  public StringBuilder deleteCharAt(int index) { return this; }
  public void setCharAt(int index, char ch) {}
  public int length() { return 0; }
  public char charAt(int index) { return 0; }
  public CharSequence subSequence(int start, int end) { return null; }
  public String toString() { return null; }
}

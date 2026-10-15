// Biblioteca de clases PERMITIDAS para los programas de los estudiantes (Fase 8).
// Solo son declaraciones: el compilador las usa para saber qué existe. En ejecución
// se usan las clases reales de Android/Java. Cada método debe existir en Android 8.0
// (API 26) y en Java 17; lo comprueba StubConformanceTest.
// Ampliar esta lista es una decisión de seguridad: no añadir nada que dé acceso a
// clases (Class, ClassLoader), reflexión, hilos, archivos, red, procesos ni System.exit.
package java.io;

public class PrintStream {
  private PrintStream() {}
  public void print(String s) {}
  public void print(Object o) {}
  public void print(char c) {}
  public void print(char[] s) {}
  public void print(int i) {}
  public void print(long l) {}
  public void print(float f) {}
  public void print(double d) {}
  public void print(boolean b) {}
  public void println() {}
  public void println(String s) {}
  public void println(Object o) {}
  public void println(char c) {}
  public void println(char[] s) {}
  public void println(int i) {}
  public void println(long l) {}
  public void println(float f) {}
  public void println(double d) {}
  public void println(boolean b) {}
}

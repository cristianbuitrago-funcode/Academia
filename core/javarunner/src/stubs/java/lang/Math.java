// Biblioteca de clases PERMITIDAS para los programas de los estudiantes (Fase 8).
// Solo son declaraciones: el compilador las usa para saber qué existe. En ejecución
// se usan las clases reales de Android/Java. Cada método debe existir en Android 8.0
// (API 26) y en Java 17; lo comprueba StubConformanceTest.
// Ampliar esta lista es una decisión de seguridad: no añadir nada que dé acceso a
// clases (Class, ClassLoader), reflexión, hilos, archivos, red, procesos ni System.exit.
package java.lang;

public final class Math {
  private Math() {}
  public static final double PI = 3.141592653589793;
  public static final double E = 2.718281828459045;
  public static int abs(int a) { return 0; }
  public static long abs(long a) { return 0; }
  public static double abs(double a) { return 0; }
  public static int max(int a, int b) { return 0; }
  public static long max(long a, long b) { return 0; }
  public static double max(double a, double b) { return 0; }
  public static int min(int a, int b) { return 0; }
  public static long min(long a, long b) { return 0; }
  public static double min(double a, double b) { return 0; }
  public static double pow(double a, double b) { return 0; }
  public static double sqrt(double a) { return 0; }
  public static double floor(double a) { return 0; }
  public static double ceil(double a) { return 0; }
  public static long round(double a) { return 0; }
  public static int floorMod(int x, int y) { return 0; }
  public static int floorDiv(int x, int y) { return 0; }
}

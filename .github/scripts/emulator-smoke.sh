#!/usr/bin/env bash
# Prueba de humo en un emulador Android real (la ejecuta el job "emulator" del CI).
# 1) Instala el APK de depuración y lo abre como lo haría una persona.
# 2) Comprueba que el proceso sigue vivo pasados unos segundos.
# 3) Ejecuta las pruebas instrumentadas (LaunchSmokeTest).
# Siempre guarda el logcat y muestra cualquier "FATAL EXCEPTION" de la app.
set -u
PKG=com.funcode.academy
status=0

echo "## Dispositivo"
echo "Android $(adb shell getprop ro.build.version.release | tr -d '\r') (API $(adb shell getprop ro.build.version.sdk | tr -d '\r')), modelo $(adb shell getprop ro.product.model | tr -d '\r'), ABI $(adb shell getprop ro.product.cpu.abi | tr -d '\r'), CPU $(adb shell nproc | tr -d '\r') núcleos, $(adb shell grep MemTotal /proc/meminfo | tr -d '\r'), heapgrowthlimit $(adb shell getprop dalvik.vm.heapgrowthlimit | tr -d '\r')"

adb logcat -c
adb install -r app/build/outputs/apk/debug/app-debug.apk || status=1
adb shell am start -W -n "$PKG/.MainActivity" || status=1
sleep 20
if adb shell pidof "$PKG" >/dev/null; then
  echo "La app sigue abierta 20 s después de iniciarse."
else
  echo "::error::La app se cerró al abrirla."
  status=1
fi
adb shell screencap -p /sdcard/inicio.png && adb pull /sdcard/inicio.png emulator-inicio.png || true

# Gradle instala sus propios APK: se desinstala antes la copia instalada arriba
# (si no, falla por firma distinta y Gradle no ejecuta ninguna prueba).
adb uninstall "$PKG" || true
./gradlew :app:connectedDebugAndroidTest || status=1
# Gradle puede terminar "BUILD SUCCESSFUL" sin ejecutar pruebas: se exige evidencia.
python3 - <<'PY' || status=1
import glob, sys, xml.etree.ElementTree as ET
# En este job no se ejecutan pruebas JVM: todos los TEST-*.xml son del emulador.
files = [f for f in glob.glob("app/build/**/TEST-*.xml", recursive=True) if "UnitTest" not in f]
tests = sum(int(ET.parse(f).getroot().get("tests", 0)) for f in files)
bad = sum(int(ET.parse(f).getroot().get(k, 0)) for f in files for k in ("failures", "errors"))
skipped = sum(int(ET.parse(f).getroot().get("skipped", 0)) for f in files)
print(f"Pruebas instrumentadas: {tests} ejecutadas, {bad} con fallo o error, {skipped} omitidas ({len(files)} informes)")
for f in files:
    for case in ET.parse(f).getroot().iter("testcase"):
        for kind in ("failure", "error", "skipped"):
            node = case.find(kind)
            if node is not None:
                text = (node.get("message") or node.text or "").strip().splitlines()
                print(f"  [{kind}] {case.get('classname')}.{case.get('name')}: {text[0][:300] if text else ''}")
if tests == 0 or bad:
    print("::error::Las pruebas instrumentadas no se ejecutaron o fallaron.")
    sys.exit(1)
PY

adb logcat -d > emulator-logcat.txt || true
echo "## Medidas y observaciones del prototipo (Fase 8)"
grep "FuncodeMedidas" emulator-logcat.txt || echo "(sin medidas)"
grep -E "FuncodeRunner" emulator-logcat.txt | head -40 || true
if grep -q "FATAL EXCEPTION" emulator-logcat.txt; then
  echo "::error::El logcat contiene un cierre inesperado:"
  grep -A40 "FATAL EXCEPTION" emulator-logcat.txt
  status=1
fi
exit $status

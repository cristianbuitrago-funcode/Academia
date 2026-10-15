#!/usr/bin/env bash
# Prueba de humo en un emulador Android real (la ejecuta el job "emulator" del CI).
# 1) Instala el APK de depuración y lo abre como lo haría una persona.
# 2) Comprueba que el proceso sigue vivo pasados unos segundos.
# 3) Ejecuta las pruebas instrumentadas (LaunchSmokeTest).
# Siempre guarda el logcat y muestra cualquier "FATAL EXCEPTION" de la app.
set -u
PKG=com.funcode.academy
status=0

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

./gradlew :app:connectedDebugAndroidTest || status=1

adb logcat -d > emulator-logcat.txt || true
if grep -q "FATAL EXCEPTION" emulator-logcat.txt; then
  echo "::error::El logcat contiene un cierre inesperado:"
  grep -A40 "FATAL EXCEPTION" emulator-logcat.txt
  status=1
fi
exit $status

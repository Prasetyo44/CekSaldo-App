#!/usr/bin/env bash
# Dijalankan di dalam emulator-runner. Kegagalan dilaporkan sebagai anotasi GitHub (::error::)
# agar terlihat di halaman Actions tanpa harus membuka log lengkap.
set -u
status=0

annotate() { # judul, isi
  local msg
  msg=$(printf '%s' "$2" | head -c 3500 | sed ':a;N;$!ba;s/%/%25/g;s/\r/%0D/g;s/\n/%0A/g')
  echo "::error title=$1::$msg"
}

./gradlew --no-daemon connectedDebugAndroidTest > gradle-test.log 2>&1
if [ $? -ne 0 ]; then
  status=1
  failures=$(find app/build/outputs/androidTest-results -name '*.xml' -exec grep -h -A6 '<failure' {} \; 2>/dev/null)
  if [ -n "$failures" ]; then
    annotate "Tes UI gagal" "$failures"
  else
    annotate "Gradle gagal" "$(grep -A15 'What went wrong' gradle-test.log || tail -40 gradle-test.log)"
  fi
fi
tail -30 gradle-test.log

adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat -c
adb shell am start -W -n id.prasetyo.ceksaldo/.MainActivity
sleep 8
crash=$(adb logcat -d -s AndroidRuntime:E)
if ! adb shell pidof id.prasetyo.ceksaldo >/dev/null || printf '%s' "$crash" | grep -q "FATAL EXCEPTION"; then
  status=1
  annotate "APK release crash" "$crash"
else
  echo "APK release berjalan tanpa crash"
fi

exit $status

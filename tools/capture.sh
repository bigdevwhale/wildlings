#!/bin/bash
# Screenshots of the -Pdemo -Pshots build in one language, opened straight on each screen by intent extras.
# Usage: tools/capture.sh en|ru   (ANDROID_SERIAL picks the phone; the Mi 9 by default)
set -e
export ANDROID_SERIAL=${ANDROID_SERIAL:-d7a925e6} JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
cd "$(dirname "$0")/.."
L=$1; P=app.wildling.demo; OUT=docs/screenshots/$L; mkdir -p $OUT
./gradlew assembleDebug -Pdemo -Pshots -Pdemolang=$L -q 2>&1 | grep -E "^e: |FAILED" && exit 1
adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
adb shell pm clear $P >/dev/null
adb shell am start -n $P/app.wildling.MainActivity >/dev/null; sleep 25
shot() { [ "$(adb shell dumpsys telephony.registry | grep -m1 mCallState | tr -dc 0-9)" = "0" ] || { echo "call"; exit 1; }
  adb shell am start -S -n $P/app.wildling.MainActivity $2 >/dev/null; sleep ${3:-6}
  f=$(adb shell dumpsys window | grep -m1 mCurrentFocus); echo "$f" | grep -q "$P" || { echo "focus lost: $f"; exit 1; }
  adb exec-out screencap -p > $OUT/$1.png; }
shot 1-home "--ei tab 0" 8
shot 2-creature "--es open habit:1"
shot 3-craving "--es open sos"
shot 4-journal "--ei tab 1 --ei sub 0"
shot 5-map "--es open map"
shot 6-cravings "--ei tab 1 --ei sub 2"
shot 7-creatures "--ei tab 2 --ei sub 0"
shot 8-chronicle "--ei tab 3 --ei sub 0"
adb shell am force-stop $P
echo done $L

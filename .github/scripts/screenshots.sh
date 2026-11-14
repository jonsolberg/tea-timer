#!/usr/bin/env bash
# Installs the debug APK on the emulator and captures screenshots of the main screens.
set -x
mkdir -p shots
PKG=se.jonsolberg.teatimer
adb shell settings put global hide_error_dialogs 1
sleep 20
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
shot() { adb exec-out screencap -p > "shots/$1.png"; adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "shots/$1.xml" >/dev/null 2>&1; }
# tap the centre of the first view whose text (or content-desc) matches
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  local b; b=$(grep -o "<node[^>]*\(text\|content-desc\)=\"$1\"[^>]*" /tmp/ui.xml | head -1 | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+' | tr '\n' ' ')
  [ -z "$b" ] && { echo "not found: $1"; return 1; }
  set -- $b; adb shell input tap $(( ($1+$3)/2 )) $(( ($2+$4)/2 )); sleep 1.5
}
adb shell am start -n $PKG/.MainActivity; sleep 6
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS; sleep 1
shot 01-main
tap_text "Black tea"
shot 02-selected
for t in Start START Brew "Start timer"; do tap_text "$t" && break; done
sleep 4
shot 03-brewing
adb shell cmd uimode night yes; sleep 3
shot 04-brewing-dark
adb shell am force-stop $PKG
adb shell am start -n $PKG/.MainActivity; sleep 5
shot 05-main-dark
adb shell cmd uimode night no
adb logcat -d | grep -E "AndroidRuntime|FATAL" > shots/crashes.txt || true
exit 0

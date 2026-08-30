#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
pkg=dev.hloth.zaragoza_tarjeta_bus
remote="/sdcard/Android/media/$pkg/screenshots"
adb="${ANDROID_HOME:?ANDROID_HOME is not set}/platform-tools/adb"

[ "$("$adb" devices | grep -c $'\tdevice$')" -eq 1 ] || {
	echo "need exactly one running device; start one with:" >&2
	echo "  \$ANDROID_HOME/emulator/emulator -avd <name> &" >&2
	"$adb" devices >&2
	exit 1
}

cd "$root"
./gradlew -q :app:assembleDebug :app:assembleDebugAndroidTest

"$adb" install -r -t app/build/outputs/apk/debug/app-debug.apk >/dev/null
"$adb" install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk >/dev/null

"$adb" shell svc power stayon true >/dev/null
"$adb" shell input keyevent KEYCODE_WAKEUP >/dev/null
"$adb" shell wm dismiss-keyguard >/dev/null
for scale in window_animation_scale transition_animation_scale animator_duration_scale; do
	"$adb" shell settings put global "$scale" 0 >/dev/null
done

"$adb" shell rm -rf "$remote"
"$adb" shell am instrument -w \
	-e class "$pkg.StoreScreenshots" \
	-e additionalTestOutputDir "$remote" \
	"$pkg.test/androidx.test.runner.AndroidJUnitRunner"

staging="$(mktemp -d)"
trap 'rm -rf "$staging"' EXIT
"$adb" pull -a "$remote" "$staging" >/dev/null

shopt -s nullglob
captured=("$staging"/screenshots/*.png)
[ "${#captured[@]}" -gt 0 ] || { echo "no screenshots pulled from $remote" >&2; exit 1; }

for png in "${captured[@]}"; do
	name="$(basename "$png" .png)"
	locale="${name%-*}"
	index="${name##*-}"
	[ -d "$root/fastlane/metadata/android/$locale" ] || {
		echo "no fastlane locale for $locale" >&2; exit 1; }
	target="$root/fastlane/metadata/android/$locale/images/phoneScreenshots"
	mkdir -p "$target"
	cp "$png" "$target/$index.png"
done

[ "${#captured[@]}" -eq 20 ] || {
	echo "expected 20 screenshots, got ${#captured[@]}" >&2; exit 1; }

echo "wrote ${#captured[@]} screenshots across $(( ${#captured[@]} / 2 )) locales"

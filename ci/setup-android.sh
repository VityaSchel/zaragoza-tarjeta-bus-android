#!/usr/bin/env bash
set -euo pipefail

JDK_RELEASE="jdk-21.0.12.1+1"
JDK_ARCHIVE="OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz"
JDK_SHA256="ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94"
CMDLINE_TOOLS_ARCHIVE="commandlinetools-linux-13114758_latest.zip"
CMDLINE_TOOLS_SHA256="7ec965280a073311c339e571cd5de778b9975026cfcbe79f2b1cdcb1e15317ee"
GRADLE_WRAPPER_SHA256="7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d"
ANDROID_PLATFORM="platforms;android-37.1"
ANDROID_BUILD_TOOLS="build-tools;37.0.0"

: "${JAVA_HOME:?}" "${ANDROID_HOME:?}"

missing=()
command -v curl >/dev/null 2>&1 || missing+=(curl ca-certificates)
command -v unzip >/dev/null 2>&1 || missing+=(unzip)
if [ ${#missing[@]} -gt 0 ]; then
	export DEBIAN_FRONTEND=noninteractive
	apt-get update -y
	apt-get install -y --no-install-recommends "${missing[@]}"
fi

curl -fsSLo /tmp/jdk.tar.gz \
	"https://github.com/adoptium/temurin21-binaries/releases/download/${JDK_RELEASE//+/%2B}/${JDK_ARCHIVE}"
echo "${JDK_SHA256}  /tmp/jdk.tar.gz" | sha256sum -c -
rm -rf "$JAVA_HOME"
mkdir -p "$JAVA_HOME"
tar xzf /tmp/jdk.tar.gz -C "$JAVA_HOME" --strip-components=1
export PATH="$JAVA_HOME/bin:$PATH"
java -version

curl -fsSLo /tmp/cmdline-tools.zip \
	"https://dl.google.com/android/repository/${CMDLINE_TOOLS_ARCHIVE}"
echo "${CMDLINE_TOOLS_SHA256}  /tmp/cmdline-tools.zip" | sha256sum -c -
rm -rf "$ANDROID_HOME"
mkdir -p "$ANDROID_HOME/cmdline-tools"
unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools"
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"

sdkmanager="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
printf 'y\n%.0s' $(seq 1 50) | "$sdkmanager" --licenses >/dev/null
"$sdkmanager" --install "$ANDROID_PLATFORM" "$ANDROID_BUILD_TOOLS" >/dev/null

echo "${GRADLE_WRAPPER_SHA256}  gradle/wrapper/gradle-wrapper.jar" | sha256sum -c -

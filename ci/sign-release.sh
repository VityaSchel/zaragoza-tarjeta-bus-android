#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
unsigned="${1:?usage: ci/sign-release.sh <unsigned.apk> [signed.apk]}"
signed="${2:-${unsigned%-unsigned.apk}.apk}"
properties="$root/keystore.properties"

[ -f "$unsigned" ] || { echo "no such file: $unsigned" >&2; exit 1; }
[ -f "$properties" ] || { echo "no keystore.properties at $properties" >&2; exit 1; }

sdk="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
tools="$(find "$sdk/build-tools" -maxdepth 1 -mindepth 1 -type d | sort -V | tail -1)"
[ -n "$tools" ] || { echo "no build-tools under $sdk" >&2; exit 1; }

"$tools/zipalign" -c -P 16 -v 4 "$unsigned" >/dev/null \
	|| { echo "$unsigned is not aligned; apksigner does not align, so it must be zipaligned first" >&2; exit 1; }

property() {
	local value
	value="$(sed -n "s/^$1=//p" "$properties" | head -1 | tr -d '\r')"
	[ -n "$value" ] || { echo "keystore.properties has no $1" >&2; exit 1; }
	printf '%s' "$value"
}

KS_PASS="$(property storePassword)" KEY_PASS="$(property keyPassword)" \
	"$tools/apksigner" sign \
	--ks "$(property storeFile)" \
	--ks-key-alias "$(property keyAlias)" \
	--ks-pass env:KS_PASS \
	--key-pass env:KEY_PASS \
	--alignment-preserved \
	--out "$signed" \
	"$unsigned"

"$tools/apksigner" verify --print-certs "$signed" \
	| grep "certificate SHA-256 digest" \
	|| { echo "FATAL: the signed APK reports no certificate" >&2; exit 1; }


run_apksigcopier() {
	if type -P apksigcopier >/dev/null 2>&1; then
		apksigcopier "$@"
	elif command -v uv >/dev/null 2>&1; then
		uv run --quiet --with apksigcopier apksigcopier "$@"
	else
		echo "FATAL: need apksigcopier (pip install apksigcopier) to verify the signature" >&2
		exit 1
	fi
}

transplanted="$(mktemp -d)/$(basename "$signed")"
trap 'rm -rf "$(dirname "$transplanted")"' EXIT

run_apksigcopier copy "$signed" "$unsigned" "$transplanted" \
	|| { echo "FATAL: the signature cannot be transplanted onto an unsigned rebuild" >&2; exit 1; }
cmp "$transplanted" "$signed" \
	|| { echo "FATAL: transplanting the signature does not reproduce the signed APK" >&2; exit 1; }

echo "signed $signed; its signature transplants back onto $(basename "$unsigned")"

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
	--out "$signed" \
	"$unsigned"

"$tools/apksigner" verify --print-certs "$signed" \
	| grep "certificate SHA-256 digest" \
	|| { echo "FATAL: the signed APK reports no certificate" >&2; exit 1; }

contents() {
	unzip -Z1 "$1" \
		| grep -vE '^META-INF/(MANIFEST\.MF|[^/]+\.(SF|RSA|EC|DSA))$' \
		| sort \
		| while IFS= read -r entry; do
			printf '%s  %s\n' \
				"$(unzip -p "$1" "$entry" | shasum -a 256 | cut -c1-64)" "$entry"
		done
}

diff <(contents "$unsigned") <(contents "$signed") >/dev/null \
	|| { echo "FATAL: signing altered the contents, not just the signature" >&2; exit 1; }

echo "signed $signed; contents still reproduce from $(basename "$unsigned")"

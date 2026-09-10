#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
key="$root/play-service-account.json"
metadata="$root/build/play-metadata/android"
package=dev.hloth.zaragoza_tarjeta_bus

[ -f "$key" ] || {
	echo "missing $key" >&2
	exit 1
}

validate_only=false
[ "${1:-}" = "--validate" ] && validate_only=true

"$root/ci/play-metadata.sh" >/dev/null

fastlane supply \
	--package_name "$package" \
	--json_key "$key" \
	--metadata_path "$metadata" \
	--skip_upload_apk true \
	--skip_upload_aab true \
	--skip_upload_changelogs true \
	--skip_upload_images true \
	--skip_upload_screenshots true \
	--validate_only "$validate_only"

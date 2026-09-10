#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source_dir="$root/fastlane/metadata/android"
out_dir="$root/build/play-metadata/android"
repo=https://git.hloth.dev/hloth/zaragoza-tarjeta-bus-android

open_source_label() {
	case "$1" in
		ar) echo "مفتوح المصدر" ;;
		be) echo "Адкрыты зыходны код" ;;
		ca) echo "Codi obert" ;;
		de-DE) echo "Open Source" ;;
		en-US) echo "Open source" ;;
		es-ES) echo "Código abierto" ;;
		fr-FR) echo "Code source ouvert" ;;
		ro) echo "Sursă deschisă" ;;
		ru-RU) echo "Открытый исходный код" ;;
		uk) echo "Відкритий вихідний код" ;;
		*) return 1 ;;
	esac
}

rm -rf "$out_dir"
mkdir -p "$(dirname "$out_dir")"
cp -R "$source_dir" "$out_dir"

for dir in "$out_dir"/*/; do
	locale="$(basename "$dir")"
	label="$(open_source_label "$locale")" || {
		echo "no open source label for locale $locale; add one to ci/play-metadata.sh" >&2
		exit 1
	}
	printf '\n%s: %s\n' "$label" "$repo" >> "$dir/full_description.txt"
done

echo "Play listing metadata written to $out_dir"
echo "upload with: fastlane supply --metadata_path $out_dir --skip_upload_apk --skip_upload_aab"

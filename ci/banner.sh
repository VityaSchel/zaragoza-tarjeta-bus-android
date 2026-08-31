#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
logo="$root/contrib/logo-no-bg.png"
metadata="$root/fastlane/metadata/android"

banner_width=1024
banner_height=500
background='#B3E595'
shot_height=480
logo_size=188
gap=14
corner_radius=12
shadow_color='rgba(0, 0, 0, 0.25)'
shadow_sigma=5
shadow_offset_y=10
shadow_pad=$(( shadow_sigma * 4 ))

command -v magick >/dev/null || {
	echo "need ImageMagick; brew install imagemagick" >&2; exit 1; }
[ -f "$logo" ] || { echo "missing $logo" >&2; exit 1; }

is_rtl() {
	case "${1%%-*}" in
		ar | fa | he | iw | ur) return 0 ;;
		*) return 1 ;;
	esac
}

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

magick "$logo" -resize "${logo_size}x${logo_size}" \
	-background none -gravity center -extent "${logo_size}x${logo_size}" \
	"$work/logo.png"

rounded() {
	local src="$1" out="$2" width="$3"
	local last_x=$(( width - 1 )) last_y=$(( shot_height - 1 ))

	magick "$src" -resize "${width}x${shot_height}!" \
		\( -size "${width}x${shot_height}" xc:none \
			-fill white -draw "roundrectangle 0,0,$last_x,$last_y,$corner_radius,$corner_radius" \) \
		-alpha set -compose DstIn -composite "$out"
}

shadow_for() {
	local out="$1" width="$2"
	local far_x=$(( shadow_pad + width - 1 )) far_y=$(( shadow_pad + shot_height - 1 ))

	magick -size "$(( width + shadow_pad * 2 ))x$(( shot_height + shadow_pad * 2 ))" xc:none \
		-fill "$shadow_color" \
		-draw "roundrectangle $shadow_pad,$shadow_pad,$far_x,$far_y,$corner_radius,$corner_radius" \
		-alpha set -channel RGBA -blur "0x$shadow_sigma" +channel "$out"
}

built=0
for dir in "$metadata"/*/; do
	dir="${dir%/}"
	locale="$(basename "$dir")"
	shots="$dir/images/phoneScreenshots"
	first="$shots/1.png"
	second="$shots/2.png"
	[ -f "$first" ] && [ -f "$second" ] || {
		echo "skipping $locale: needs both 1.png and 2.png" >&2; continue; }

	first_width=$(magick identify -format "%[fx:round(w*$shot_height/h)]" "$first")
	second_width=$(magick identify -format "%[fx:round(w*$shot_height/h)]" "$second")

	rounded "$first" "$work/first.png" "$first_width"
	rounded "$second" "$work/second.png" "$second_width"
	shadow_for "$work/first-shadow.png" "$first_width"
	shadow_for "$work/second-shadow.png" "$second_width"

	if is_rtl "$locale"; then
		order=("$work/second.png:$second_width" "$work/logo.png:$logo_size" "$work/first.png:$first_width")
		shadows=("$work/second-shadow.png" "" "$work/first-shadow.png")
	else
		order=("$work/first.png:$first_width" "$work/logo.png:$logo_size" "$work/second.png:$second_width")
		shadows=("$work/first-shadow.png" "" "$work/second-shadow.png")
	fi

	row_width=$(( first_width + second_width + logo_size + gap * 2 ))
	[ "$row_width" -le "$banner_width" ] || {
		echo "$locale: row is ${row_width}px, wider than the ${banner_width}px banner" >&2; exit 1; }

	args=()
	x=$(( (banner_width - row_width) / 2 ))
	for i in "${!order[@]}"; do
		image="${order[$i]%:*}"
		width="${order[$i]##*:}"
		height=$shot_height
		[ -n "${shadows[$i]}" ] || height=$logo_size
		y=$(( (banner_height - height) / 2 ))

		if [ -n "${shadows[$i]}" ]; then
			args+=("${shadows[$i]}" -geometry
				"+$(( x - shadow_pad ))+$(( y - shadow_pad + shadow_offset_y ))" -composite)
		fi
		args+=("$image" -geometry "+$x+$y" -composite)
		x=$(( x + width + gap ))
	done

	magick -size "${banner_width}x${banner_height}" "xc:$background" \
		"${args[@]}" \
		-alpha remove -alpha off -strip "PNG24:$dir/images/featureGraphic.png"
	"$root/ci/tag-srgb.sh" "$dir/images/featureGraphic.png"
	built=$(( built + 1 ))
done

echo "built $built feature graphics"

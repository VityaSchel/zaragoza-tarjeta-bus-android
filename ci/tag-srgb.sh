#!/usr/bin/env bash
set -euo pipefail

# ImageMagick writes cHRM instead of an sRGB chunk
for png in "$@"; do
	tmp="$(mktemp)"
	{
		head -c 33 "$png"
		printf '\000\000\000\001sRGB\000\256\316\034\351'
		tail -c +34 "$png"
	} > "$tmp"
	mv "$tmp" "$png"
done

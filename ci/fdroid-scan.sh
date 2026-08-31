#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

git -C "$root" ls-files | while IFS= read -r file; do
	mkdir -p "$work/tree/$(dirname "$file")"
	cp "$root/$file" "$work/tree/$file"
done

cat > "$work/scan.py" <<'PY'
import sys

from fdroidserver import scanner

problems = scanner.scan_source(sys.argv[1])
if problems:
    print(f"FATAL: F-Droid's scanner reported {problems} problem(s)", file=sys.stderr)
    sys.exit(1)
print("F-Droid's scanner reports no problems")
PY

if python3 -c "import fdroidserver" >/dev/null 2>&1; then
	python3 "$work/scan.py" "$work/tree"
elif command -v uv >/dev/null 2>&1; then
	uv run --quiet --with fdroidserver python "$work/scan.py" "$work/tree"
else
	echo "FATAL: need fdroidserver (apt-get install fdroidserver)" >&2
	exit 1
fi

#!/usr/bin/env bash
set -euo pipefail

NODE_VERSION="20.20.2"
NODE_SHA256="df770b2a6f130ed8627c9782c988fda9669fa23898329a61a871e32f965e007d"
NODE_HOME="${NODE_HOME:-/opt/node}"

if command -v node >/dev/null 2>&1; then
	node --version
	exit 0
fi

missing=()
command -v curl >/dev/null 2>&1 || missing+=(curl ca-certificates)
command -v xz >/dev/null 2>&1 || missing+=(xz-utils)
if [ ${#missing[@]} -gt 0 ]; then
	export DEBIAN_FRONTEND=noninteractive
	apt-get update -y
	apt-get install -y --no-install-recommends "${missing[@]}"
fi

archive="node-v${NODE_VERSION}-linux-x64.tar.xz"
curl -fsSLo "/tmp/$archive" "https://nodejs.org/dist/v${NODE_VERSION}/${archive}"
echo "${NODE_SHA256}  /tmp/$archive" | sha256sum -c -
rm -rf "$NODE_HOME"
mkdir -p "$NODE_HOME"
tar xJf "/tmp/$archive" -C "$NODE_HOME" --strip-components=1

mkdir -p /usr/local/bin
ln -sf "$NODE_HOME/bin/node" /usr/local/bin/node
command -v node >/dev/null || {
	echo "installed node at $NODE_HOME but /usr/local/bin is not on PATH" >&2
	exit 1
}
node --version

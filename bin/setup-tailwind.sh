#!/usr/bin/env bash
set -euo pipefail

BIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="$BIN_DIR/tailwindcss"

if [ -x "$TARGET" ]; then
    echo "tailwindcss already installed at $TARGET"
    exit 0
fi

OS="$(uname -s)"
ARCH="$(uname -m)"

case "$OS" in
    Linux) PLATFORM="linux" ;;
    Darwin) PLATFORM="macos" ;;
    *) echo "Unsupported OS: $OS" >&2; exit 1 ;;
esac

case "$ARCH" in
    x86_64|amd64) SUFFIX="x64" ;;
    arm64|aarch64) SUFFIX="arm64" ;;
    *) echo "Unsupported architecture: $ARCH" >&2; exit 1 ;;
esac

ASSET="tailwindcss-${PLATFORM}-${SUFFIX}"
URL="https://github.com/tailwindlabs/tailwindcss/releases/latest/download/${ASSET}"

echo "Downloading $ASSET..."
curl -fsSL "$URL" -o "$TARGET"
chmod +x "$TARGET"
echo "Installed tailwindcss to $TARGET"
"$TARGET" --version

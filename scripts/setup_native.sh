#!/usr/bin/env bash
# Fetches llama.cpp into app/src/main/cpp/llama.cpp so the native inference
# backend gets compiled. Without this, the app still builds in STUB mode
# (UI works, no real inference). Re-run to update.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$REPO_ROOT/app/src/main/cpp/llama.cpp"
LLAMA_TAG="${LLAMA_TAG:-master}"

if [ -d "$DEST/.git" ]; then
  echo "Updating existing llama.cpp checkout at $DEST"
  git -C "$DEST" fetch --depth 1 origin "$LLAMA_TAG"
  git -C "$DEST" checkout FETCH_HEAD
else
  echo "Cloning llama.cpp ($LLAMA_TAG) into $DEST"
  git clone --depth 1 --branch "$LLAMA_TAG" \
    https://github.com/ggerganov/llama.cpp.git "$DEST" 2>/dev/null \
    || git clone --depth 1 https://github.com/ggerganov/llama.cpp.git "$DEST"
fi

echo "Done. Rebuild the app to compile the native backend."

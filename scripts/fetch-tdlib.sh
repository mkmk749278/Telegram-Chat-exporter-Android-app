#!/usr/bin/env bash
# Pull the prebuilt TDLib AAR from the GitHub Release for local development.
# Usage:
#   scripts/fetch-tdlib.sh [tag]
# If no tag is passed, reads TDLIB_RELEASE_TAG from .github/workflows/android-build.yml.
set -euo pipefail

cd "$(dirname "$0")/.."

tag="${1:-}"
if [ -z "$tag" ]; then
  tag=$(grep -E '^\s*TDLIB_RELEASE_TAG:' .github/workflows/android-build.yml | head -1 | awk '{print $2}')
fi

if [ -z "$tag" ] || [ "$tag" = "tdlib-master-PLACEHOLDER" ]; then
  echo "Error: no TDLIB_RELEASE_TAG pinned." >&2
  echo "Run the 'Build TDLib AAR' workflow on GitHub first, then either:" >&2
  echo "  - update env.TDLIB_RELEASE_TAG in .github/workflows/android-build.yml, or" >&2
  echo "  - pass the tag explicitly: scripts/fetch-tdlib.sh tdlib-master-XXXXXXX" >&2
  exit 1
fi

mkdir -p app/libs
echo "Downloading $tag/tdlib.aar"
gh release download "$tag" --pattern tdlib.aar --dir app/libs --clobber
ls -lh app/libs/tdlib.aar

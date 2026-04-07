#!/usr/bin/env bash
# Downloads the LookseeCore JAR for the version supplied as $1 (or read from
# /tmp/LOOKSEE_CORE_VERSION if no argument is provided) and installs it into
# the local Maven repo so the surrounding service build can resolve it.
#
# This script is intentionally the single source of truth for how services
# fetch LookseeCore. Each service Dockerfile copies it from the repo root.
set -euo pipefail

VERSION="${1:-}"
if [ -z "${VERSION}" ] && [ -f /tmp/LOOKSEE_CORE_VERSION ]; then
  VERSION="$(cat /tmp/LOOKSEE_CORE_VERSION)"
fi
if [ -z "${VERSION}" ]; then
  echo "usage: download-core.sh <version>" >&2
  exit 1
fi

REPO="${LOOKSEE_CORE_REPO:-deepthought42/LookseeCore}"
JAR_URL="https://github.com/${REPO}/releases/download/v${VERSION}/looksee-core-${VERSION}.jar"
DEST_DIR="${LOOKSEE_CORE_LIB_DIR:-libs}"
DEST_JAR="${DEST_DIR}/core-${VERSION}.jar"

mkdir -p "${DEST_DIR}"

echo "Downloading LookseeCore ${VERSION} from ${JAR_URL}"
curl -fsSL "${JAR_URL}" -o "${DEST_JAR}"

mvn -q install:install-file \
  -Dfile="${DEST_JAR}" \
  -DgroupId=com.looksee \
  -DartifactId=core \
  -Dversion="${VERSION}" \
  -Dpackaging=jar

echo "Installed com.looksee:core:${VERSION}"

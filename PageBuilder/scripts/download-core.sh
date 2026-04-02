#!/usr/bin/env bash
set -euo pipefail

# Download LookseeCore JAR from GitHub releases.
# Version resolution order:
#   1) first positional argument
#   2) LOOKSEE_CORE_VERSION environment variable
#   3) <looksee-core.version> from pom.xml

VERSION="${1:-${LOOKSEE_CORE_VERSION:-}}"
if [[ -z "${VERSION}" && -f "pom.xml" ]]; then
  VERSION="$(sed -n 's:.*<looksee-core.version>\(.*\)</looksee-core.version>.*:\1:p' pom.xml | head -n1)"
fi

if [[ -z "${VERSION}" ]]; then
  echo "Failed to determine LookseeCore version. Provide it as an argument or set LOOKSEE_CORE_VERSION."
  exit 1
fi

REPO="deepthought42/LookseeCore"
JAR_NAME="core-${VERSION}.jar"
DOWNLOAD_URL="https://github.com/${REPO}/releases/download/v${VERSION}/${JAR_NAME}"
LIBS_DIR="libs"
TARGET_PATH="${LIBS_DIR}/${JAR_NAME}"

mkdir -p "${LIBS_DIR}"

echo "Downloading ${JAR_NAME} from GitHub release..."
echo "URL: ${DOWNLOAD_URL}"

curl --fail --show-error --location --retry 3 --retry-delay 2 -o "${TARGET_PATH}" "${DOWNLOAD_URL}"

echo "Successfully downloaded ${JAR_NAME} to ${LIBS_DIR}/"
echo "File size: $(du -h "${TARGET_PATH}" | cut -f1)"

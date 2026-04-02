#!/bin/bash
set -euo pipefail

VERSION="0.3.13"
REPO="deepthought42/LookseeCore"
JAR_NAME="core-${VERSION}.jar"
DOWNLOAD_URL="https://github.com/${REPO}/releases/download/v${VERSION}/${JAR_NAME}"
LIBS_DIR="libs"
TARGET_FILE="${LIBS_DIR}/${JAR_NAME}"

mkdir -p "${LIBS_DIR}"

echo "Downloading ${JAR_NAME} from GitHub release..."
echo "URL: ${DOWNLOAD_URL}"

curl --fail --show-error --location --retry 5 --retry-delay 2 --retry-connrefused \
  -o "${TARGET_FILE}" "${DOWNLOAD_URL}"

if ! jar tf "${TARGET_FILE}" >/dev/null 2>&1; then
  echo "Downloaded file is not a valid JAR: ${TARGET_FILE}"
  exit 1
fi

echo "Successfully downloaded ${JAR_NAME} to ${TARGET_FILE}"
echo "File size: $(du -h "${TARGET_FILE}" | cut -f1)"

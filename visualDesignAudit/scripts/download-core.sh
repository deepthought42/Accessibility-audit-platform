#!/bin/bash

# Download LookseeCore JAR from GitHub release
# This script downloads the core-0.1.5.jar from the GitHub release

VERSION="0.3.20"
REPO="deepthought42/LookseeCore"
JAR_NAME="core-${VERSION}.jar"
DOWNLOAD_URL="https://github.com/${REPO}/releases/download/v${VERSION}/${JAR_NAME}"
LIBS_DIR="libs"

# Create libs directory if it doesn't exist
mkdir -p "${LIBS_DIR}"

echo "Downloading ${JAR_NAME} from GitHub release..."
echo "URL: ${DOWNLOAD_URL}"

# Download the JAR file
curl -L -o "${LIBS_DIR}/${JAR_NAME}" "${DOWNLOAD_URL}"

if [ $? -eq 0 ]; then
    echo "Successfully downloaded ${JAR_NAME} to ${LIBS_DIR}/"
    echo "File size: $(du -h "${LIBS_DIR}/${JAR_NAME}" | cut -f1)"
else
    echo "Failed to download ${JAR_NAME}"
    exit 1
fi 
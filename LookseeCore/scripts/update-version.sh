#!/bin/bash

NEW_VERSION=$1

# Update pom.xml with the new version
echo "🔧 Bumping version to $NEW_VERSION"
mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false

# Ensure the version is updated properly
mvn versions:commit

# Verify the version update
UPDATED_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "✅ Version updated to $UPDATED_VERSION"

# Check if the update succeeded
if [ "$UPDATED_VERSION" != "$NEW_VERSION" ]; then
  echo "❌ Version update failed!"
  exit 1
fi

# Commit the version changes to main
echo "📝 Committing version changes to main"
git add pom.xml
git commit -m "bump version"
git push origin main

echo "🎉 Version bump completed and committed to main"

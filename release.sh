#!/usr/bin/env bash
set -euo pipefail

read -rp "Release version (e.g. 3.1): " RELEASE_VERSION
read -rp "Next dev version (e.g. 3.2-SNAPSHOT): " NEXT_DEV_VERSION

if [[ -z "${RELEASE_VERSION}" || -z "${NEXT_DEV_VERSION}" ]]; then
  echo "Both version values are required."
  exit 1
fi

echo "Releasing ${RELEASE_VERSION}..."
mvn versions:set -DnewVersion="${RELEASE_VERSION}" -DgenerateBackupPoms=false
git add .
git commit -m "releasing ${RELEASE_VERSION}"
git push origin main

git tag -a "${RELEASE_VERSION}" -m "${RELEASE_VERSION}"
git push origin "${RELEASE_VERSION}"

echo "Bumping to next dev version ${NEXT_DEV_VERSION}..."
mvn versions:set -DnewVersion="${NEXT_DEV_VERSION}" -DgenerateBackupPoms=false
git add .
git commit -m "iterating to next dev version ${NEXT_DEV_VERSION}"
git push origin main

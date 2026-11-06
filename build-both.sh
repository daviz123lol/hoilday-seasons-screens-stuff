#!/usr/bin/env bash
# Builds both jars into ./dist. Needs a JDK 25 on PATH (or JAVA_HOME). First run downloads Minecraft: be patient.
set -euo pipefail
cd "$(dirname "$0")"

ATM11_NEO="26.1.2.109"     # All the Mods 11
LATEST_NEO="26.1.2.126"    # newest NeoForge 26.1.2 build when this was written: bump it when a newer one is out

rm -rf dist && mkdir -p dist
for v in "$ATM11_NEO" "$LATEST_NEO"; do
  echo "=== Building for NeoForge $v ==="
  ./gradlew clean build -Pneo_version="$v"
  cp build/libs/festivescreens-neoforge-"$v"-*.jar dist/
done
echo; echo "Done:"; ls -1 dist

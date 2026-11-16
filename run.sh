#!/usr/bin/env sh
# Build and run Oh Heck! on macOS / Linux. Requires a JDK (11+).
set -e
cd "$(dirname "$0")"

rm -rf bin
mkdir -p bin
javac -d bin -cp "libraries/*" $(find src -name '*.java')
java -cp "bin:resources:libraries/*" game.main.Main

#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
KOTLINC="${KOTLINC:-kotlinc}"
mkdir -p core/build/checks
"$KOTLINC" core/src/main/kotlin/ko/bura/core/*.kt core/src/test/kotlin/ko/bura/core/*.kt -include-runtime -d core/build/checks/checks.jar
java -jar core/build/checks/checks.jar

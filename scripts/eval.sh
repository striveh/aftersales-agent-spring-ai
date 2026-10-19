#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -B -pl aftersales-eval -am package -DskipTests
java -jar aftersales-eval/target/aftersales-eval-0.1.0-exec.jar "$@"

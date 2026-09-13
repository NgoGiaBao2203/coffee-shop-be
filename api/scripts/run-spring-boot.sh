#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

PROFILE="${1:-dev}"

cd "$PROJECT_DIR"

echo "Starting Spring Boot with profile: $PROFILE"
./mvnw spring-boot:run -Dspring-boot.run.profiles="$PROFILE"

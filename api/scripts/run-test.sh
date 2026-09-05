#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo "Starting database..."
docker compose up -d --wait

echo "Cleaning project..."
./mvnw clean

echo "Running tests..."
./mvnw test

echo "Tests completed."

echo "Opening JaCoCo coverage report..."
REPORT_PATH="$PROJECT_DIR/target/site/jacoco/index.html"

# Check if running on WSL
if grep -qi microsoft /proc/version &> /dev/null; then
  # Convert WSL path to Windows path and open with Windows default browser
  WINDOWS_PATH=$(wslpath -w "$REPORT_PATH")
  cmd.exe /c "start $WINDOWS_PATH" 2>/dev/null || echo "Coverage report generated at: $REPORT_PATH"
elif command -v xdg-open &> /dev/null; then
  xdg-open "$REPORT_PATH"
elif command -v open &> /dev/null; then
  open "$REPORT_PATH"
else
  echo "Coverage report generated at: $REPORT_PATH"
fi

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
fixture=src/main/resources/security-demo.properties
case "${1:-}" in
  enable)
    if [[ -e "$fixture" ]]; then
      echo "Demo file already exists: $fixture" >&2
      exit 1
    fi
    # Deliberately invalid, local-only credential. No service accepts this token.
    suffix=0123456789abcdef0123456789abcdef
    printf '# FAKE credential for the security risk demo; never a real token.\n%s=%s%s\n' \
      DEMO_API_TOKEN demo_only_ "$suffix" > "$fixture"
    echo "Violation enabled: $fixture. Run bash scripts/scan-secrets.sh."
    ;;
  disable)
    rm -f "$fixture"
    echo "Violation removed. Run bash scripts/scan-secrets.sh again."
    ;;
  *)
    echo "Usage: bash scripts/security-demo.sh enable|disable" >&2
    exit 2
    ;;
esac

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p security-reports
# Read-only source mount. Only redacted reports are written to the output mount.
docker run --rm --user "$(id -u):$(id -g)" \
  -v "$PWD:/source:ro" \
  -v "$PWD/security-reports:/reports" \
  ghcr.io/gitleaks/gitleaks@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f \
  dir /source --config /source/.gitleaks.toml \
  --verbose --redact=100 --report-format json --report-path /reports/gitleaks.json \
  --exit-code 1

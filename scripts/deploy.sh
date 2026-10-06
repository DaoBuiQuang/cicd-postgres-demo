#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
compose=(docker compose -p "${COMPOSE_PROJECT_NAME:-cicd-demo}" -f docker-compose.deploy.yml)
"${compose[@]}" config --quiet
if [[ "${SKIP_PULL:-false}" != "true" ]]; then
  "${compose[@]}" pull
fi
"${compose[@]}" up -d --wait --wait-timeout 180
binding=$("${compose[@]}" port app 8080)
port=${binding##*:}
for attempt in {1..30}; do
  if curl --fail --silent --max-time 5 "http://127.0.0.1:${port}/api/health" | grep -q '"UP"' &&
     curl --fail --silent --max-time 5 "http://127.0.0.1:${port}/api/products" >/dev/null; then
    echo "Deployment verified: health and PostgreSQL-backed products API on port ${port}"
    "${compose[@]}" ps
    exit 0
  fi
  sleep 2
done
echo "Application verification failed" >&2
"${compose[@]}" logs --tail=100 app >&2
exit 1

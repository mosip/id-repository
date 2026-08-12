#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo "Bootstrapping local Keycloak realm/roles (~10y token lifespan)..."
docker compose up -d keycloak
docker compose run --rm --no-deps \
  -e KEYCLOAK_URL=http://keycloak:8080 \
  -e WRITE_WIREMOCK_STUBS=false \
  keycloak-init

echo "Starting live auth bridge + reloading WireMock proxies..."
docker compose up -d --force-recreate auth-token-bridge mock-service

echo "Done. Auth path: POST /v1/authmanager/authenticate/clientidsecretkey"
echo "  -> WireMock -> auth-token-bridge -> Keycloak (live JWT, not hardcoded)"

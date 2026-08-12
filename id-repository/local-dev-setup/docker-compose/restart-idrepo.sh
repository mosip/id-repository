#!/usr/bin/env bash
# Bring dependencies up first, then start/recreate id-repository-service.
# Prefer this over "docker compose restart id-repository-service"
# (restart does not re-check depends_on health).
set -euo pipefail
cd "$(dirname "$0")"

echo "[1/3] Seeding ZK keys (keys-generator writes PKCS12 before keymanager loads it)..."
docker compose up -d \
  database config-server keys-generator keys-generator-expiry

echo "[2/3] Starting keymanager + remaining deps (Keycloak mints WireMock auth JWT first)..."
docker compose up -d \
  keycloak keycloak-init auth-token-bridge uin-generator websub-mock mock-service minio minio-init biosdk-service \
  keymanager-service keymanager-init datashare-service

echo "[3/3] Starting id-repository-service (waits for all depends_on)..."
docker compose up -d --force-recreate id-repository-service

docker compose ps
echo "Done. Check: curl http://localhost:8090/actuator/health"
